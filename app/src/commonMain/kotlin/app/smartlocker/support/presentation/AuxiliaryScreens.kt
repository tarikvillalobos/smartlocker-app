package app.smartlocker.support.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.smartlocker.config.Environment
import app.smartlocker.design.*
import app.smartlocker.platform.PlatformServices
import app.smartlocker.shared.domain.DemoScenario
import app.smartlocker.shared.presentation.*

@Composable
fun AuxiliaryScreen(state: AppState, controller: AppController, platform: PlatformServices) {
    val brand = controller.configuration.brand
    when (state.route) {
        Route.NOTICES -> {
            PageTitle("Notificações", { controller.navigate(Route.HOME) })
            if (state.notices.isEmpty()) EmptyState("Nenhum aviso", "As notificações das suas entregas aparecerão aqui.")
            state.notices.forEach { notice ->
                Panel {
                    Text(notice.title, style = MaterialTheme.typography.titleMedium)
                    Text(dateTime(notice.createdAt), color = Tokens.secondary)
                    Text(if (notice.read) "Lida" else "Não lida", style = MaterialTheme.typography.labelSmall)
                    TextButton({ controller.notice(notice) }) { Text("Ver encomenda") }
                }
            }
        }
        Route.LOCATIONS -> {
            PageTitle("Meus locais", { controller.navigate(Route.PROFILE) })
            state.profile?.memberships?.forEach { member ->
                Panel {
                    Text(member.location, style = MaterialTheme.typography.titleMedium)
                    Text(member.unit)
                    PrimaryButton(if (member.id == state.membershipId) "Local selecionado" else "Selecionar local",
                        member.id != state.membershipId) { controller.membership(member.id) }
                }
            }
