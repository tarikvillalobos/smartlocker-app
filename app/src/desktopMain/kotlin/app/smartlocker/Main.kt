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
        val platform = DesktopServices()
        val specifiedBrand = args.firstOrNull { it.startsWith("--brand=") }?.substringAfter("=")
            ?: if ("--aurora" in args) "aurora" else null
        val brand = specifiedBrand?.let { id -> Brands.all.single { it.id == id } }
        val environment = if ("--demo" in args) Environment.DEMO else platform.local.read("environment")
            ?.let { runCatching { Environment.valueOf(it) }.getOrNull() } ?: Environment.PRODUCTION
        val configuration = if (brand != null || "--demo" in args)
            AppConfiguration(brand ?: Brands.smartLocker, environment) else null
        AppRuntime(platform, configuration)
    }
    val holder by runtime.state.collectAsState()
    Window(onCloseRequest = { runtime.close(); exitApplication() }, title = holder.configuration.brand.name,
        state = rememberWindowState(width = 1100.dp, height = 900.dp)) {
        SmartLockerApp(runtime)
    }
}
