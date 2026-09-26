package app.smartlocker.platform

import app.smartlocker.shared.domain.*
import com.sun.jna.platform.win32.Crypt32Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64

/** Keychain on macOS, DPAPI on Windows, Secret Service on Linux. Never plaintext fallback. */
class DesktopSecureStorage(private val directory: Path) : SecureStorage {
    private val os = System.getProperty("os.name").lowercase()
    private val service = "app.smartlocker.session"
    private fun encoded(value: String) = Base64.getEncoder().encodeToString(value.toByteArray())
    private fun decoded(value: String) = String(Base64.getDecoder().decode(value.trim()))

    override suspend fun read(key: String): String? = withContext(Dispatchers.IO) {
        when {
            "mac" in os -> command(listOf("/usr/bin/security", "find-generic-password", "-a", key, "-s", service, "-w"), allowMissing = true)
                ?.let(::decoded)
            "win" in os -> {
                val path = directory.resolve(encoded(key) + ".protected")
                if (Files.exists(path)) String(Crypt32Util.cryptUnprotectData(Files.readAllBytes(path))) else null
            }
            else -> command(listOf("secret-tool", "lookup", "service", service, "account", key), allowMissing = true)
                ?.takeIf { it.isNotBlank() }?.let(::decoded)
        }
    }

    override suspend fun write(key: String, value: String?) = withContext(Dispatchers.IO) {
        require(key.matches(Regex("[a-zA-Z0-9._-]+")))
        when {
            "mac" in os -> {
                if (value == null) command(listOf("/usr/bin/security", "delete-generic-password", "-a", key, "-s", service), allowMissing = true)
                else {
                    // The secret is sent on stdin, not exposed in the process argument list.
                    val input = "add-generic-password -U -a $key -s $service -w ${encoded(value)}\n"
                    command(listOf("/usr/bin/security", "-i"), input)
                }
            }
            "win" in os -> {
                val path = directory.resolve(encoded(key) + ".protected")
                if (value == null) Files.deleteIfExists(path)
                else Files.write(path, Crypt32Util.cryptProtectData(value.toByteArray()))
            }
            else -> {
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
        process.errorStream.bufferedReader().readText() // Never log secret-service output.
        val result = process.waitFor()
        if (result == 0) return output
        if (allowMissing && (result == 44 || result == 1)) return null
        throw unavailable()
    }
    private fun unavailable() = AppFailure(FailureKind.UNAVAILABLE,
        "Armazenamento seguro indisponível. Desbloqueie o cofre do sistema; no Linux instale secret-tool.")
}
