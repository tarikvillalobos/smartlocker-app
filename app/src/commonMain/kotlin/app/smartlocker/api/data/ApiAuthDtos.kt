package app.smartlocker.api.data

import app.smartlocker.auth.domain.Challenge
import app.smartlocker.auth.domain.LoginChannel
import app.smartlocker.auth.domain.Session
import kotlinx.serialization.Serializable

@Serializable
data class ApiChallenge(
    val id: String,
    val expiresAt: String,
    val resendAt: String,
    val channel: String,
    val maskedDestination: String,
    val codeLength: Int,
    val purpose: String,
) {
    fun toDomain(expectedPurpose: String? = null): Challenge {
        apiRequire(codeLength == 6 && purpose in setOf("login", "contact_change", "password_recovery"))
        apiRequire(expectedPurpose == null || purpose == expectedPurpose)
        val mappedChannel = when (channel) {
            "sms" -> LoginChannel.SMS
            "email" -> LoginChannel.EMAIL
            else -> invalidApiResponse()
        }
        return Challenge(apiId(id), apiInstant(expiresAt), apiInstant(resendAt),
            apiText(maskedDestination, 254), mappedChannel)
    }
}

@Serializable
data class ApiSessionTokens(
    val tokenType: String,
    val accessToken: String,
    val accessExpiresAt: String,
    val refreshToken: String,
    val refreshExpiresAt: String,
    val userId: String,
    val brandId: String,
    val sessionId: String,
    val permissions: List<String>,
) {
    fun toDomain(): Session {
        apiRequire(tokenType == "Bearer")
        apiRequire(accessToken.length in 1..8192 && accessToken.none { it.isWhitespace() || it.code < 32 || it.code == 127 })
        apiRequire(refreshToken.length in 1..8192 && refreshToken.isNotBlank())
        apiId(brandId)
        apiId(sessionId)
        apiInstant(accessExpiresAt)
        apiRequire(permissions.all { it.isNotBlank() } && permissions.size == permissions.toSet().size)
        return Session(accessToken, apiId(userId), apiInstant(refreshExpiresAt))
    }

    override fun toString() = "ApiSessionTokens(redacted)"
}

@Serializable
data class ApiLoginRequest(val contact: String, val cpf: String, val channel: String) {
    override fun toString() = "ApiLoginRequest(redacted)"
}

@Serializable
data class ApiPasswordLoginRequest(val identifier: String, val password: String) {
    override fun toString() = "ApiPasswordLoginRequest(redacted)"
}

@Serializable
data class ApiPasswordRecoveryRequest(val identifier: String, val channel: String) {
    override fun toString() = "ApiPasswordRecoveryRequest(redacted)"
}

@Serializable
data class ApiPasswordRecoveryVerify(val code: String, val newPassword: String) {
    override fun toString() = "ApiPasswordRecoveryVerify(redacted)"
}

@Serializable
data class ApiOtpVerification(val code: String) {
    override fun toString() = "ApiOtpVerification(redacted)"
}

@Serializable
data class ApiRefreshRequest(val refreshToken: String) {
    override fun toString() = "ApiRefreshRequest(redacted)"
}

@Serializable
data class ApiContactChangeRequest(val contact: String, val channel: String) {
    override fun toString() = "ApiContactChangeRequest(redacted)"
}
