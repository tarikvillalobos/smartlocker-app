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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.smartlocker.auth.domain.*
import app.smartlocker.config.*
import app.smartlocker.design.*
import app.smartlocker.shared.presentation.*

@Composable
fun LoginScreen(controller: AppController, state: AppState, legal: () -> Unit) {
    val brand = controller.brand
    val demo = controller.configuration.environment == Environment.DEMO
    val smsEnabled = "sms" in brand.loginChannels
    val emailEnabled = "email" in brand.loginChannels
    val otpEnabled = "otp" in brand.authMethods && (smsEnabled || emailEnabled)
    val passwordEnabled = !demo && "password" in brand.authMethods
    val invitationEnabled = !demo && "invitation" in brand.authMethods
    var contact by rememberSaveable { mutableStateOf("") }
    var cpf by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf(false) }
    val useEmail = emailEnabled && (email || !smsEnabled)
    var passwordMode by rememberSaveable { mutableStateOf(false) }
    var recoveryMode by rememberSaveable { mutableStateOf(false) }
    var invitationMode by rememberSaveable { mutableStateOf(false) }
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val usePassword = passwordEnabled && (passwordMode || !otpEnabled)
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
                if (invitationMode) {
                    InvitationForm(controller, state, brand, legal) {
                        controller.clearInvitationPreview(); invitationMode = false
                    }
                } else if (recoveryMode) {
                    RecoveryForm(controller, state, smsEnabled, emailEnabled) {
                        controller.cancelPasswordRecovery()
                        recoveryMode = false
                    }
                } else if (usePassword) {
                    OutlinedTextField(identifier, { identifier = it.take(254) }, label = { Text("E-mail, celular ou CPF") },
                        modifier = Modifier.fillMaxWidth().keepAboveKeyboard(), singleLine = true, shape = Tokens.control)
                    OutlinedTextField(password, { password = it.take(8192) }, label = { Text("Senha") },
                        modifier = Modifier.fillMaxWidth().keepAboveKeyboard(), singleLine = true, shape = Tokens.control,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    PrimaryButton("Entrar", !state.busy && identifier.trim().length >= 3 && password.isNotEmpty()) {
                        controller.loginWithPassword(identifier, password)
                        password = ""
                    }
                    if (otpEnabled) TextButton({ passwordMode = false }, Modifier.fillMaxWidth()) { Text("Entrar com código") }
                    if (smsEnabled || emailEnabled) TextButton({ recoveryMode = true }, Modifier.fillMaxWidth()) {
                        Text("Esqueci minha senha")
                    }
                } else if (otpEnabled) {
                OutlinedTextField(contact, { contact = it }, label = { Text(if (useEmail) "E-mail" else "Celular") },
                    modifier = Modifier.fillMaxWidth().keepAboveKeyboard(), singleLine = true, shape = Tokens.control,
                    placeholder = { Text(if (useEmail) "voce@exemplo.com" else "(11) 90000-0000") },
                    keyboardOptions = KeyboardOptions(keyboardType = if (useEmail) KeyboardType.Email else KeyboardType.Phone))
                OutlinedTextField(cpf, { cpf = it.take(14) }, label = { Text("CPF") },
                    modifier = Modifier.fillMaxWidth().keepAboveKeyboard(), singleLine = true, shape = Tokens.control,
                    placeholder = { Text("000.000.000-00") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                PrimaryButton(if (useEmail) "Receber código por e-mail" else "Receber código por SMS", !state.busy) {
                    controller.login(LoginRequest(contact.trim(), cpf, if (useEmail) LoginChannel.EMAIL else LoginChannel.SMS))
                }
                if (smsEnabled && emailEnabled) TextButton({ email = !email; contact = "" },
                    Modifier.fillMaxWidth(), enabled = !state.busy) {
                    Text(if (useEmail) "Entrar com celular" else "Entrar com e-mail")
                }
                if (demo) Panel {
                    Text("Experimente com dados fictícios", style = MaterialTheme.typography.labelLarge)
                    Text("Nenhum SMS ou e-mail será enviado.", color = Tokens.secondary)
                    TextButton({ contact = if (useEmail) "ana@example.test" else "11987654321"; cpf = "52998224725" }) {
                        Text("Preencher dados de demonstração")
                    }
                }
                if (passwordEnabled) TextButton({ passwordMode = true }, Modifier.fillMaxWidth()) { Text("Entrar com senha") }
                } else {
                    Text("Nenhum método de login está disponível para esta marca.", color = Tokens.secondary)
                }
                if (!invitationMode && !recoveryMode && invitationEnabled) TextButton({ invitationMode = true },
                    Modifier.fillMaxWidth()) { Text("Tenho um convite de primeiro acesso") }
            } else {
                Text("Confira seu código", style = MaterialTheme.typography.headlineSmall)
                Text("Enviado para ${state.challenge.maskedDestination ?: contact}.", color = Tokens.secondary)
                val remaining = ((state.challenge.expiresAt - state.now + 999) / 1000).coerceAtLeast(0)
                Text(if (remaining == 0L) "Código expirado. Solicite um novo envio."
                    else "Código válido por ${remaining / 60} min ${remaining % 60} s.", color = Tokens.secondary)
                if (demo) Text("Código demonstrativo: 123456", color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) },
                    label = { Text("Código de 6 dígitos") }, singleLine = true, shape = Tokens.control,
                    modifier = Modifier.fillMaxWidth().keepAboveKeyboard(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                PrimaryButton("Confirmar código", !state.busy && code.length == 6 && state.now < state.challenge.expiresAt) { controller.verify(code) }
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
@Composable
private fun RecoveryForm(controller: AppController, state: AppState, smsEnabled: Boolean,
    emailEnabled: Boolean, onBack: () -> Unit) {
    var identifier by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf(!smsEnabled) }
    var code by rememberSaveable { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    val useEmail = emailEnabled && (email || !smsEnabled)
    val challenge = state.recoveryChallenge
    Text("Recuperar senha", style = MaterialTheme.typography.headlineSmall)
    if (challenge == null) {
        OutlinedTextField(identifier, { identifier = it.take(254) }, label = { Text("E-mail, celular ou CPF") },
            modifier = Modifier.fillMaxWidth().keepAboveKeyboard(), singleLine = true, shape = Tokens.control)
        if (smsEnabled && emailEnabled) TextButton({ email = !email }) {
            Text(if (useEmail) "Receber por SMS" else "Receber por e-mail")
        }
        PrimaryButton("Receber código", !state.busy && identifier.trim().length >= 3) {
            controller.requestPasswordRecovery(identifier, if (useEmail) LoginChannel.EMAIL else LoginChannel.SMS)
        }
    } else {
        Text("Código enviado para ${challenge.maskedDestination.orEmpty()}.", color = Tokens.secondary)
        OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, label = { Text("Código de recuperação") },
            modifier = Modifier.fillMaxWidth().keepAboveKeyboard(), singleLine = true, shape = Tokens.control,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
        OutlinedTextField(newPassword, { newPassword = it.take(8192) }, label = { Text("Nova senha") },
            modifier = Modifier.fillMaxWidth().keepAboveKeyboard(), singleLine = true, shape = Tokens.control,
            visualTransformation = PasswordVisualTransformation())
        PrimaryButton("Redefinir senha", !state.busy && code.length == 6 && newPassword.isNotEmpty()
            && state.now < challenge.expiresAt) { controller.verifyPasswordRecovery(code, newPassword); newPassword = "" }
        TextButton({ code = ""; newPassword = ""; controller.restartPasswordRecovery() }, enabled = !state.busy) {
            Text("Solicitar novo código")
        }
    }
    TextButton(onBack, Modifier.fillMaxWidth()) { Text("Voltar ao login") }
}

@Composable
private fun InvitationForm(controller: AppController, state: AppState, brand: Brand,
    legal: () -> Unit, onBack: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    val preview = state.invitationPreview
    Text("Primeiro acesso", style = MaterialTheme.typography.headlineSmall)
    if (preview == null) {
        OutlinedTextField(code, { code = it.take(32) }, Modifier.fillMaxWidth().keepAboveKeyboard(),
            label = { Text("Código do convite") }, singleLine = true)
        PrimaryButton("Conferir convite", !state.busy && code.length in 6..32) { controller.previewInvitation(code) }
    } else {
        Text(preview.condominiumName, style = MaterialTheme.typography.titleMedium)
        preview.unitLabel?.let { Text(it, color = Tokens.secondary) }
    }
    TextButton(onBack, Modifier.fillMaxWidth()) { Text("Voltar ao login") }
}
