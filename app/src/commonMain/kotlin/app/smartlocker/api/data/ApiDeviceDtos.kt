package app.smartlocker.api.data

import kotlinx.serialization.Serializable

@Serializable
data class ApiPushRegistrationRequest(
    val platform: String,
    val provider: String,
    val token: String,
    val permission: String,
    val appVersion: String,
) {
    fun validate() {
        apiRequire((platform == "android" && provider == "fcm") || (platform == "ios" && provider == "apns"))
        apiRequire(permission in setOf("authorized", "provisional"))
        apiRequire(token.length in 1..8192 && token.isNotBlank())
        apiText(appVersion, 50)
    }

    override fun toString() = "ApiPushRegistrationRequest(redacted)"
