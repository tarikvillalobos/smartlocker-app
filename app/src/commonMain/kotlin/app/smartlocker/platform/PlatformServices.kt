package app.smartlocker.platform

import app.smartlocker.shared.domain.LocalStorage
import app.smartlocker.shared.domain.SecureStorage

interface PlatformServices {
    val apiBaseUrl: String? get() = null
    val termsVersion: String? get() = null
    val local: LocalStorage
    val secure: SecureStorage
    fun copyText(value: String)
    fun openLink(url: String): Boolean
    suspend fun notificationPermission(): String
}
