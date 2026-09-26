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
