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
    val brand = controller.configuration.brand
    val scope = rememberCoroutineScope()
    var permission by remember { mutableStateOf("Consultar permissão do dispositivo") }
    PageTitle("Perfil")
    Panel(dark = true) {
        Text(profile.name, style = MaterialTheme.typography.titleLarge)
        Text(brand.name, color = Tokens.soft)
        Text("${state.membership?.location} · ${state.membership?.unit}", color = Tokens.soft)
    }
    Text("DADOS DE CONTATO", style = MaterialTheme.typography.labelMedium, color = Tokens.secondary)
    Panel {
        Metadata("Celular", profile.phone)
        HorizontalDivider(color = Tokens.border)
        Metadata("E-mail", profile.email)
        if (brand.features.contactEditing) MenuRow("Editar e verificar contato", Symbol.EDIT) { controller.navigate(Route.CONTACT) }
    }
    Text("AVISOS DE ENCOMENDA", style = MaterialTheme.typography.labelMedium, color = Tokens.secondary)
    Panel {
        PreferenceRow("Notificação no aplicativo", profile.preferences.inApp, !state.busy) {
            controller.preferences(profile.preferences.copy(inApp = it))
        }
