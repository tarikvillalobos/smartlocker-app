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
                        waitUntil(10_000) { controller.state.value.initialized }
                        capture("login", width, fontScale)
                        runOnIdle { controller.login(demoLogin) }
