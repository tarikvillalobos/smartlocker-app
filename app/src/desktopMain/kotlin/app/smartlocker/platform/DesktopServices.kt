package app.smartlocker.platform

import app.smartlocker.shared.domain.*
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest

class DesktopServices : PlatformServices {
    override val apiBaseUrl: String? = System.getenv("SMARTLOCKER_API_BASE_URL")?.takeIf { it.isNotBlank() }
    override val termsVersion: String? = System.getenv("SMARTLOCKER_TERMS_VERSION")?.takeIf { it.isNotBlank() }
    private val directory = Path.of(System.getProperty("user.home"), ".smartlocker")
    init { Files.createDirectories(directory); restrict(directory, true) }
    override val local = object : LocalStorage {
        private fun path(key: String): Path {
            val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
            return directory.resolve(digest.joinToString("") { "%02x".format(it) } + ".json")
        }
        override fun read(key: String): String? = path(key).let { if (Files.exists(it)) Files.readString(it) else null }
        override fun write(key: String, value: String?) {
            val file = path(key)
            if (value == null) Files.deleteIfExists(file)
            else {
                val temporary = Files.createTempFile(directory, "cache-", ".tmp")
                restrict(temporary)
                Files.writeString(temporary, value)
                Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
    override val secure: SecureStorage = DesktopSecureStorage(directory)
    override fun copyText(value: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
    }
    override fun openLink(url: String): Boolean = runCatching {
        val uri = URI(url)
        require(uri.scheme in setOf("https", "mailto"))
        if (uri.scheme == "mailto") Desktop.getDesktop().mail(uri) else Desktop.getDesktop().browse(uri)
        true
    }.getOrDefault(false)
    override suspend fun notificationPermission() = "Central disponível; push do sistema não integrado."
}

internal fun restrict(path: Path, directory: Boolean = false) {
    if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(if (directory) "rwx------" else "rw-------"))
    }
}
