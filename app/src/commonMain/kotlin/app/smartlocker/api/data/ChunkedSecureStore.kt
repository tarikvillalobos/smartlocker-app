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
        val bytes = value?.encodeToByteArray(throwOnInvalidSequence = true)
        if (bytes != null && bytes.size > MAX_BYTES) throw failure()
        val encoded = bytes?.let { Base64.encode(it) }
        val next = if (bytes == null || encoded == null) null else Manifest(
            Uuid.random().toString(), bytes.size, encoded.length,
            (encoded.length + CHUNK_SIZE - 1) / CHUNK_SIZE, checksum(bytes),
        )
        val previous = readHead()
        if (value == null) { clear(previous); return@guarded }
        // Never replace a journal until all of its obsolete generations were removed.
        cleanup(previous, required = true)
        val generations = listOfNotNull(previous, next)
        if (generations.isEmpty()) return@guarded
        storage.write(journalKey, generations.joinToString("\n", transform = Manifest::encode))
        if (next != null && encoded != null) repeat(next.chunks) { index ->
            storage.write(chunkKey(next, index), encoded.substring(index * CHUNK_SIZE,
                minOf((index + 1) * CHUNK_SIZE, encoded.length)))
        }
        // The small pointer is the commit point. A failed/uncertain write leaves a recoverable journal.
        storage.write(headKey, next?.encode())
        // A cleanup failure must not turn a successfully committed session into a failed write.
        cleanup(next, required = false)
    }

    private suspend fun readHead() = storage.read(headKey)?.let(::parse)
    private fun chunkKey(manifest: Manifest, index: Int) = "$key.chunk.${manifest.generation}.$index"

    private suspend fun cleanup(head: Manifest?, required: Boolean) {
        try {
            val raw = storage.read(journalKey) ?: return
            if (raw.length > 320) throw failure()
            val lines = raw.split('\n')
            if (lines.size !in 1..2) throw failure()
            val entries = lines.map(::parse)
            if (entries.map { it.generation }.distinct().size != entries.size) throw failure()
            for (entry in entries) {
                if (entry.generation == head?.generation) {
                    if (entry != head) throw failure()
                } else repeat(entry.chunks) { storage.write(chunkKey(entry, it), null) }
            }
            storage.write(journalKey, null)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (required) throw failure()
            // Keep the journal for a later read/write. Never infer keys from an invalid manifest.
        }
    }

    private fun parse(raw: String): Manifest {
        if (raw.length > 160) throw failure()
        val fields = raw.split('|')
        if (fields.size != 6 || fields[0] != "1" || !UUID.matches(fields[1])) throw failure()
        val bytes = fields[2].toIntOrNull() ?: throw failure()
        val encodedLength = fields[3].toIntOrNull() ?: throw failure()
        val chunks = fields[4].toIntOrNull() ?: throw failure()
        if (bytes !in 0..MAX_BYTES || encodedLength != ((bytes + 2) / 3) * 4 ||
            chunks != (encodedLength + CHUNK_SIZE - 1) / CHUNK_SIZE || !CHECKSUM.matches(fields[5])) throw failure()
        val value = Manifest(fields[1], bytes, encodedLength, chunks, fields[5])
        if (value.encode() != raw) throw failure()
        return value
    }

    private data class Manifest(
        val generation: String, val bytes: Int, val encodedLength: Int, val chunks: Int, val checksum: String,
    ) {
        fun encode() = "1|$generation|$bytes|$encodedLength|$chunks|$checksum"
    }

    private suspend fun <T> guarded(block: suspend () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        throw failure()
    }

    // Detect accidental chunk corruption; confidentiality/integrity protection belongs to the native vault.
    private fun checksum(bytes: ByteArray): String {
        var crc = -1
        for (byte in bytes) {
            crc = crc xor (byte.toInt() and 255)
            repeat(8) { crc = (crc ushr 1) xor if ((crc and 1) == 1) 0xedb88320.toInt() else 0 }
        }
        return (crc xor -1).toUInt().toString(16).padStart(8, '0')
    }

    private fun failure() = AppFailure(FailureKind.UNAVAILABLE, "Não foi possível acessar a sessão protegida.")

    private companion object {
        const val MAX_BYTES = 256 * 1024
        const val CHUNK_SIZE = 1500
        val UUID = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
        val CHECKSUM = Regex("[0-9a-f]{8}")
    }
}
