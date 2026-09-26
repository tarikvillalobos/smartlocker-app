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
    BACK("M15 18 L9 12 L15 6"),
    NEXT("M9 18 L15 12 L9 6"),
    COPY("M9 9 H22 V22 H9 Z M5 15 H2 V2 H15 V5"),
    CHECK("M5 12 L10 17 L19 7"),
    HELP("M21 12 A9 9 0 1 1 3 12 A9 9 0 1 1 21 12 M9 9 A3 3 0 0 1 15 10 C15 12 12 13 12 14 M12 17 V17.1"),
    EDIT("M12 20 H21 M16 4 L20 8 L7 21 L3 22 L4 18 Z"),
    EXIT("M9 21 H3 V3 H9 M16 17 L21 12 L16 7 M21 12 H9"),
    REFRESH("M3 12 A9 9 0 1 0 6 5.3 M3 3 V8 H8"),
    LOCATION("M20 10 C20 16 12 22 12 22 C12 22 4 16 4 10 A8 8 0 0 1 20 10 M15 10 A3 3 0 1 1 9 10 A3 3 0 1 1 15 10"),
}

@Composable
fun AppIcon(symbol: Symbol, description: String? = null, modifier: Modifier = Modifier,
            tint: Color = androidx.compose.material3.LocalContentColor.current) {
    val vector = remember(symbol) {
        ImageVector.Builder(symbol.name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(PathParser().parsePathString(symbol.path).toNodes(),
                stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }.build()
