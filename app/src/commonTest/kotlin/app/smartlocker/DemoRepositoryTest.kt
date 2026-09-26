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
        val challenge = repo.requestLogin(demoLogin)
        assertFailsWith<AppFailure> { repo.requestLogin(demoLogin) }
        repeat(5) { assertFailsWith<AppFailure> { repo.verifyLogin(challenge.id, "000000") } }
        assertEquals(FailureKind.ATTEMPTS_EXCEEDED,
            assertFailsWith<AppFailure> { repo.verifyLogin(challenge.id, "123456") }.kind)
        clock.time += 31_000
        val expired = repo.requestLogin(demoLogin)
        clock.time += 300_000
        assertEquals(FailureKind.EXPIRED_CODE,
            assertFailsWith<AppFailure> { repo.verifyLogin(expired.id, "123456") }.kind)
    }
    @Test fun restoresSessionButRejectsExpiredSessionAndClearsOnLogout() = runTest {
        val repo = repository()
        repo.signIn()
        assertNotNull(repository().restoreSession())
        clock.time += 8 * 86_400_000L
        assertNull(repository().restoreSession())
        repo.logout()
        assertTrue(secure.values.isEmpty())
        assertTrue(cache.values.isEmpty())
