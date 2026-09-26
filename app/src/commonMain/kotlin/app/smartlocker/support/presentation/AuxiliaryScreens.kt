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
    val brand = controller.brand
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
            if (state.noticeCursor != null) PrimaryButton("Carregar mais avisos", !state.busy, controller::moreNotices)
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
                DemoScenario.LONG_TEXT to "Nomes e endereços longos",
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
            if (controller.features.issues) MenuRow("Acompanhar solicitações", Symbol.HELP) { controller.navigate(Route.ISSUES) }
            TextButton({
                val email = brand.supportEmail
                if (email == null || !platform.openLink("mailto:$email")) controller.feedback("Canal externo de suporte ainda não configurado.")
            }) { Text("Contato do suporte") }
        }
        else -> Unit
    }
}

@Composable
private fun IssueScreen(state: AppState, controller: AppController) {
    var message by rememberSaveable(state.selectedId) { mutableStateOf("") }
    var observedSubmission by rememberSaveable { mutableLongStateOf(state.issueSubmission) }
    LaunchedEffect(state.issueSubmission) {
        if (state.issueSubmission != observedSubmission) message = ""
        observedSubmission = state.issueSubmission
    }
    PageTitle("Solicitações", { controller.navigate(if (state.selectedId != null) Route.DETAIL else Route.PROFILE) })
    if (state.selectedId != null && state.selected?.canReportIssue == true && controller.features.issues) Panel {
        Text("Problema com ${state.selected?.carrier ?: "a encomenda selecionada"}", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(message, { message = it.take(2000) }, Modifier.fillMaxWidth().keepAboveKeyboard(),
            label = { Text("Descreva o problema") }, minLines = 3, shape = Tokens.control)
        PrimaryButton("Enviar relato", !state.busy && !state.stale && message.trim().length >= 10) { controller.report(message) }
        if (controller.configuration.environment == Environment.DEMO) Text("Solicitações fictícias, salvas apenas neste dispositivo.",
            style = MaterialTheme.typography.bodySmall, color = Tokens.secondary)
    }
    if (state.issues.isEmpty()) EmptyState("Nenhuma solicitação", "Relate problemas a partir do detalhe da encomenda.")
        Panel {
            Text("${issue.id} · ${issue.status}", style = MaterialTheme.typography.labelLarge)
            Text(issue.message)
            Text(dateTime(issue.createdAt), style = MaterialTheme.typography.bodySmall, color = Tokens.secondary)
        }
    }
}

@Composable
fun LegalScreen(controller: AppController, platform: PlatformServices) {
    PageTitle("Termos e privacidade", { controller.navigate(Route.PROFILE) })
    Panel {
        Text("SmartLocker App · software privado e proprietário", style = MaterialTheme.typography.titleMedium)
        Text("Esta demonstração usa dados fictícios. Documentos legais de cada marca deverão ser configurados antes da distribuição.")
        listOf("Termos de uso" to controller.configuration.brand.termsUrl,
            "Política de privacidade" to controller.configuration.brand.privacyUrl).forEach { (label, url) ->
            TextButton({
                if (url == null || !platform.openLink(url)) controller.feedback("Documento desta marca ainda não configurado.")
            }) { Text(label) }
        }
    }
}
