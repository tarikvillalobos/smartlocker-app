package app.smartlocker.android

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.smartlocker.config.Brands
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeAppFlowTest {
    @get:Rule val ui = createEmptyComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before fun launchIsolatedValidationApplication() {
        check(context.packageName == "app.smartlocker.validation") {
            "Run with -PapplicationId=app.smartlocker.validation to isolate test data."
        }
        context.getSharedPreferences("smartlocker.secure", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("smartlocker.local", Context.MODE_PRIVATE).edit().clear()
            .putString("brand", Brands.smartLocker.id).putString("environment", "DEMO").commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForText("Preencher dados de demonstração")
    }

    @After fun closeActivity() {
        if (::scenario.isInitialized) scenario.close()
    }

    @Test fun enteredLoginAndOtpSurviveRotationAndManualPickupWorks() {
        ui.onNodeWithText("Celular").performScrollTo().performTextInput("11987654321")
