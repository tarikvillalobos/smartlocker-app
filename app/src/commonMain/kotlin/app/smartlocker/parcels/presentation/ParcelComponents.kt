package app.smartlocker.parcels.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.smartlocker.design.*
import app.smartlocker.parcels.domain.*
import app.smartlocker.platform.PlatformServices
import app.smartlocker.shared.presentation.*

@Composable
fun ParcelRow(parcel: Parcel, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = Tokens.card, color = Color.White, modifier = Modifier.fillMaxWidth()) {
