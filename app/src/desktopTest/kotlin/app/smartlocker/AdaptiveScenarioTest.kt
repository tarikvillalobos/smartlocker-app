package app.smartlocker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.smartlocker.config.*
import app.smartlocker.parcels.domain.ParcelFilter
import app.smartlocker.shared.domain.DemoScenario
import app.smartlocker.shared.presentation.Route
import java.io.File
import javax.imageio.ImageIO
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class AdaptiveScenarioTest {
    @Test fun resizingAndShortLandscapeKeepInputSelectionAndFilter() = runDesktopComposeUiTest(width = 1200, height = 960) {
        val runtime = AppRuntime(TestPlatform(), AppConfiguration(Brands.smartLocker, Environment.DEMO))
        val controller = runtime.state.value.controller
        var width by mutableStateOf(390)
        var height by mutableStateOf(900)
        try {
            setContent { Box(Modifier.size(width.dp, height.dp)) { SmartLockerApp(runtime) } }
            waitUntil(10_000) { controller.state.value.initialized }
            onNodeWithText("Celular").performScrollTo().performTextInput("11987654321")
            onNodeWithText("CPF").performScrollTo().performTextInput("52998224725")
            runOnIdle { width = 840; height = 390 }
            onNodeWithText("11987654321").assertExists()
            onNodeWithText("52998224725").assertExists()
            onNodeWithText("Receber código por SMS").performScrollTo().assertIsDisplayed()
            runOnIdle { controller.login(demoLogin) }
            waitUntil(10_000) { controller.state.value.challenge != null }
            runOnIdle { controller.verify("123456") }
            waitUntil(15_000) { controller.state.value.profile != null && !controller.state.value.busy }
            runOnIdle { controller.filter(ParcelFilter.WAITING); controller.select("demo-1") }
            waitUntil(15_000) { controller.state.value.selected?.id == "demo-1" && !controller.state.value.busy }
            for ((w, h) in listOf(1200 to 840, 600 to 840, 320 to 600, 840 to 390, 390 to 400)) {
                runOnIdle { width = w; height = h }
                waitForIdle()
                assertEquals(Route.DETAIL, controller.state.value.route)
                assertEquals(ParcelFilter.WAITING, controller.state.value.filter)
                assertEquals("demo-1", controller.state.value.selected?.id)
                onNodeWithText("Já retirei a encomenda").performScrollTo().assertIsDisplayed()
                capture("resize-$w-$h")
            }
        } finally { runOnIdle { runtime.close() } }
    }

    @Test fun longManyAndEmptyContentRemainUsableWithDoubleFontScale() = runDesktopComposeUiTest(width = 390, height = 960) {
        val runtime = AppRuntime(TestPlatform(), AppConfiguration(Brands.smartLocker, Environment.DEMO))
        val controller = runtime.state.value.controller
        try {
            setContent { CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) { SmartLockerApp(runtime) } }
            waitUntil(10_000) { controller.state.value.initialized }
