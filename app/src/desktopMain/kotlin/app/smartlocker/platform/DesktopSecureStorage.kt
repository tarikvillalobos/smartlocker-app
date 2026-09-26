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
