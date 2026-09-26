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
