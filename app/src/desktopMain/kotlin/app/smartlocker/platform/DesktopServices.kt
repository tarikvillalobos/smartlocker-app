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
    private val directory = Path.of(System.getProperty("user.home"), ".smartlocker")
    init { Files.createDirectories(directory); restrict(directory, true) }
    override val local = object : LocalStorage {
        private fun path(key: String): Path {
            val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
            return directory.resolve(digest.joinToString("") { "%02x".format(it) } + ".json")
        }
