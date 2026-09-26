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
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Código de retirada", value)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            clip.description.extras = android.os.PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        clipboard.setPrimaryClip(clip)
    }
    override fun openLink(url: String): Boolean = runCatching {
        val uri = Uri.parse(url)
        require(uri.scheme in setOf("https", "mailto"))
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
    override suspend fun notificationPermission(): String {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (manager.areNotificationsEnabled()) "Sistema permite avisos; push ainda não integrado."
        else "Avisos do sistema desativados. A central continua disponível."
    }
}
