package app.smartlocker

import app.smartlocker.platform.DesktopSecureStorage
import app.smartlocker.shared.domain.AppFailure
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Opt-in integration tests. Values and keys are synthetic, never real user sessions. */
class NativeSecureStorageTest {
    @Test(timeout = 90_000) fun persistsUpdatesAndDeletesIsolatedKeys() = fixture { storage, directory, first, second ->
        assertNull(storage.read(first), "Fresh test key must be absent")
        storage.write(first, null)
        val original = "fixture-${UUID.randomUUID()}-ação-🔒\nsecond line"
