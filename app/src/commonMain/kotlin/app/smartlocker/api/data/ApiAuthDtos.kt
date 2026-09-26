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
