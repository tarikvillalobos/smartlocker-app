package app.smartlocker.demo.data

import app.smartlocker.auth.domain.*
import app.smartlocker.shared.domain.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class StoredSession(val token: String, val userId: String, val expiresAt: Long)

class DemoAuth(private val secure: SecureStorage, brandId: String, private val clock: AppClock) {
    private val key = "demo.$brandId.session"
    private var challenge: Challenge? = null
    private var attempts = 0
    private var serial = 0
    var session: Session? = null
        private set

    fun request(request: LoginRequest): Challenge {
        val valid = when (request.channel) {
            LoginChannel.SMS -> InputValidation.phone(request.contact)
            LoginChannel.EMAIL -> InputValidation.email(request.contact)
        }
        if (!valid || !InputValidation.cpf(request.cpf)) {
            throw AppFailure(FailureKind.VALIDATION, "Confira o contato e o CPF informado.")
        }
        if (challenge?.resendAt?.let { clock.now() < it } == true) {
            throw AppFailure(FailureKind.VALIDATION, "Aguarde o intervalo para reenviar o código.")
        }
        attempts = 0
        return Challenge("demo-${++serial}", clock.now() + 300_000, clock.now() + 30_000)
            .also { challenge = it }
    }

    fun verify(id: String, code: String) {
        val current = challenge
        if (current == null || current.id != id || clock.now() >= current.expiresAt) {
            throw AppFailure(FailureKind.EXPIRED_CODE, "O código expirou. Solicite outro.")
        }
        if (attempts >= 5) throw AppFailure(FailureKind.ATTEMPTS_EXCEEDED, "Limite de tentativas atingido.")
