package app.smartlocker.design

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

enum class Symbol(val path: String) {
    HOME("M3 10 L12 3 L21 10 V21 H15 V15 H9 V21 H3 Z"),
    HISTORY("M3 12 A9 9 0 1 0 6 5.3 L3 8 M3 3 V8 H8 M12 7 V12 L15 14"),
    USER("M16 8 A4 4 0 1 1 8 8 A4 4 0 1 1 16 8 M4 21 A8 8 0 0 1 20 21"),
    PARCEL("M21 8 L12 3 L3 8 V16 L12 21 L21 16 Z M3 8 L12 13 L21 8 M12 13 V21"),
    BELL("M6 8 A6 6 0 0 1 18 8 C18 15 21 17 21 17 H3 C3 17 6 15 6 8 M10 21 H14"),
