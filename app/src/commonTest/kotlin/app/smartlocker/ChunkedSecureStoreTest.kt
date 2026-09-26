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
