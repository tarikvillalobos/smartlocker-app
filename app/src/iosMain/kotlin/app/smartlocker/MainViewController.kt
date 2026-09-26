package app.smartlocker

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.uikit.OnFocusBehavior
import app.smartlocker.platform.IosServices

fun MainViewController(readSecret: (String) -> String?, writeSecret: (String, String?) -> Boolean) =
        val runtime = androidx.compose.runtime.remember { AppRuntime(IosServices(readSecret, writeSecret)) }
        DisposableEffect(runtime) { onDispose { runtime.close() } }
        SmartLockerApp(runtime)
    }
