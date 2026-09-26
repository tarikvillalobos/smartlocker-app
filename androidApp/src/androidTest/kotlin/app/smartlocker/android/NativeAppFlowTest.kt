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
        waitForWindowFocus()
        waitForText("Preencher dados de demonstração")
    }

    @After fun closeActivity() {
        if (::scenario.isInitialized) scenario.close()
    }

    @Test fun enteredLoginAndOtpSurviveRotationAndManualPickupWorks() {
        focusWithVisibleIme("Celular").performTextInput("11987654321")
        focusWithVisibleIme("CPF").performTextInput("52998224725")
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, Configuration.ORIENTATION_LANDSCAPE)
        // Assert before any manual scroll or refocus can repair a rotation regression.
        assertFieldAboveVisibleIme("CPF")
        ui.onNodeWithText("11987654321").assertExists()
        ui.onNodeWithText("52998224725").assertExists()
        focusWithVisibleIme("Celular")
        focusWithVisibleIme("CPF")
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

    private fun waitForWindowFocus() {
        try {
            ui.waitUntil(10_000) {
                var focused = false
                scenario.onActivity { focused = it.hasWindowFocus() }
                focused
            }
        } catch (error: ComposeTimeoutException) {
            throw AssertionError("Validation app did not obtain window focus. Check for a system dialog or another foreground window.", error)
        }
    }

    private fun focusWithVisibleIme(label: String): SemanticsNodeInteraction {
        val field = ui.onNode(hasSetTextAction() and hasText(label))
        field.performScrollTo().performClick()
        assertFieldAboveVisibleIme(label)
        return field
    }

    private fun assertFieldAboveVisibleIme(label: String) {
        val field = ui.onNode(hasSetTextAction() and hasText(label))
        var latest: FieldImeSample? = null
        var stableSince = 0L
        try {
            ui.waitUntil(10_000) {
                val window = keyboardWindow() ?: return@waitUntil false
                val node = runCatching { field.fetchSemanticsNode() }.getOrNull() ?: return@waitUntil false
                val sample = ui.runOnIdle {
                    FieldImeSample(window, node.boundsInWindow, Rect(node.positionInWindow, node.size.toSize()),
                        node.config.getOrNull(SemanticsProperties.Focused) == true)
                }
                val now = SystemClock.uptimeMillis()
                if (sample != latest) { latest = sample; stableSince = now }
                sample.isFullyVisible() && now - stableSince >= 250
            }
        } catch (error: ComposeTimeoutException) {
            throw AssertionError("The complete focused $label field must remain above a visible, settled IME. Geometry: $latest", error)
        }
        field.assertIsFocused()
    }

    private fun keyboardWindow(): KeyboardWindow? {
        var result: KeyboardWindow? = null
        scenario.onActivity { activity ->
            val insets = ViewCompat.getRootWindowInsets(activity.window.decorView) ?: return@onActivity
            // Full window bounds do not shrink with adjustResize; avoid subtracting the IME twice.
            val bounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity).bounds
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            result = KeyboardWindow(insets.isVisible(WindowInsetsCompat.Type.ime()),
                insets.getInsets(WindowInsetsCompat.Type.ime()).bottom, bounds.width(), bounds.height(),
                systemBars.top, systemBars.left, systemBars.right)
        }
        return result
    }

    private data class KeyboardWindow(
        val imeVisible: Boolean, val imeBottom: Int, val width: Int, val height: Int,
        val safeTop: Int, val safeLeft: Int, val safeRight: Int,
    )
    private data class FieldImeSample(
        val window: KeyboardWindow, val clipped: Rect, val complete: Rect, val focused: Boolean,
    ) {
        fun isFullyVisible(): Boolean {
            val tolerance = 1f
            return focused && window.imeVisible && window.imeBottom > 0 &&
                complete.width > 0 && complete.height > 0 &&
                kotlin.math.abs(clipped.left - complete.left) <= tolerance &&
                kotlin.math.abs(clipped.top - complete.top) <= tolerance &&
                kotlin.math.abs(clipped.right - complete.right) <= tolerance &&
                kotlin.math.abs(clipped.bottom - complete.bottom) <= tolerance &&
                complete.top >= window.safeTop - tolerance &&
                complete.left >= window.safeLeft - tolerance &&
                complete.right <= window.width - window.safeRight + tolerance &&
                complete.bottom <= window.height - window.imeBottom + tolerance
        }
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
