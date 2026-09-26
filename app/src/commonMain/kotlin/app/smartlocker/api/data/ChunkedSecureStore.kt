package app.smartlocker.api.data

import app.smartlocker.shared.domain.AppFailure
import app.smartlocker.shared.domain.FailureKind
import app.smartlocker.shared.domain.SecureStorage
import kotlinx.coroutines.CancellationException
import kotlin.io.encoding.Base64
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** All calls, including calls through other instances with this key, must be serialized by the owner. */
@OptIn(ExperimentalUuidApi::class)
class ChunkedSecureStore(private val storage: SecureStorage, private val key: String) {
    private val headKey = "$key.head"
    private val journalKey = "$key.journal"

    init {
        if (!key.matches(Regex("[A-Za-z0-9._-]{1,100}"))) throw failure()
    }

    suspend fun read(): String? = guarded {
        val head = readHead()
        cleanup(head, required = false)
        if (head == null) return@guarded null
        val encoded = buildString(head.encodedLength) {
            repeat(head.chunks) { index ->
                val part = storage.read(chunkKey(head, index)) ?: throw failure()
                val expected = minOf(CHUNK_SIZE, head.encodedLength - index * CHUNK_SIZE)
                if (part.length != expected) throw failure()
                append(part)
            }
        }
        val bytes = Base64.decode(encoded)
        if (bytes.size != head.bytes || Base64.encode(bytes) != encoded || checksum(bytes) != head.checksum) throw failure()
        bytes.decodeToString(throwOnInvalidSequence = true)
    }

    suspend fun write(value: String?) = guarded {
        // Check limits before any mutation, including journal recovery.
        if (value != null && value.length > MAX_BYTES) throw failure()
