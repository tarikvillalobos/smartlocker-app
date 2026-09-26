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

