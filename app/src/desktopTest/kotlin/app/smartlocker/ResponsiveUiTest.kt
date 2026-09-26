package app.smartlocker

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.toAwtImage
import app.smartlocker.config.*
import app.smartlocker.platform.PlatformServices
import app.smartlocker.shared.presentation.Route
import java.io.File
import javax.imageio.ImageIO
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestPlatform : PlatformServices {
    override val local = MemoryStorage()
    override val secure = MemorySecure()
    var copied: String? = null
    override fun copyText(value: String) { copied = value }
    override fun openLink(url: String) = false
    override suspend fun notificationPermission() = "Teste: não autorizado"
}

@OptIn(ExperimentalTestApi::class)
class ResponsiveUiTest {
    @Test fun allReferenceScreensAtRepresentativeWidthsAndDoubleFontScale() {
        for (width in listOf(320, 390, 430, 600, 840, 1200)) {
            for (fontScale in listOf(1f, 2f)) {
                runDesktopComposeUiTest(width = width, height = 960) {
                    val runtime = AppRuntime(TestPlatform(), AppConfiguration(Brands.smartLocker, Environment.DEMO))
                    val controller = runtime.state.value.controller
                    try {
                        setContent {
                            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) { SmartLockerApp(runtime) }
                        }
                        waitUntil(timeoutMillis = 10_000) { controller.state.value.initialized }
                        capture("login", width, fontScale)
                        runOnIdle { controller.login(demoLogin) }
                        runOnIdle { controller.verify("123456") }
                        waitUntil(15_000) { controller.state.value.profile != null && !controller.state.value.busy }
                        for ((route, name) in listOf(Route.HOME to "home", Route.HISTORY to "history",
                            Route.DETAIL to "detail", Route.PROFILE to "profile")) {
                            runOnIdle { controller.navigate(route) }
                            waitForIdle()
                            capture(name, width, fontScale)
                            val root = onRoot().fetchSemanticsNode().boundsInRoot
                            assertEquals(width.toFloat(), root.width)
                            onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
                                val bounds = node.boundsInRoot
                                if (bounds.width > 0 && bounds.height > 0) {
                                    assertTrue(bounds.left >= -1 && bounds.right <= width + 1, "$name: horizontal overflow $bounds")
                                }
                            }
                        }
                    } finally { runOnIdle { runtime.close() } }
                }
            }
        }
    }

    @Test fun loginAndManualPickupWorkThroughAccessibleActions() = runDesktopComposeUiTest(width = 390, height = 1100) {
        val platform = TestPlatform()
        val runtime = AppRuntime(platform, AppConfiguration(Brands.smartLocker, Environment.DEMO))
        try {
            setContent { SmartLockerApp(runtime) }
            waitUntil(10_000) { runtime.state.value.controller.state.value.initialized }
            onNodeWithText("Preencher dados de demonstração").performScrollTo().performClick()
            onNodeWithText("Receber código por SMS").performScrollTo().performClick()
            waitUntil(10_000) { runtime.state.value.controller.state.value.challenge != null }
            onNodeWithText("Código de 6 dígitos").performScrollTo().performTextInput("123456")
            onNodeWithText("Confirmar código").performScrollTo().performClick()
            waitUntil(15_000) { runtime.state.value.controller.state.value.profile != null }
            onNodeWithText("Copiar código").performScrollTo().performClick()
            assertTrue(platform.copied?.length == 6)
            onNodeWithText("Fechar").performClick()
            onNodeWithContentDescription("Ver detalhes").performScrollTo().performClick()
            onNodeWithText("Já retirei a encomenda").performScrollTo().performClick()
            onNodeWithText("Sim, retirei").performClick()
            waitUntil(10_000) { runtime.state.value.controller.state.value.selected?.manualAt != null }
            onNodeWithText("Retirada informada").assertExists()
        } finally { runOnIdle { runtime.close() } }
    }

    private fun ComposeUiTest.capture(screen: String, width: Int, fontScale: Float) {
        val directory = File("build/reports/screenshots").apply { mkdirs() }
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(directory, "$screen-$width-${fontScale.toInt()}x.png"))
    }
}
