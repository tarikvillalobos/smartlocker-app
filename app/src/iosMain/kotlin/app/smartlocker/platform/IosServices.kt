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
    override val local = object : LocalStorage {
        private val defaults = NSUserDefaults.standardUserDefaults
        override fun read(key: String) = defaults.stringForKey("smartlocker.$key")
        override fun write(key: String, value: String?) {
