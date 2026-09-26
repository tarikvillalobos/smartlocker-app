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
        val replacement = "replacement-${UUID.randomUUID()}"
        storage.write(first, original)
        storage.write(second, replacement)
        assertSecret(original, storage.read(first))
        assertSecret(replacement, storage.read(second))
        assertSecret(original, DesktopSecureStorage(directory).read(first))
        storage.write(first, "")
        assertSecret("", storage.read(first))
        storage.write(first, replacement)
        assertSecret(replacement, storage.read(first))
        storage.write(first, null)
        storage.write(first, null)
        assertNull(storage.read(first), "Deleted test key must be absent")
        assertSecret(replacement, storage.read(second))
        assertNoPlaintext(directory, original, replacement)
    }

    @Test(timeout = 90_000) fun longValuesNeverSilentlyReplaceThePreviousSessionWithTruncatedData() =
        fixture { storage, directory, first, _ ->
            val prior = "prior-${UUID.randomUUID()}"
