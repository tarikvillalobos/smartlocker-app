package app.smartlocker.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import qrcode.raw.QRCodeProcessor
import kotlin.math.floor

/** Encodes an authorized payload; never creates a pickup credential. */
fun qrMatrix(payload: String): List<List<Boolean>> {
    require(payload.isNotEmpty() && payload.length <= 2048)
    return QRCodeProcessor(payload).encode().map { row -> row.map { it.dark } }
