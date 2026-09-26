package app.smartlocker.profile.presentation

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.smartlocker.platform.PlatformServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun Modifier.semanticsLabel(label: String) = semantics { contentDescription = label }
fun CoroutineScope.launchPermission(platform: PlatformServices, result: (String) -> Unit) = launch {
    result(platform.notificationPermission())
}
