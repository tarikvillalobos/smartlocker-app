package app.smartlocker.android

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.toSize
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.window.layout.WindowMetricsCalculator
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
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, Configuration.ORIENTATION_LANDSCAPE)
        ui.onNodeWithText("11987654321").assertExists()
        ui.onNodeWithText("52998224725").assertExists()
        ui.onNodeWithText("Receber código por SMS").performScrollTo().performClick()
        waitForText("Código de 6 dígitos")
        ui.onNodeWithText("Código de 6 dígitos").performScrollTo().performTextInput("123")
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, Configuration.ORIENTATION_PORTRAIT)
        ui.onNodeWithText("123").assertExists()
        ui.onNodeWithText("Código de 6 dígitos").performScrollTo().performTextReplacement("123456")
        ui.onNodeWithText("Confirmar código").performScrollTo().performClick()
        waitForText("Copiar código")
        ui.onNodeWithContentDescription("Ver detalhes").performScrollTo().performClick()
        ui.onNodeWithText("Já retirei a encomenda").performScrollTo().performClick()
        ui.onNodeWithText("Sim, retirei").performClick()
        waitForText("Retirada informada")
        ui.onNodeWithText("Retirada informada").assertExists()
        ui.onNodeWithText("Copiar código").assertDoesNotExist()
    }

    @Test fun closingAndReopeningTheActivityRestoresSessionUntilLogout() {
        login()
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForText("Copiar código")
        ui.onNodeWithText("Preencher dados de demonstração").assertDoesNotExist()
        ui.onNodeWithText("Perfil").performClick()
        ui.onNodeWithText("Sair").performScrollTo().performClick()
        waitForText("Preencher dados de demonstração")
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForText("Preencher dados de demonstração")
        ui.onNodeWithText("Copiar código").assertDoesNotExist()
    }

    private fun login() {
        ui.onNodeWithText("Preencher dados de demonstração").performScrollTo().performClick()
        ui.onNodeWithText("Receber código por SMS").performScrollTo().performClick()
        waitForText("Código de 6 dígitos")
        ui.onNodeWithText("Código de 6 dígitos").performScrollTo().performTextInput("123456")
        ui.onNodeWithText("Confirmar código").performScrollTo().performClick()
        waitForText("Copiar código")
    }

    private fun rotate(requested: Int, expected: Int) {
        scenario.onActivity { it.requestedOrientation = requested }
        ui.waitUntil(10_000) { context.resources.configuration.orientation == expected }
        ui.waitForIdle()
    }

    private fun waitForText(value: String) {
        ui.waitUntil(15_000) { ui.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
    }
}
