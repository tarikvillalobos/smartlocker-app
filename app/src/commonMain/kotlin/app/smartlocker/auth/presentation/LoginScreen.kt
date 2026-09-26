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
                    BrandSymbol(brand)
                    Text(brand.name, style = MaterialTheme.typography.labelLarge)
                }
            }
            Text(brand.headline, style = MaterialTheme.typography.displaySmall, color = Color.White)
            Text(brand.introduction, color = Tokens.soft, style = MaterialTheme.typography.bodyLarge)
        }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (state.challenge == null) {
                OutlinedTextField(contact, { contact = it }, label = { Text(if (email) "E-mail" else "Celular") },
                    modifier = Modifier.fillMaxWidth().keepAboveKeyboard(), singleLine = true, shape = Tokens.control,
                    placeholder = { Text(if (email) "voce@exemplo.com" else "(11) 90000-0000") },
                    keyboardOptions = KeyboardOptions(keyboardType = if (email) KeyboardType.Email else KeyboardType.Phone))
                OutlinedTextField(cpf, { cpf = it.take(14) }, label = { Text("CPF") },
                    modifier = Modifier.fillMaxWidth().keepAboveKeyboard(), singleLine = true, shape = Tokens.control,
                    placeholder = { Text("000.000.000-00") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                PrimaryButton(if (email) "Receber código por e-mail" else "Receber código por SMS", !state.busy) {
                    controller.login(LoginRequest(contact.trim(), cpf, if (email) LoginChannel.EMAIL else LoginChannel.SMS))
                }
                TextButton({ email = !email; contact = "" }, Modifier.fillMaxWidth(), enabled = !state.busy) {
                    Text(if (email) "Entrar com celular" else "Entrar com e-mail")
                }
                if (demo) Panel {
                    Text("Experimente com dados fictícios", style = MaterialTheme.typography.labelLarge)
                    Text("Nenhum SMS ou e-mail será enviado.", color = Tokens.secondary)
                    TextButton({ contact = if (email) "ana@example.test" else "11987654321"; cpf = "52998224725" }) {
                        Text("Preencher dados de demonstração")
                    }
                }
            } else {
                Text("Confira seu código", style = MaterialTheme.typography.headlineSmall)
                Text("Enviado para $contact. Válido por até 5 minutos.", color = Tokens.secondary)
                if (demo) Text("Código demonstrativo: 123456", color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) },
                    label = { Text("Código de 6 dígitos") }, singleLine = true, shape = Tokens.control,
                PrimaryButton("Confirmar código", !state.busy && code.length == 6) { controller.verify(code) }
                val seconds = ((state.challenge.resendAt - state.now + 999) / 1000).coerceAtLeast(0)
                TextButton({ code = ""; controller.resend() }, Modifier.fillMaxWidth(), enabled = !state.busy && seconds == 0L) {
                    Text(if (seconds > 0) "Reenviar em ${seconds}s" else "Reenviar código")
                }
                TextButton({ code = ""; controller.correctContact() }, Modifier.fillMaxWidth()) { Text("Corrigir contato") }
            }
            TextButton(legal, Modifier.fillMaxWidth()) { Text("Termos de uso e privacidade") }
        }
    }
}
