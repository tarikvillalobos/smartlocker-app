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
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = Tokens.control, color = if (parcel.status == ParcelStatus.WAITING) Tokens.warning else Tokens.success) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    AppIcon(Symbol.PARCEL, tint = if (parcel.status == ParcelStatus.WAITING) Tokens.warningText else Tokens.successText)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(parcel.carrier, style = MaterialTheme.typography.labelLarge)
                Text("${dateTime(parcel.collectedAt ?: parcel.depositedAt)} · Porta ${parcel.compartment}",
                    style = MaterialTheme.typography.bodySmall, color = Tokens.secondary)
                StatusBadge(parcel)
            }
            AppIcon(Symbol.NEXT, modifier = Modifier.padding(top = 10.dp), tint = Tokens.secondary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PickupCard(state: AppState, controller: AppController, platform: PlatformServices, detail: Boolean = false) {
