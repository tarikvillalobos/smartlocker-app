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
fun HomeScreen(state: AppState, controller: AppController, platform: PlatformServices, showRecent: Boolean = true) {
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
            var menu by remember { mutableStateOf(false) }
            Text("Encomendas aguardando", style = MaterialTheme.typography.labelLarge)
            Box {
                OutlinedButton({ menu = true }, Modifier.fillMaxWidth()) {
                    Text("Selecionar entrega · ${state.selected?.carrier ?: "Escolher"}")
                }
                DropdownMenu(menu, { menu = false }) {
                    state.pending.forEach { parcel ->
                        DropdownMenuItem(text = { Text("${parcel.carrier} · Porta ${parcel.compartment}") },
                            onClick = { menu = false; controller.select(parcel.id, false) })
                    }
                    DropdownMenuItem(text = { Text("Ver todas no histórico") }, onClick = {
                        menu = false
                        controller.filter(ParcelFilter.WAITING)
                        controller.navigate(Route.HISTORY)
                    })
                }
            }
        }
        if (state.selected?.status == ParcelStatus.WAITING) PickupCard(state, controller, platform)
        else PrimaryButton("Ver encomenda pendente") { controller.select(state.pending.first().id, false) }
    }
    if (showRecent) RecentParcels(state, controller)
}

@Composable
fun RecentParcels(state: AppState, controller: AppController) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Recentes", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        TextButton({ controller.navigate(Route.HISTORY) }) { Text("Ver histórico") }
    }
    state.recent.forEach { item -> ParcelRow(item) { controller.select(item.id) } }
    if (state.recent.isEmpty()) Text("Suas próximas entregas aparecerão aqui.", color = Tokens.secondary)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(state: AppState, controller: AppController) {
    PageTitle("Histórico")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(ParcelFilter.ALL to "Todas", ParcelFilter.WAITING to "Aguardando", ParcelFilter.COLLECTED to "Retiradas")
            .forEach { (filter, title) ->
                FilterChip(state.filter == filter, { controller.filter(filter) }, label = { Text(title) }, enabled = !state.busy)
            }
    }
    Panel {
        Text("Últimos 30 dias · neste local", style = MaterialTheme.typography.bodySmall, color = Tokens.secondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Metadata("Total recebido", state.statistics?.total?.let { "$it encomendas" } ?: "Sem dados")
            Metadata("Tempo médio para retirar", durationLabel(state.statistics?.averageMillis))
        }
        if (state.statistics?.complete == false) Text("Indicadores indisponíveis para este período.", color = Tokens.secondary)
        Text("Média de retiradas físicas confirmadas. Marcações manuais não entram na média.",
            style = MaterialTheme.typography.bodySmall, color = Tokens.secondary)
    }
    if (state.parcels.isEmpty() && !state.busy) EmptyState("Nenhuma encomenda", "Não há entregas neste filtro.")
    state.parcels.forEach { item -> ParcelRow(item) { controller.select(item.id) } }
    if (state.nextCursor != null) PrimaryButton("Carregar mais", !state.busy, controller::more)
}

@Composable
fun DetailScreen(state: AppState, controller: AppController, platform: PlatformServices) {
    var confirmation by remember { mutableStateOf(false) }
    val parcel = state.selected
    PageTitle("Detalhe da encomenda", { controller.navigate(Route.HISTORY) })
    if (parcel == null) {
        if (!state.busy) EmptyState("Encomenda indisponível", "Atualize ou volte ao histórico para selecionar outra entrega.")
        return
    }
    PickupCard(state, controller, platform, detail = true)
    Panel {
        Metadata("Localização do armário", "${parcel.locker} · ${parcel.address}")
        Metadata("Compartimento", "Porta ${parcel.compartment} · ${parcel.size ?: "Tamanho não informado"}")
    }
    Panel {
        Text("Linha do tempo", style = MaterialTheme.typography.titleMedium)
        TimelineStep("Depositada", dateTime(parcel.depositedAt), true)
        TimelineStep("Aviso disponibilizado", parcel.notifiedAt?.let(::dateTime) ?: "Ainda não informado", parcel.notifiedAt != null)
        TimelineStep(if (parcel.status == ParcelStatus.MANUAL) "Informada por você" else "Retirada física",
            (parcel.collectedAt ?: parcel.manualAt)?.let(::dateTime) ?: "Aguardando retirada", parcel.status != ParcelStatus.WAITING)
    }
    if (parcel.status == ParcelStatus.WAITING && parcel.canMarkManually && controller.features.manualPickup) {
        PrimaryButton("Já retirei a encomenda", !state.busy && !state.stale) { confirmation = true }
    }
    if (parcel.canUndo && controller.configuration.brand.features.manualPickup) {
        TextButton(controller::undo, Modifier.fillMaxWidth(), enabled = !state.busy && !state.stale) { Text("Desfazer marcação manual") }
    }
    if (parcel.status != ParcelStatus.WAITING) PrimaryButton("Ver histórico") { controller.navigate(Route.HISTORY) }
    if (controller.configuration.brand.features.issues) {
        TextButton({ controller.navigate(Route.ISSUES) }, Modifier.fillMaxWidth()) { Text("Relatar um problema") }
    }
    if (controller.configuration.environment == Environment.DEMO && parcel.status == ParcelStatus.WAITING) {
        OutlinedButton(controller::physicalPickup, Modifier.fillMaxWidth(), enabled = !state.busy && !state.stale) {
            Text("Simular retirada física")
        }
    }
    if (confirmation) AlertDialog(onDismissRequest = { confirmation = false },
        title = { Text("Confirmar sua retirada?") },
        text = { Text("Esta marcação informa que você retirou a encomenda. Ela não representa confirmação do armário e revoga o código atual.") },
        confirmButton = { TextButton({ confirmation = false; controller.markCollected() }) { Text("Sim, retirei") } },
        dismissButton = { TextButton({ confirmation = false }) { Text("Cancelar") } })
}

@Composable
private fun TimelineStep(title: String, time: String, done: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(if (done) Symbol.CHECK else Symbol.HISTORY, tint = if (done) Tokens.successText else Tokens.secondary)
        Column { Text(title, style = MaterialTheme.typography.labelMedium); Text(time, style = MaterialTheme.typography.bodySmall, color = Tokens.secondary) }
    }
}
