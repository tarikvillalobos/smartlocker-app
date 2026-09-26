package app.smartlocker

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.smartlocker.config.*
import app.smartlocker.platform.DesktopServices

fun main(args: Array<String>) = application {
    val runtime = remember {
        val brand = if ("--aurora" in args) Brands.aurora else Brands.smartLocker
        val configuration = if ("--demo" in args) AppConfiguration(brand, Environment.DEMO) else null
        AppRuntime(DesktopServices(), configuration)
    }
    val holder by runtime.state.collectAsState()
    Window(onCloseRequest = { runtime.close(); exitApplication() }, title = holder.configuration.brand.name,
        state = rememberWindowState(width = 1100.dp, height = 900.dp)) {
        SmartLockerApp(runtime)
    }
}
