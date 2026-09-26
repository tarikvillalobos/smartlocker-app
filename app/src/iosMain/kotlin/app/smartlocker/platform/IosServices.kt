package app.smartlocker.platform

import app.smartlocker.shared.domain.*
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import kotlin.coroutines.resume

class IosServices(
    private val readSecret: (String) -> String?,
    private val writeSecret: (String, String?) -> Boolean,
) : PlatformServices {
    override val apiBaseUrl: String? = (platform.Foundation.NSBundle.mainBundle
        .objectForInfoDictionaryKey("SmartLockerApiBaseUrl") as? String)?.takeIf { it.isNotBlank() && !it.startsWith("$(") }
    override val local = object : LocalStorage {
        private val defaults = NSUserDefaults.standardUserDefaults
        override fun read(key: String) = defaults.stringForKey("smartlocker.$key")
        override fun write(key: String, value: String?) {
            if (value == null) defaults.removeObjectForKey("smartlocker.$key")
            else defaults.setObject(value, "smartlocker.$key")
        }
    }
    override val secure = object : SecureStorage {
        override suspend fun read(key: String): String? = readSecret(key)
        override suspend fun write(key: String, value: String?) {
            if (!writeSecret(key, value)) throw AppFailure(FailureKind.UNAVAILABLE, "Não foi possível acessar o Chaves do dispositivo.")
        }
    }
    override fun copyText(value: String) { UIPasteboard.generalPasteboard.string = value }
    override fun openLink(url: String): Boolean {
        val value = NSURL.URLWithString(url) ?: return false
        if (value.scheme !in setOf("https", "mailto")) return false
        if (!UIApplication.sharedApplication.canOpenURL(value)) return false
        UIApplication.sharedApplication.openURL(value, emptyMap<Any?, Any>(), null)
        return true
    }
    override suspend fun notificationPermission(): String = suspendCancellableCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            if (continuation.isActive) continuation.resume(
                if (settings?.authorizationStatus == UNAuthorizationStatusAuthorized) "Sistema permite avisos; push não integrado."
                else "Avisos não autorizados; a central continua disponível.")
        }
    }
}
