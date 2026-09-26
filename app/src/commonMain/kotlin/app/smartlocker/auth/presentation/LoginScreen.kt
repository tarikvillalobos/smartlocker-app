package app.smartlocker.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.smartlocker.auth.domain.*
import app.smartlocker.config.*
import app.smartlocker.design.*
import app.smartlocker.shared.presentation.*

@Composable
fun LoginScreen(controller: AppController, state: AppState, legal: () -> Unit) {
    val brand = controller.configuration.brand
    val demo = controller.configuration.environment == Environment.DEMO
    var contact by rememberSaveable { mutableStateOf("") }
    var cpf by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.widthIn(max = Tokens.maxForm).fillMaxWidth().verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().background(Color(brand.dark), RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .padding(horizontal = 24.dp, vertical = 36.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Surface(shape = Tokens.control, color = Color.White.copy(alpha = .08f), contentColor = Tokens.soft) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppIcon(Symbol.PARCEL)
                    Text(brand.name, style = MaterialTheme.typography.labelLarge)
                }
            }
            Text(brand.headline, style = MaterialTheme.typography.displaySmall, color = Color.White)
