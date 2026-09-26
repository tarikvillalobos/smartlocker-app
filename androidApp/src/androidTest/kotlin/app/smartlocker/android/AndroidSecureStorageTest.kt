package app.smartlocker.android

import android.content.Context
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.smartlocker.auth.domain.LoginChannel
import app.smartlocker.auth.domain.LoginRequest
import app.smartlocker.demo.data.DemoAuth
import app.smartlocker.platform.AndroidSecureStorage
import app.smartlocker.shared.domain.AppClock
import app.smartlocker.shared.domain.AppFailure
import app.smartlocker.shared.domain.FailureKind
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AndroidSecureStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences = context.getSharedPreferences("smartlocker.secure", Context.MODE_PRIVATE)
    private val prefix = "instrumented.${UUID.randomUUID()}"
    private val storage = AndroidSecureStorage(context)

    @After fun removeOnlyTestRecords() {
        preferences.edit().also { editor ->
            preferences.all.keys.filter { it.contains(prefix) }.forEach(editor::remove)
        }.commit()
    }

    @Test fun ciphertextIsRandomizedAndNewStorageInstanceRestoresTheSession() = runBlocking {
        val key = "$prefix.session"
        val secret = "isolated-session-token-with-utf8-á"
        storage.write(key, secret)
        val first = preferences.getString(key, null)!!
        assertFalse(first.contains(secret))
        assertFalse(String(Base64.decode(first, Base64.NO_WRAP)).contains(secret))
