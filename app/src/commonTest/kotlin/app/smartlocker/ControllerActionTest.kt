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
