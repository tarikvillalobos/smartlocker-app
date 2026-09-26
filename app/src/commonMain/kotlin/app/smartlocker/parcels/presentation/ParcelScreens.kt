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
