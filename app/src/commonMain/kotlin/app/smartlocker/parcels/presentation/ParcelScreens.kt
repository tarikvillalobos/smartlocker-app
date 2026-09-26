package app.smartlocker.parcels.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.smartlocker.config.Environment
import app.smartlocker.design.*
import app.smartlocker.parcels.domain.*
import app.smartlocker.platform.PlatformServices
import app.smartlocker.shared.presentation.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(state: AppState, controller: AppController, platform: PlatformServices) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Olá, ${state.profile?.name?.substringBefore(' ') ?: ""}", color = Tokens.secondary)
            Text(state.membership?.unit.orEmpty(), style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary)
            Text(state.membership?.location.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Tokens.secondary)
        }
        IconButton({ controller.navigate(Route.NOTICES) }) {
            BadgedBox(badge = { if (state.unreadCount > 0) Badge { Text(state.unreadCount.toString()) } }) {
                AppIcon(Symbol.BELL, "Notificações")
            }
        }
    }
    if (state.pending.isEmpty()) EmptyState("Tudo em dia!", "Nenhuma encomenda aguardando retirada neste local.")
    else {
        if (state.pending.size > 1) {
            Text("${state.pending.size} encomendas aguardando", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.pending.forEach { parcel ->
                    FilterChip(parcel.id == state.selectedId, { controller.select(parcel.id, false) },
                        label = { Text("${parcel.carrier} · ${parcel.compartment}") })
                }
            }
