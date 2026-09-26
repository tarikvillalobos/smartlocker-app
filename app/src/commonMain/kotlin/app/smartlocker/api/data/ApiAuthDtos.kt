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
        apiRequire(codeLength == 6 && purpose in setOf("login", "contact_change"))
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
