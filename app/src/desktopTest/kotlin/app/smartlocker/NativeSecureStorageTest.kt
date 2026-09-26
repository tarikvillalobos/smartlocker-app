package app.smartlocker

import app.smartlocker.platform.DesktopSecureStorage
import app.smartlocker.api.data.ChunkedSecureStore
import app.smartlocker.shared.domain.SecureStorage
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
            val longValue = "prefix-${UUID.randomUUID()}-" + "T".repeat(10_000) + "-complete-tail"
            storage.write(first, prior)
            val failure = runCatching { storage.write(first, longValue) }.exceptionOrNull()
            if (failure == null) assertSecret(longValue, storage.read(first))
            else {
                assertTrue(failure is AppFailure || failure is IllegalArgumentException,
                    "Native storage must fail safely for unsupported payloads")
                assertSecret(prior, storage.read(first))
            }
            assertNoPlaintext(directory, prior, longValue)
        }

    @Test(timeout = 180_000) fun chunkedApiSessionRoundTripsLargePayloadInTheNativeVault() =
        fixture { storage, directory, namespace, _ ->
            val touched = linkedSetOf<String>()
            val tracked = object : SecureStorage by storage {
                override suspend fun write(key: String, value: String?) {
                    touched += key
                    storage.write(key, value)
                }
            }
            val chunks = ChunkedSecureStore(tracked, namespace)
            val marker = UUID.randomUUID().toString()
            val original = """{"accessToken":"$marker-${"A".repeat(8100)}","refreshToken":"${"R".repeat(8100)}","metadata":"${"ação-🔒".repeat(160)}","version":1}"""
            val replacement = original.replace("\"version\":1", "\"version\":2")
            assertTrue(original.toByteArray(Charsets.UTF_8).size in 18_000..20_000)
            var failure: Throwable? = null
            try {
                chunks.write(original)
                assertSecret(original, ChunkedSecureStore(DesktopSecureStorage(directory), namespace).read())
                chunks.write(replacement)
                assertSecret(replacement, ChunkedSecureStore(DesktopSecureStorage(directory), namespace).read())
                assertNoPlaintext(directory, original, replacement)
                chunks.write(null)
                chunks.write(null)
                assertNull(ChunkedSecureStore(DesktopSecureStorage(directory), namespace).read())
                for (key in touched) assertNull(storage.read(key), "Every isolated chunk and manifest must be removed")
            } catch (problem: Throwable) {
                failure = problem
                throw problem
            } finally {
                // Track keys before writes, including writes whose native acknowledgement might be lost.
                // Attempt every isolated cleanup even if one item fails; never enumerate a user's vault.
                for (key in listOf("$namespace.head") + touched) {
                    try { storage.write(key, null) }
                    catch (cleanup: Throwable) {
                        if (failure == null) failure = cleanup else failure.addSuppressed(cleanup)
                    }
                }
                failure?.let { throw it }
            }
        }
    private fun fixture(test: suspend (DesktopSecureStorage, Path, String, String) -> Unit) {
        assumeTrue("Native vault tests require explicit opt-in", System.getenv("SMARTLOCKER_NATIVE_SECURE_TESTS") == "1")
        val directory = Files.createTempDirectory("smartlocker-vault-test-")
        val storage = DesktopSecureStorage(directory)
        val first = "test.smartlocker.${UUID.randomUUID()}"
        val second = "$first.second"
        var failure: Throwable? = null
        try {
            runBlocking { test(storage, directory, first, second) }
        } catch (problem: Throwable) {
            failure = problem
            throw problem
        } finally {
            // Attempt every cleanup even when an assertion or one vault operation fails.
            for (key in listOf(first, second)) {
                try { runBlocking { storage.write(key, null) } }
                catch (cleanup: Throwable) {
                    if (failure == null) failure = cleanup else failure.addSuppressed(cleanup)
                }
            }
            try {
                Files.walk(directory).use { paths ->
                    paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
                }
            } catch (cleanup: Throwable) {
                if (failure == null) failure = cleanup else failure.addSuppressed(cleanup)
            }
            failure?.let { throw it }
        }
    }

    private fun assertSecret(expected: String, actual: String?) {
        // Avoid equality assertions which print secrets into JUnit reports on failure.
        assertTrue(expected == actual, "Native vault must return the complete stored value")
    }

    private fun assertNoPlaintext(directory: Path, vararg secrets: String) {
        Files.walk(directory).use { paths ->
            paths.filter { Files.isRegularFile(it) }.forEach { file ->
                val bytes = Files.readAllBytes(file).toString(Charsets.UTF_8)
                for (secret in secrets) assertFalse(bytes.contains(secret), "Cache must not contain plaintext session data")
            }
        }
    }
}
