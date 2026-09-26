package app.smartlocker.api.data

import app.smartlocker.MemorySecure
import app.smartlocker.shared.domain.AppFailure
import app.smartlocker.shared.domain.SecureStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ChunkedSecureStoreTest {
    @Test fun largeUnicodeSessionRoundTripsUsingOnlySmallSecureEntries() = runTest {
        val memory = MemorySecure()
        val store = ChunkedSecureStore(memory, KEY)
        val session = "{\"access\":\"${"A".repeat(8192)}\",\"refresh\":\"${"R".repeat(8192)}\",\"name\":\"ação 🔒\"}"
        store.write(session)
        assertTrue(ChunkedSecureStore(memory, KEY).read() == session, "Complete session must round-trip")
        assertTrue(memory.values.keys.all { it.length <= 160 && it.matches(Regex("[A-Za-z0-9._-]+")) })
        assertTrue(memory.values.values.all { it.length <= 1500 && it != session })
        assertTrue(memory.values.keys.count { ".chunk." in it } > 10)
        assertFalse(memory.values.containsKey("$KEY.journal"))
    }

    @Test fun interruptedChunkWritePreservesOldSessionAndRecoversItsJournal() = runTest {
        val vault = FaultVault()
        val store = ChunkedSecureStore(vault, KEY)
        store.write("previous")
        val previousKeys = vault.memory.values.keys.toSet()
        vault.fail = { key, value -> value != null && ".chunk." in key && key.endsWith(".1") }
        assertFailsWith<AppFailure> { store.write("replacement".repeat(1000)) }
        assertTrue(vault.memory.values.containsKey("$KEY.journal"))
        vault.fail = { _, _ -> false }
        assertTrue(ChunkedSecureStore(vault, KEY).read() == "previous")
        assertEquals(previousKeys, vault.memory.values.keys)
    }

    @Test fun failedPointerWriteOrDeleteKeepsThePreviousGenerationReadable() = runTest {
        for (clear in listOf(false, true)) {
            val vault = FaultVault()
            val store = ChunkedSecureStore(vault, KEY)
            store.write("previous")
            val previousKeys = vault.memory.values.keys.toSet()
            vault.fail = { key, _ -> key == "$KEY.head" }
            assertFailsWith<AppFailure> { store.write(if (clear) null else "replacement".repeat(1000)) }
            vault.fail = { _, _ -> false }
            assertTrue(ChunkedSecureStore(vault, KEY).read() == "previous")
            assertEquals(previousKeys, vault.memory.values.keys)
        }
    }

    @Test fun cleanupFailureDoesNotUndoCommitAndIsRetriedBeforeAnotherWrite() = runTest {
        val vault = FaultVault()
        val store = ChunkedSecureStore(vault, KEY)
        store.write("previous")
        val obsolete = vault.memory.values.keys.filter { ".chunk." in it }.toSet()
        vault.fail = { key, value -> key in obsolete && value == null }
        store.write("replacement".repeat(1000))
        assertTrue(store.read() == "replacement".repeat(1000))
        assertTrue(vault.memory.values.containsKey("$KEY.journal"))
        assertFailsWith<AppFailure> { store.write("third") }
        vault.fail = { _, _ -> false }
        assertTrue(ChunkedSecureStore(vault, KEY).read() == "replacement".repeat(1000))
        assertTrue(obsolete.none { it in vault.memory.values })
        assertFalse(vault.memory.values.containsKey("$KEY.journal"))
    }

    @Test fun uncertainPointerResultRecoversTheGenerationActuallyCommitted() = runTest {
        val memory = MemorySecure()
        val storage = object : SecureStorage by memory {
            var failAfterCommit = false
            override suspend fun write(key: String, value: String?) {
                memory.write(key, value)
                if (failAfterCommit && key == "$KEY.head") error("Synthetic lost acknowledgement")
            }
        }
        val store = ChunkedSecureStore(storage, KEY)
        store.write("previous")
        storage.failAfterCommit = true
        assertFailsWith<AppFailure> { store.write("replacement".repeat(1000)) }
        storage.failAfterCommit = false
        assertTrue(ChunkedSecureStore(storage, KEY).read() == "replacement".repeat(1000))
        assertFalse(memory.values.containsKey("$KEY.journal"))
    }

    @Test fun interruptedClearStaysClearedWhileCleanupWaitsForTheVault() = runTest {
        val vault = FaultVault()
        val store = ChunkedSecureStore(vault, KEY)
        store.write("session".repeat(1000))
        vault.fail = { key, value -> ".chunk." in key && value == null }
        store.write(null)
        assertNull(store.read())
        assertTrue(vault.memory.values.containsKey("$KEY.journal"))
        vault.fail = { _, _ -> false }
        store.write(null)
        store.write(null)
        assertTrue(vault.memory.values.isEmpty())
    }

    @Test fun cancellationLeavesTrackedChunksAndNeverCommitsPartialData() = runTest {
        val memory = MemorySecure()
        val storage = object : SecureStorage by memory {
            var cancel = false
            override suspend fun write(key: String, value: String?) {
                if (cancel && ".chunk." in key && key.endsWith(".1") && value != null) throw CancellationException()
                memory.write(key, value)
            }
        }
        val store = ChunkedSecureStore(storage, KEY)
        store.write("previous")
        storage.cancel = true
        assertFailsWith<CancellationException> { store.write("replacement".repeat(1000)) }
        storage.cancel = false
        assertTrue(ChunkedSecureStore(storage, KEY).read() == "previous")
        assertEquals(2, memory.values.size)
    }

    @Test fun missingTruncatedOrModifiedChunksFailWithoutRevealingPayloads() = runTest {
        for (corruption in 0..2) {
            val memory = MemorySecure()
            val store = ChunkedSecureStore(memory, KEY)
            store.write("private-session".repeat(1000))
            val chunk = memory.values.keys.first { ".chunk." in it }
            val original = memory.values.getValue(chunk)
            when (corruption) {
                0 -> memory.values.remove(chunk)
                1 -> memory.values[chunk] = original.dropLast(1)
                2 -> memory.values[chunk] = (if (original[0] == 'A') "B" else "A") + original.drop(1)
            }
            val failure = assertFailsWith<AppFailure> { store.read() }
            assertEquals("Não foi possível acessar a sessão protegida.", failure.message)
        }
    }

    @Test fun oversizedOrMalformedManifestNeverEnumeratesUnboundedKeys() = runTest {
        val vault = FaultVault()
        val store = ChunkedSecureStore(vault, KEY)
        store.write("previous")
        val snapshot = vault.memory.values.toMap()
        assertFailsWith<AppFailure> { store.write("é".repeat(256 * 1024)) }
        assertEquals(snapshot, vault.memory.values)
        val manifest = vault.memory.values.getValue("$KEY.head")
        vault.memory.values["$KEY.head"] = manifest.split('|').toMutableList().apply { this[4] = "2147483647" }.joinToString("|")
        vault.reads = 0
        assertFailsWith<AppFailure> { store.read() }
        assertEquals(1, vault.reads)
    }

    @Test fun clearAndReplacementPreserveAnotherBrandAndSupportEmptyValues() = runTest {
        val memory = MemorySecure()
        val first = ChunkedSecureStore(memory, KEY)
        val second = ChunkedSecureStore(memory, "api.session.another")
        first.write("first")
        second.write("second")
        first.write("")
        assertTrue(first.read() == "")
        first.write(null)
        assertNull(first.read())
        assertTrue(second.read() == "second")
        assertTrue(memory.values.keys.all { it.startsWith("api.session.another.") })
    }

