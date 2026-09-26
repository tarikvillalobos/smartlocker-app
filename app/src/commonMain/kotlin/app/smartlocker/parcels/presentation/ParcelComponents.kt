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
    val parcel = state.selected ?: return
    Panel(dark = true) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(parcel)
            Text("Retire até ${dateTime(parcel.deadline).substringBefore(" ·")}",
                color = Tokens.soft, style = MaterialTheme.typography.bodySmall)
        }
        if (detail) {
            Text(parcel.carrier, style = MaterialTheme.typography.titleMedium)
            parcel.tracking?.let { Text("Rastreio $it", color = Tokens.soft, style = MaterialTheme.typography.bodySmall) }
        }
        Surface(shape = Tokens.card, color = Color.White, contentColor = Tokens.text) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val credential = state.credential
                if (credential != null && credential.canDisplay(state.now, !state.stale)) {
                    PickupQr(credential.payload, Modifier.widthIn(max = if (detail) 232.dp else 208.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        credential.code.chunked(3).forEach { part ->
                            Text(part, style = MaterialTheme.typography.displaySmall,
                                letterSpacing = 4.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    if (detail) Text("Aproxime do leitor do armário ou digite o código no painel.",
                        style = MaterialTheme.typography.bodySmall, color = Tokens.secondary)
                } else if (parcel.status != ParcelStatus.WAITING) {
                    Surface(shape = CircleShape, color = Tokens.success) {
                        AppIcon(Symbol.CHECK, modifier = Modifier.padding(20.dp).size(40.dp), tint = Tokens.successText)
                    }
                    Text(if (parcel.status == ParcelStatus.MANUAL) "Retirada informada" else "Encomenda retirada",
                        style = MaterialTheme.typography.headlineSmall)
                    Text(if (parcel.status == ParcelStatus.MANUAL) "Marcada por você. O armário ainda não confirmou."
                        else "Confirmação física ${dateTime(parcel.collectedAt!!)}",
                        color = Tokens.secondary, style = MaterialTheme.typography.bodyMedium)
                    Text("O código anterior está finalizado.", style = MaterialTheme.typography.bodySmall)
                } else {
                    AppIcon(Symbol.HELP, modifier = Modifier.size(36.dp), tint = Tokens.warningText)
                    Text(state.credentialMessage ?: if (state.stale) "Conecte-se para verificar o código."
                        else "Atualize para verificar a validade do código.", color = Tokens.secondary)
                    TextButton(controller::refresh, enabled = !state.busy) { Text("Verificar código") }
                }
            }
        }
        Text("${parcel.locker} · Porta ${parcel.compartment}${parcel.size?.let { " ($it)" } ?: ""}",
            style = MaterialTheme.typography.bodySmall, color = Tokens.soft)
        Text("Chegou em ${dateTime(parcel.depositedAt)}", style = MaterialTheme.typography.bodySmall, color = Tokens.soft)
        if (parcel.deadline < state.now && parcel.status == ParcelStatus.WAITING) {
            Text("Prazo vencido · entre em contato com o suporte", color = Color(0xFFFFD99A))
        }
        val credential = state.credential
        if (credential?.canDisplay(state.now, !state.stale) == true) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    PrimaryButton("Copiar código") {
                        platform.copyText(credential.code)
                        controller.feedback("Código copiado.")
                    }
                }
                if (!detail) OutlinedIconButton({ controller.navigate(Route.DETAIL) }) {
