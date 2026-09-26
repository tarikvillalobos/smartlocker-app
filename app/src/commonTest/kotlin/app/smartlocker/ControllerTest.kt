package app.smartlocker

import app.smartlocker.config.*
import app.smartlocker.demo.data.DemoRepository
import app.smartlocker.parcels.domain.ParcelStatus
import app.smartlocker.shared.data.ScopedCache
import app.smartlocker.shared.data.CacheScope
import app.smartlocker.shared.domain.*
import app.smartlocker.shared.presentation.*
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
