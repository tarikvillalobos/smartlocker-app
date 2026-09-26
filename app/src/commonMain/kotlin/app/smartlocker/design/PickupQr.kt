package app.smartlocker.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import qrcode.raw.QRCodeProcessor
import kotlin.math.floor

/** Encodes an authorized payload; never creates a pickup credential. */
fun qrMatrix(payload: String): List<List<Boolean>> {
    require(payload.isNotEmpty() && payload.length <= 2048)
    return QRCodeProcessor(payload).encode().map { row -> row.map { it.dark } }
}

/** Returns no symbol when the authorized content exceeds QR capacity; never truncates it. */
fun qrMatrixOrNull(payload: String): List<List<Boolean>>? = try {
    qrMatrix(payload)
} catch (_: IllegalArgumentException) {
    null
}

@Composable
fun PickupQr(payload: String, modifier: Modifier = Modifier) {
    val matrix = remember(payload) { qrMatrixOrNull(payload) }
    if (matrix == null) {
        Text("QR Code indisponível. Use o código numérico abaixo.",
            modifier.widthIn(max = 232.dp).fillMaxWidth(), style = MaterialTheme.typography.bodyMedium,
            color = Tokens.secondary, textAlign = TextAlign.Center)
        return
    }
    Canvas(modifier.widthIn(max = 232.dp).fillMaxWidth().aspectRatio(1f)
        .background(Color.White).semantics { contentDescription = "QR Code de retirada; código numérico disponível abaixo" }) {
        val cells = matrix.size + 8 // Four white modules on every edge.
        val unit = floor(size.minDimension / cells).coerceAtLeast(1f)
        val inset = (size.minDimension - unit * cells) / 2
        matrix.forEachIndexed { row, values ->
            values.forEachIndexed { col, dark ->
                if (dark) drawRect(Color.Black, Offset(inset + (col + 4) * unit, inset + (row + 4) * unit), Size(unit, unit))
            }
        }
    }
}
