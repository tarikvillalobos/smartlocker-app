package app.smartlocker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.smartlocker.auth.presentation.LoginScreen
import app.smartlocker.config.*
import app.smartlocker.design.*
import app.smartlocker.parcels.presentation.*
import app.smartlocker.profile.presentation.ProfileScreen
import app.smartlocker.shared.presentation.*
import app.smartlocker.support.presentation.*

@Composable
fun SmartLockerApp(runtime: AppRuntime, modifier: Modifier = Modifier) {
    val holder by runtime.state.collectAsState()
    val controller = holder.controller
    val state by controller.state.collectAsState()
    SmartLockerTheme(holder.configuration.brand) {
        Surface(modifier.fillMaxSize(), color = Tokens.background) {
            BoxWithConstraints(Modifier.safeDrawingPadding().imePadding().onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Escape && state.session != null) {
                    controller.navigate(Route.HOME)
                    true
                } else false
            }) {
                val fontScale = LocalDensity.current.fontScale
                val rail = maxWidth >= 600.dp && maxWidth.value / fontScale >= 440
                val split = maxWidth >= 1000.dp && maxWidth.value / fontScale >= 800
                Column(Modifier.fillMaxSize()) {
                    EnvironmentHeader(runtime, holder, state)
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.error?.let { message ->
                        Surface(color = Tokens.warning, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                Text(message, color = Tokens.warningText, style = MaterialTheme.typography.bodySmall)
                                if (state.session != null) TextButton(controller::refresh, enabled = !state.busy) { Text("Tentar novamente") }
                            }
                        }
                    }
                    if (state.stale) Text("Dados desatualizados. Códigos ocultos até nova verificação.",
                        Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall,
                        color = Tokens.warningText)
                    if (state.session == null) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                            if (state.route == Route.LEGAL) ScrollPage {
                                LegalScreen(controller, runtime.platform)
                                TextButton({ controller.navigate(Route.HOME) }) { Text("Voltar ao login") }
                            } else LoginScreen(controller, state) { controller.navigate(Route.LEGAL) }
                        }
                    } else {
                        Row(Modifier.weight(1f).fillMaxWidth()) {
