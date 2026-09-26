package app.smartlocker.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.smartlocker.config.Brand
import app.smartlocker.resources.*
import org.jetbrains.compose.resources.Font

object Tokens {
    val text = Color(0xFF0F1F1B)
    val secondary = Color(0xFF4F605B)
    val background = Color(0xFFF3F7F5)
    val border = Color(0xFFE1EAE6)
    val strongBorder = Color(0xFFD6E2DD)
    val soft = Color(0xFFCFE3DC)
    val success = Color(0xFFE1F2EC)
    val successText = Color(0xFF005C47)
    val warning = Color(0xFFFFF1DC)
    val warningText = Color(0xFF8A4B00)
    val deadline = Color(0xFFE0691B)
    val destructive = Color(0xFFB42318)
    val space = 4.dp
    val gutter = 20.dp
    val gap = 16.dp
    val fieldHeight = 52.dp
    val touch = 48.dp
    val card = RoundedCornerShape(18.dp)
    val hero = RoundedCornerShape(24.dp)
    val control = RoundedCornerShape(14.dp)
    val maxForm = 480.dp
    val maxContent = 1140.dp
}

