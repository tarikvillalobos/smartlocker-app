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
