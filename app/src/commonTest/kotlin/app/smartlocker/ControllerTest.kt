package app.smartlocker

import app.smartlocker.config.*
import app.smartlocker.demo.data.DemoRepository
import app.smartlocker.parcels.domain.ParcelStatus
import app.smartlocker.shared.data.ScopedCache
import app.smartlocker.shared.data.CacheScope
import app.smartlocker.shared.domain.*
import app.smartlocker.shared.presentation.*
import kotlinx.coroutines.CompletableDeferred
import app.smartlocker.auth.domain.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ControllerTest {
    @Test fun updatesHomeHistoryAndDetailFromSameResult() = runTest {
        val clock = TestClock()
        val repository = DemoRepository(MemoryStorage(), MemorySecure(), Brands.smartLocker, clock, 0)
        repository.signIn()
        val controller = AppController(AppConfiguration(Brands.smartLocker, Environment.DEMO), repository, clock, backgroundScope)
        controller.state.first { it.profile != null }
        controller.select("demo-0")
        controller.state.first { !it.busy && it.selectedId == "demo-0" }
        controller.markCollected()
        val marked = controller.state.first { it.selected?.status == ParcelStatus.MANUAL }
        assertNull(marked.credential)
        assertTrue(marked.pending.none { it.id == "demo-0" })
        assertEquals(ParcelStatus.MANUAL, marked.parcels.first { it.id == "demo-0" }.status)
        controller.logout()
        controller.state.first { it.session == null }
        assertTrue(controller.state.value.parcels.isEmpty())
    }
    @Test fun ignoresResponsesFromPreviousMembership() = runTest {
        val clock = TestClock()
        val repository = DemoRepository(MemoryStorage(), MemorySecure(), Brands.smartLocker, clock, 10)
        repository.signIn()
        val controller = AppController(AppConfiguration(Brands.smartLocker, Environment.DEMO), repository, clock, backgroundScope)
        controller.state.first { it.profile != null }
        controller.select("demo-0")
        advanceTimeBy(12)
        controller.membership("office")
        val current = controller.state.first { !it.busy && it.membershipId == "office" && it.lastUpdated != null }
        assertTrue(current.parcels.all { it.locationId == "office" })
        assertNotEquals("demo-0", current.credential?.parcelId)
    }
    @Test fun isolatesCacheAndProtectsDisabledNavigation() = runTest {
        val cache = ScopedCache(MemoryStorage())
        val scope = CacheScope("a", "u", "home")
        cache.write(scope, "safe metadata")
        assertNull(cache.read(scope.copy(user = "other")))
        assertNull(cache.read(scope.copy(brand = "b")))
        assertNull(cache.read(scope.copy(membership = "office")))
        val clock = TestClock()
        val controller = AppController(AppConfiguration(Brands.aurora, Environment.DEMO),
            DemoRepository(MemoryStorage(), MemorySecure(), Brands.aurora, clock, 0), clock, backgroundScope)
        controller.navigate(Route.RESIDENTS)
        assertEquals(Route.HOME, controller.state.value.route)
    }

    @Test fun logoutDiscardsContactVerificationAlreadyInFlight() = runTest {
        val clock = TestClock()
        val demo = DemoRepository(MemoryStorage(), MemorySecure(), Brands.smartLocker, clock, 0)
        demo.signIn()
        val profile = demo.profile()
        val response = CompletableDeferred<app.smartlocker.profile.domain.Profile>()
        val repository = object : LockerRepository by demo {
            override suspend fun verifyContactChange(challengeId: String, code: String) = response.await()
        }
        val controller = AppController(AppConfiguration(Brands.smartLocker, Environment.DEMO), repository, clock, backgroundScope)
        controller.state.first { it.profile != null }
        controller.contact("changed@example.test", LoginChannel.EMAIL)
        controller.state.first { it.contactChallenge != null }
        controller.verifyContact("123456")
        runCurrent()
        controller.logout()
        runCurrent()
        response.complete(profile.copy(email = "changed@example.test"))
        runCurrent()
}
