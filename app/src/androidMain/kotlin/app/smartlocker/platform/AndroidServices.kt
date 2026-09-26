package app.smartlocker.platform

import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import app.smartlocker.shared.domain.*

class AndroidServices(private val context: Context) : PlatformServices {
    private val preferences = context.getSharedPreferences("smartlocker.local", Context.MODE_PRIVATE)
    override val local = object : LocalStorage {
        override fun read(key: String): String? = preferences.getString(key, null)
        override fun write(key: String, value: String?) {
            check(preferences.edit().putString(key, value).commit()) { "Could not persist app data" }
        }
    }
    override val secure: SecureStorage = AndroidSecureStorage(context)
    override fun copyText(value: String) {
