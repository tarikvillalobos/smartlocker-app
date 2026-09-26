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
            }
            "win" in os -> {
                val path = directory.resolve(encoded(key) + ".protected")
                if (value == null) Files.deleteIfExists(path)
                else Files.write(path, Crypt32Util.cryptProtectData(value.toByteArray()))
            }
            else -> {
                if (value == null) command(listOf("secret-tool", "clear", "service", service, "account", key))
                else command(listOf("secret-tool", "store", "--label=SmartLocker", "service", service, "account", key), encoded(value))
            }
        }
        Unit
    }

    private fun command(args: List<String>, input: String? = null, allowMissing: Boolean = false): String? {
        val process = try { ProcessBuilder(args).start() }
        catch (_: Exception) { throw unavailable() }
        process.outputStream.use { stream -> input?.let { stream.write(it.toByteArray()) } }
        val output = process.inputStream.bufferedReader().readText()
        val errorOutput = process.errorStream.bufferedReader().readText() // Never log secret-service output.
        val result = process.waitFor()
        if (result == 0) return output
        val missing = if ("mac" in os) result == 44 else result == 1 && errorOutput.isBlank()
        if (allowMissing && missing) return null
        throw unavailable()
    }
    private fun unavailable() = AppFailure(FailureKind.UNAVAILABLE,
        "Armazenamento seguro indisponível. Desbloqueie o cofre do sistema; no Linux instale secret-tool.")
}
