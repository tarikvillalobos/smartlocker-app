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
        }
        Route.RESIDENTS -> {
            PageTitle("Moradores", { controller.navigate(Route.PROFILE) })
            Text("A lista de moradores não concede acesso às encomendas de outras pessoas.", color = Tokens.secondary)
            state.residents.forEach { resident -> Panel { Metadata(resident.relationship, resident.name) } }
        }
        Route.ISSUES -> IssueScreen(state, controller)
        Route.CONTACT -> app.smartlocker.profile.presentation.ContactScreen(state, controller)
        Route.DEMO -> {
            PageTitle("Demonstração", { controller.navigate(Route.PROFILE) })
            Text("Dados fictícios locais. Nenhum hardware ou fornecedor é acionado.", color = Tokens.secondary)
            PrimaryButton("Simular nova entrega", !state.busy, controller::deposit)
            val scenarios = listOf(
                DemoScenario.NORMAL to "Restaurar exemplo inicial",
                DemoScenario.EMPTY to "Nenhuma encomenda",
                DemoScenario.MANY to "Muitas encomendas",
                DemoScenario.NETWORK to "Falha de rede",
                DemoScenario.DENIED to "Acesso negado",
                DemoScenario.EXPIRED_SESSION to "Sessão expirada",
                DemoScenario.EXPIRED_CODE to "Código e prazo expirados",
                DemoScenario.LOCKER_OFFLINE to "Armário indisponível",
            )
            scenarios.forEach { (scenario, title) ->
                OutlinedButton({ controller.demoScenario(scenario) }, Modifier.fillMaxWidth(), enabled = !state.busy) { Text(title) }
            }
        }
        Route.LEGAL -> LegalScreen(controller, platform)
        Route.SUPPORT -> {
            PageTitle("Ajuda e suporte", { controller.navigate(Route.PROFILE) })
            Panel {
                Text("Como retirar sua encomenda", style = MaterialTheme.typography.titleMedium)
                Text("No armário indicado, apresente o QR Code ao leitor ou digite o código no painel. Confira a porta informada.")
                Text("Se o código estiver expirado ou o armário indisponível, abra os detalhes e relate o problema.")
            }
            if (brand.features.issues) MenuRow("Acompanhar solicitações", Symbol.HELP) { controller.navigate(Route.ISSUES) }
            TextButton({
                val email = brand.supportEmail
                if (email == null || !platform.openLink("mailto:$email")) controller.feedback("Canal externo de suporte ainda não configurado.")
            }) { Text("Contato do suporte") }
        }
