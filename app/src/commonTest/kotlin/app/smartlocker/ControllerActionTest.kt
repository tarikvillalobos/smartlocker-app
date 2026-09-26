package app.smartlocker

import app.smartlocker.config.*
import app.smartlocker.demo.data.DemoRepository
import app.smartlocker.parcels.domain.*
import app.smartlocker.shared.domain.LockerRepository
import app.smartlocker.shared.presentation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ControllerActionTest {
    @Test fun pendingMutationKeepsItsSelectionAndPublishesOneConfirmedResult() = runTest {
        val clock = TestClock()
        val demo = DemoRepository(MemoryStorage(), MemorySecure(), Brands.smartLocker, clock, 0)
        demo.signIn()
        val response = CompletableDeferred<Unit>()
        var calls = 0
        val repository = object : LockerRepository by demo {
            override suspend fun markCollected(locationId: String, parcelId: String): Parcel {
                calls++
                response.await()
                return demo.markCollected(locationId, parcelId)
            }
        }
        val controller = start(repository, clock)
        val selected = controller.state.value.selectedId!!
        controller.markCollected()
        runCurrent()
        assertTrue(controller.state.value.busy)
        controller.markCollected()
        controller.select("demo-1")
        controller.membership("office")
        controller.filter(ParcelFilter.COLLECTED)
        controller.refresh()
        assertEquals(selected, controller.state.value.selectedId)
        assertEquals("home", controller.state.value.membershipId)
        assertEquals(ParcelFilter.ALL, controller.state.value.filter)
        response.complete(Unit)
        val confirmed = controller.state.first { !it.busy && it.selected?.status == ParcelStatus.MANUAL }
        assertEquals(1, calls)
        assertEquals(ParcelStatus.MANUAL, confirmed.parcels.first { it.id == selected }.status)
        assertTrue(confirmed.pending.none { it.id == selected })
        assertNull(confirmed.credential)
    }

    @Test fun notificationReadUpdatesBadgeBeforeDetailFinishes() = runTest {
        val clock = TestClock()
        val demo = DemoRepository(MemoryStorage(), MemorySecure(), Brands.smartLocker, clock, 0)
        demo.signIn()
        val response = CompletableDeferred<Unit>()
        var suspendDetail = false
        val repository = object : LockerRepository by demo {
            override suspend fun parcel(locationId: String, id: String): Parcel {
                if (suspendDetail) response.await()
                return demo.parcel(locationId, id)
            }
        }
        val controller = start(repository, clock)
        val initial = controller.state.value
        val notice = initial.notices.first { !it.read }
        suspendDetail = true
        controller.notice(notice)
        runCurrent()
        assertTrue(controller.state.value.busy)
        assertEquals(initial.unreadCount - 1, controller.state.value.unreadCount)
        assertTrue(controller.state.value.notices.first { it.id == notice.id }.read)
        assertEquals(Route.DETAIL, controller.state.value.route)
        response.complete(Unit)
        val current = controller.state.first { !it.busy && it.selected?.id == notice.parcelId }
        assertEquals(initial.unreadCount - 1, current.unreadCount)
    }

    @Test fun logoutCancelsActionAndIgnoresAnUncooperativeLateResponse() = runTest {
        val clock = TestClock()
        val demo = DemoRepository(MemoryStorage(), MemorySecure(), Brands.smartLocker, clock, 0)
        demo.signIn()
        val response = CompletableDeferred<Parcel>()
        val late = demo.parcel("home", "demo-0").copy(manualAt = clock.now(), credentialStatus = CredentialStatus.REVOKED)
        var cancelled = false
        val repository = object : LockerRepository by demo {
            override suspend fun markCollected(locationId: String, parcelId: String): Parcel = try {
                response.await()
            } catch (_: CancellationException) {
                cancelled = true
                withContext(NonCancellable) { response.await() }
            }
        }
        val controller = start(repository, clock)
        controller.markCollected()
        runCurrent()
        controller.logout()
        runCurrent()
        assertTrue(cancelled)
        response.complete(late)
        runCurrent()
        val loggedOut = controller.state.value
        assertNull(loggedOut.session)
        assertNull(loggedOut.profile)
        assertNull(loggedOut.selected)
        assertNull(loggedOut.credential)
        assertNull(loggedOut.feedback)
        assertTrue(loggedOut.parcels.isEmpty())
        assertFalse(loggedOut.busy)
        assertEquals(Route.HOME, loggedOut.route)
    }

    private suspend fun TestScope.start(repository: LockerRepository, clock: TestClock): AppController {
        val controller = AppController(AppConfiguration(Brands.smartLocker, Environment.DEMO), repository, clock, backgroundScope)
        controller.state.first { it.profile != null && !it.busy }
        return controller
    }
}
