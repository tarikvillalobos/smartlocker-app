package app.smartlocker

import app.smartlocker.auth.domain.*
import app.smartlocker.config.*
import app.smartlocker.demo.data.DemoRepository
import app.smartlocker.parcels.domain.*
import app.smartlocker.profile.domain.Profile
import app.smartlocker.shared.domain.*
import app.smartlocker.shared.presentation.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ControllerRecoveryTest {
    @Test fun openingDetailPreservesLoadedPagesAndCursor() = runTest {
        val clock = TestClock()
        val demo = DemoRepository(MemoryStorage(), MemorySecure(), Brands.smartLocker, clock, 0)
        demo.signIn()
        demo.scenario(DemoScenario.MANY)
        val controller = AppController(AppConfiguration(Brands.smartLocker, Environment.DEMO), demo, clock, backgroundScope)
        controller.state.first { it.profile != null }
        controller.more()
        val page = controller.state.first { it.parcels.size == 40 }
        val selected = page.parcels[30]
        controller.select(selected.id)
        val detail = controller.state.first { !it.busy && it.selected?.id == selected.id }
        assertEquals(page.parcels, detail.parcels)
        assertEquals(page.nextCursor, detail.nextCursor)
        controller.navigate(Route.HISTORY)
        assertEquals(40, controller.state.value.parcels.size)
    }

    @Test fun homeRecentDeliveriesIgnoreTheHistoryFilter() = runTest {
        val clock = TestClock()
        val demo = DemoRepository(MemoryStorage(), MemorySecure(), Brands.smartLocker, clock, 0)
        demo.signIn()
        val controller = AppController(AppConfiguration(Brands.smartLocker, Environment.DEMO), demo, clock, backgroundScope)
        val initial = controller.state.first { it.profile != null }.recent
        assertTrue(initial.any { it.status == ParcelStatus.WAITING })
        for (filter in listOf(ParcelFilter.COLLECTED, ParcelFilter.WAITING)) {
            controller.filter(filter)
            controller.state.first { !it.busy && it.parcels.isNotEmpty() }
            controller.navigate(Route.HOME)
            assertEquals(initial, controller.state.value.recent)
            assertEquals(filter, controller.state.value.filter)
        }
    }

    @Test fun expiredSessionRestoresTheSafeDestinationAndMembership() = runTest {
        val clock = TestClock()
        val demo = DemoRepository(MemoryStorage(), MemorySecure(), Brands.smartLocker, clock, 0)
        demo.signIn()
        val controller = AppController(AppConfiguration(Brands.smartLocker, Environment.DEMO), demo, clock, backgroundScope)
        controller.state.first { it.profile != null }
        controller.membership("office")
        controller.state.first { !it.busy && it.membershipId == "office" }
        controller.select("demo-8")
        controller.state.first { !it.busy && it.selected?.id == "demo-8" }
