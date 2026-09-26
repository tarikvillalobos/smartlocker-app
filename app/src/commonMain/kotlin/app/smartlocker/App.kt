package app.smartlocker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.*
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
    MembershipTimeZone(state.membership?.timeZone) {
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
                            if (rail) AppNavigation(state, controller, true)
                            if (split && state.route in setOf(Route.HISTORY, Route.DETAIL)) {
                                Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Box(Modifier.weight(1f)) { ScrollPage { HistoryScreen(state, controller) } }
                                    Box(Modifier.weight(1f)) {
                                        ScrollPage { DetailScreen(state, controller, runtime.platform) }
                                    }
                                }
                            } else if (split && state.route == Route.HOME) {
                                Row(Modifier.widthIn(max = Tokens.maxContent).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Box(Modifier.weight(1f)) {
                                        ScrollPage { HomeScreen(state, controller, runtime.platform, showRecent = false) }
                                    }
                                    Box(Modifier.weight(1f)) {
                                        ScrollPage {
                                            PageTitle("Suas entregas")
                                            Panel {
                                                Metadata("Local selecionado", state.membership?.location.orEmpty())
                                                MenuRow("Trocar local", Symbol.LOCATION) { controller.navigate(Route.LOCATIONS) }
                                            }
                                            RecentParcels(state, controller)
                                        }
                                    }
                                }
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                    ScrollPage(maxWidth = if (state.route == Route.HOME && split) 720 else 600) {
                                        when (state.route) {
                                            Route.HOME -> HomeScreen(state, controller, runtime.platform)
                                            Route.HISTORY -> HistoryScreen(state, controller)
                                            Route.DETAIL -> DetailScreen(state, controller, runtime.platform)
                                            Route.PROFILE -> ProfileScreen(state, controller, runtime.platform)
                                            else -> AuxiliaryScreen(state, controller, runtime.platform)
                                        }
                                    }
                                }
                            }
                        }
                        if (!rail) AppNavigation(state, controller, false)
                    }
                    state.feedback?.let { message ->
                        Snackbar(action = { TextButton({ controller.feedback(null) }) { Text("Fechar") } }, modifier = Modifier.padding(8.dp)) {
                            Text(message)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScrollPage(maxWidth: Int = 600, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.widthIn(max = maxWidth.dp).fillMaxWidth().fillMaxHeight()
        .verticalScroll(rememberScrollState()).padding(Tokens.gutter),
        verticalArrangement = Arrangement.spacedBy(Tokens.gap), content = content)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnvironmentHeader(runtime: AppRuntime, holder: RuntimeState, state: AppState) {
    val demo = holder.configuration.environment == Environment.DEMO
    Surface(color = if (demo) Tokens.success else Tokens.background) {
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.Center) {
                Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodySmall,
                color = if (demo) Tokens.successText else Tokens.secondary)
            if (state.session == null) {
                var menu by remember { mutableStateOf(false) }
                Box {
                    TextButton({ menu = true }) { Text(holder.controller.brand.name, style = MaterialTheme.typography.labelSmall) }
                    DropdownMenu(menu, { menu = false }) {
                        Brands.all.forEach { brand -> DropdownMenuItem(text = { Text(brand.name) },
                            onClick = { menu = false; runtime.configure(brand = brand) }) }
                    }
                }
                TextButton({ runtime.configure(environment = if (demo) Environment.PRODUCTION else Environment.DEMO) }) {
                    Text(if (demo) "Usar API externa" else "Experimentar demonstração", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                IconButton(holder.controller::refresh, enabled = !state.busy) { AppIcon(Symbol.REFRESH, "Atualizar encomendas") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppNavigation(state: AppState, controller: AppController, rail: Boolean) {
    val items = listOf(Triple(Route.HOME, "Início", Symbol.HOME), Triple(Route.HISTORY, "Histórico", Symbol.HISTORY),
        Triple(Route.PROFILE, "Perfil", Symbol.USER))
    if (rail) NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
        Spacer(Modifier.height(16.dp))
        items.forEach { (route, title, symbol) ->
            NavigationRailItem(state.route == route || route == Route.HISTORY && state.route == Route.DETAIL,
                { controller.navigate(route) }, icon = { AppIcon(symbol) }, label = { Text(title) })
        }
    } else if (LocalDensity.current.fontScale >= 1.5f) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            FlowRow(Modifier.fillMaxWidth().padding(4.dp).selectableGroup(),
                horizontalArrangement = Arrangement.Center) {
                items.forEach { (route, title, symbol) ->
                    val active = state.route == route || route == Route.HISTORY && state.route == Route.DETAIL
                    TextButton({ controller.navigate(route) }, Modifier.heightIn(min = Tokens.touch).semantics {
                        selected = active
                        role = Role.Tab
                    }, colors = ButtonDefaults.textButtonColors(
                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                        AppIcon(symbol)
                        Spacer(Modifier.width(8.dp))
                        Text(title, softWrap = false, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    } else NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { (route, title, symbol) ->
            NavigationBarItem(state.route == route || route == Route.HISTORY && state.route == Route.DETAIL,
                { controller.navigate(route) }, icon = { AppIcon(symbol) }, label = { Text(title) })
        }
    }
}
