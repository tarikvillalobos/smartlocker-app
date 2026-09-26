package app.smartlocker.platform

import app.smartlocker.shared.domain.*
import com.sun.jna.platform.win32.Crypt32Util
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.NoSuchFileException
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Keychain on macOS, DPAPI on Windows, Secret Service on Linux. Never plaintext fallback. */
class DesktopSecureStorage(private val directory: Path) : SecureStorage {
    private val os = System.getProperty("os.name").lowercase()
    private val isMac = "mac" in os || "darwin" in os
    private val isWindows = "windows" in os
    private val service = "app.smartlocker.session"
    private fun encoded(value: String) = Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun decoded(value: String) = Base64.getDecoder().decode(value.trim()).toString(Charsets.UTF_8)
    private fun validateKey(key: String) {
        require(key.matches(Regex("[a-zA-Z0-9._-]{1,160}"))) { "Identificador de sessão inválido." }
    }

    override suspend fun read(key: String): String? = withContext(Dispatchers.IO) {
        validateKey(key)
        nativeOperation {
            when {
                isMac -> command(listOf("/usr/bin/security", "find-generic-password", "-a", key, "-s", service, "-w"),
                    allowMissing = true)?.let(::decoded)
                isWindows -> {
                    val path = directory.resolve(encoded(key) + ".protected")
                    val protected = try { Files.readAllBytes(path) } catch (_: NoSuchFileException) { null }
                    protected?.let { Crypt32Util.cryptUnprotectData(it).toString(Charsets.UTF_8) }
                }
                "linux" in os -> command(listOf("secret-tool", "lookup", "service", service, "account", key),
                    allowMissing = true)?.let(::decoded)
                else -> throw unavailable()
            }
        }
    }

    override suspend fun write(key: String, value: String?) = withContext(Dispatchers.IO) {
        validateKey(key)
        nativeOperation {
            when {
                isMac -> {
                    if (value == null) command(listOf("/usr/bin/security", "delete-generic-password", "-a", key, "-s", service),
                        allowMissing = true)
                    else {
                        // Quoting preserves empty values. Base64 contains no shell/parser metacharacters.
                        val input = "add-generic-password -U -a $key -s $service -w \"${encoded(value)}\"\n"
                        requirePayload(input.toByteArray(Charsets.UTF_8).size < 4096)
                        command(listOf("/usr/bin/security", "-i"), input)
                    }
                }
                isWindows -> {
                    val path = directory.resolve(encoded(key) + ".protected")
                    if (value == null) Files.deleteIfExists(path) else writeProtected(path, value)
                }
                "linux" in os -> {
                    if (value == null) command(listOf("secret-tool", "clear", "service", service, "account", key),
                        allowMissing = true)
                    else {
                        val input = encoded(value)
                        requirePayload(input.length < 8192)
                        command(listOf("secret-tool", "store", "--label=SmartLocker", "service", service, "account", key), input)
                    }
                }
                else -> throw unavailable()
            }
            Unit
        }
    }

    private fun writeProtected(path: Path, value: String) {
        val protected = Crypt32Util.cryptProtectData(value.toByteArray(Charsets.UTF_8))
        Files.createDirectories(directory)
        val temporary = Files.createTempFile(directory, "session-", ".protected.tmp")
        try {
            Files.write(temporary, protected)
            try { Files.move(temporary, path, ATOMIC_MOVE, REPLACE_EXISTING) }
            catch (_: AtomicMoveNotSupportedException) { Files.move(temporary, path, REPLACE_EXISTING) }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private suspend fun <T> nativeOperation(operation: suspend () -> T): T = try {
        currentCoroutineContext().ensureActive()
        operation()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: AppFailure) {
        throw failure
    } catch (_: Exception) {
        throw unavailable()
    }

    private fun requirePayload(supported: Boolean) {
        if (!supported) throw AppFailure(FailureKind.UNAVAILABLE,
            "A sessão excede o limite do cofre deste sistema. A sessão anterior foi preservada.")
    }

    private suspend fun command(args: List<String>, input: String? = null, allowMissing: Boolean = false): String? {
        val process = ProcessBuilder(args).start()
        val workers = Executors.newFixedThreadPool(3) { task ->
            Thread(task, "smartlocker-vault-io").apply { isDaemon = true }
        }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        try {
            // Drain both streams while writing; a full stderr pipe must never block stdout.
            val output = workers.submit<String> { readOutput(process.inputStream) }
            val errors = workers.submit<String> { readOutput(process.errorStream) }
            val writing = workers.submit<Unit> {
                process.outputStream.use { stream -> input?.let { stream.write(it.toByteArray(Charsets.UTF_8)) } }
            }
            while (!process.waitFor(100, TimeUnit.MILLISECONDS)) {
                currentCoroutineContext().ensureActive()
                if (System.nanoTime() >= deadline) throw unavailable()
            }
            currentCoroutineContext().ensureActive()
            while (!writing.isDone || !output.isDone || !errors.isDone) {
                currentCoroutineContext().ensureActive()
                if (System.nanoTime() >= deadline) throw unavailable()
                delay(10)
            }
            currentCoroutineContext().ensureActive()
            writing.get()
            val text = output.get()
            val errorText = errors.get()
            val result = process.exitValue()
            if (result == 0) return text
            val missing = if (isMac) result == 44 else result == 1 && errorText.isBlank()
            if (allowMissing && missing) return null
            throw unavailable()
        } finally {
            if (process.isAlive) process.destroyForcibly()
    private fun unavailable() = AppFailure(FailureKind.UNAVAILABLE,
        "Armazenamento seguro indisponível. Desbloqueie o cofre do sistema; no Linux instale secret-tool.")
}
