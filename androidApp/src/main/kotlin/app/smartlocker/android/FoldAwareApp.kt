package app.smartlocker.android

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import app.smartlocker.AppRuntime
import app.smartlocker.SmartLockerApp

/** Keep the entire interactive app in the larger unobstructed pane. */
@Composable
fun FoldAwareApp(activity: ComponentActivity, runtime: AppRuntime) {
    val info = remember(activity) { WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity) }
    val layout by info.collectAsState(initial = null)
    val fold = layout?.displayFeatures?.filterIsInstance<FoldingFeature>()?.firstOrNull { it.isSeparating }
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val padding = if (fold == null) PaddingValues(0.dp) else with(density) {
            val bounds = fold.bounds
            if (fold.orientation == FoldingFeature.Orientation.VERTICAL) {
                val left = bounds.left.toDp()
                val right = bounds.right.toDp()
                if (left > maxWidth - right) PaddingValues(end = maxWidth - left)
                else PaddingValues(start = right)
            } else {
                val top = bounds.top.toDp()
                val bottom = bounds.bottom.toDp()
                if (top > maxHeight - bottom) PaddingValues(bottom = maxHeight - top)
                else PaddingValues(top = bottom)
            }
        }
        SmartLockerApp(runtime, Modifier.padding(padding))
    }
}
