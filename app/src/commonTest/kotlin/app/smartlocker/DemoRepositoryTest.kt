package app.smartlocker

import app.smartlocker.config.Brands
import app.smartlocker.demo.data.DemoRepository
import app.smartlocker.parcels.domain.*
import app.smartlocker.profile.domain.CommunicationPreferences
import app.smartlocker.shared.domain.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class DemoRepositoryTest {
    private val cache = MemoryStorage()
    private val secure = MemorySecure()
    private val clock = TestClock()
    private fun repository(brand: app.smartlocker.config.Brand = Brands.smartLocker) =
        DemoRepository(cache, secure, brand, clock, 0)

    @Test fun requiresOtpEnforcesAttemptLimitExpiryAndResendInterval() = runTest {
        val repo = repository()
        assertFailsWith<AppFailure> { repo.profile() }
