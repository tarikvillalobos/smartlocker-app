package app.smartlocker

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.ComposeUIViewController
import app.smartlocker.platform.IosServices

fun MainViewController(readSecret: (String) -> String?, writeSecret: (String, String?) -> Boolean) =
    ComposeUIViewController {
        val runtime = androidx.compose.runtime.remember { AppRuntime(IosServices(readSecret, writeSecret)) }
        DisposableEffect(runtime) { onDispose { runtime.close() } }
        SmartLockerApp(runtime)
    }
