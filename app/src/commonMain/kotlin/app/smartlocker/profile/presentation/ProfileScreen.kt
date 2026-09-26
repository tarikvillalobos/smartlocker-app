package app.smartlocker.profile.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.smartlocker.auth.domain.LoginChannel
import app.smartlocker.config.Environment
import app.smartlocker.design.*
import app.smartlocker.platform.PlatformServices
import app.smartlocker.shared.presentation.*

@Composable
fun ProfileScreen(state: AppState, controller: AppController, platform: PlatformServices) {
    val profile = state.profile ?: return
    val brand = controller.brand
    val scope = rememberCoroutineScope()
    var permission by remember { mutableStateOf("Consultar permissão do dispositivo") }
    PageTitle("Perfil")
    Panel(dark = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = Color(brand.primary)) {
                Text(profile.name.split(" ").take(2).joinToString("") { it.take(1) },
                    Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(profile.name, style = MaterialTheme.typography.titleLarge)
                Text(brand.name, color = Tokens.soft, style = MaterialTheme.typography.bodySmall)
                Text(listOfNotNull(state.membership?.location, state.membership?.unit?.takeIf { it.isNotBlank() }).joinToString(" · "), color = Tokens.soft,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    Text("DADOS DE CONTATO", style = MaterialTheme.typography.labelMedium, color = Tokens.secondary)
    Panel {
        Metadata("Celular", profile.phone.ifBlank { "Não informado" })
        HorizontalDivider(color = Tokens.border)
        Metadata("E-mail", profile.email.ifBlank { "Não informado" })
        if (controller.features.contactEditing) MenuRow("Editar e verificar contato", Symbol.EDIT) { controller.navigate(Route.CONTACT) }
    }
    Text("AVISOS DE ENCOMENDA", style = MaterialTheme.typography.labelMedium, color = Tokens.secondary)
    Panel {
        PreferenceRow("Notificação no aplicativo", profile.preferences.inApp, !state.busy && "app" in state.channels(brand)) {
            controller.preferences(profile.preferences.copy(inApp = it))
        }
        if ("sms" in state.channels(brand)) PreferenceRow("SMS", profile.preferences.sms, !state.busy) {
            controller.preferences(profile.preferences.copy(sms = it))
        }
        if ("whatsapp" in state.channels(brand)) PreferenceRow("WhatsApp", profile.preferences.whatsapp, !state.busy) {
            controller.preferences(profile.preferences.copy(whatsapp = it))
        }
        Text("Preferência salva no app. O envio depende do fornecedor e da permissão do dispositivo.",
            style = MaterialTheme.typography.bodySmall, color = Tokens.secondary)
        Text(if (controller.configuration.environment == Environment.DEMO) "Demonstração: nenhum canal externo envia mensagens."
            else "Disponibilidade dos canais: aguardando contrato da API.", style = MaterialTheme.typography.bodySmall)
        TextButton({ scope.launchPermission(platform) { permission = it } }) { Text(permission) }
    }
    Panel {
        MenuRow("Meus locais e unidades", Symbol.LOCATION) { controller.navigate(Route.LOCATIONS) }
        if (controller.features.residents) MenuRow("Moradores da unidade", Symbol.USER) { controller.navigate(Route.RESIDENTS) }
        MenuRow("Ajuda e suporte", Symbol.HELP) { controller.navigate(Route.SUPPORT) }
        MenuRow("Termos e privacidade", Symbol.HELP) { controller.navigate(Route.LEGAL) }
        if (controller.configuration.environment == Environment.DEMO) MenuRow("Cenários de demonstração", Symbol.PARCEL) {
            controller.navigate(Route.DEMO)
        }
        TextButton(controller::logout, Modifier.fillMaxWidth(), enabled = !state.busy) {
            AppIcon(Symbol.EXIT, tint = Tokens.destructive)
            Spacer(Modifier.width(12.dp))
            Text("Sair", color = Tokens.destructive)
        }
    }
}

@Composable
private fun PreferenceRow(label: String, value: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f))
        Switch(value, onChange, enabled = enabled, modifier = Modifier.semanticsLabel(label))
    }
}

@Composable
fun ContactScreen(state: AppState, controller: AppController) {
    var contact by rememberSaveable { mutableStateOf(state.contactValue) }
    var code by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf(state.contactChannel == LoginChannel.EMAIL) }
    PageTitle("Editar contato", { controller.navigate(Route.PROFILE) })
    Text("O novo contato só será salvo depois da verificação.", color = Tokens.secondary)
    if (state.contactChallenge == null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(email, { email = it; contact = "" })
            Text(if (email) "E-mail" else "Celular")
        }
        OutlinedTextField(contact, { contact = it }, Modifier.fillMaxWidth().keepAboveKeyboard(), label = { Text("Novo contato") }, shape = Tokens.control)
        PrimaryButton("Verificar novo contato", !state.busy) {
            controller.contact(contact, if (email) LoginChannel.EMAIL else LoginChannel.SMS)
        }
    } else {
        Text("Confira o código enviado para ${state.contactValue}.")
        if (controller.configuration.environment == Environment.DEMO) Text("Código demonstrativo: 123456")
        OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth().keepAboveKeyboard(), label = { Text("Código recebido") })
        PrimaryButton("Confirmar alteração", !state.busy && code.length == 6) { controller.verifyContact(code) }
        val seconds = ((state.contactChallenge.resendAt - state.now + 999) / 1000).coerceAtLeast(0)
        TextButton(controller::resendContact, enabled = !state.busy && seconds == 0L) {
            Text(if (seconds > 0) "Reenviar em ${seconds}s" else "Reenviar código")
        }
        TextButton({ code = ""; controller.correctProfileContact() }, enabled = !state.busy) { Text("Corrigir novo contato") }
    }
}
