package app.smartlocker.api.data

import app.smartlocker.shared.domain.*
import kotlinx.serialization.json.*
import kotlin.time.Instant

/** Only documented error codes select local text; never display a remote error body. */
internal fun apiFailure(status: Int, body: String): AppFailure {
    val problem = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull()
    val code = problem?.takeIf { it["status"]?.jsonPrimitive?.intOrNull == status }
        ?.get("code")?.jsonPrimitive?.contentOrNull
    val retryAt = runCatching { Instant.parse(problem?.get("retryAt")?.jsonPrimitive?.content.orEmpty()).toEpochMilliseconds() }.getOrNull()
    val (kind, message) = when {
        status == 401 -> FailureKind.EXPIRED_SESSION to "Sua sessão expirou. Entre novamente."
        code == "INVALID_OTP" -> FailureKind.INVALID_CODE to "Código incorreto. Confira e tente novamente."
        code == "OTP_EXPIRED" -> FailureKind.EXPIRED_CODE to "O código expirou. Solicite outro."
        code == "OTP_ATTEMPTS_EXCEEDED" -> FailureKind.ATTEMPTS_EXCEEDED to "Limite de tentativas atingido. Solicite outro código após o intervalo."
        code == "RESEND_TOO_EARLY" -> FailureKind.VALIDATION to "Aguarde o intervalo antes de reenviar o código."
        code in setOf("CREDENTIAL_EXPIRED", "CREDENTIAL_REVOKED", "CREDENTIAL_CONSUMED") ->
            FailureKind.EXPIRED_CODE to "Este código não está mais disponível. Atualize a encomenda."
        code == "LOCKER_UNAVAILABLE" -> FailureKind.UNAVAILABLE to "O armário está indisponível. Tente novamente mais tarde."
        code == "CURSOR_EXPIRED" -> FailureKind.CONFLICT to "A consulta expirou. Atualize a lista para continuar."
        code == "FEATURE_UNAVAILABLE" -> FailureKind.DENIED to "Este recurso não está disponível neste local."
        status in setOf(403, 404) -> FailureKind.DENIED to "Recurso indisponível ou sem autorização de acesso."
        status in setOf(409, 412) -> FailureKind.CONFLICT to "Os dados foram alterados. Atualize antes de tentar novamente."
        status == 429 -> FailureKind.UNAVAILABLE to "Muitas solicitações. Aguarde antes de tentar novamente."
        status in 400..499 -> FailureKind.VALIDATION to "A solicitação não foi aceita. Confira os dados informados."
        else -> FailureKind.UNAVAILABLE to "Serviço indisponível. Tente novamente mais tarde."
    }
    return AppFailure(kind, message, retryAt)
}
