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
}

@Serializable
data class ApiPushRegistration(val installationId: String, val registeredAt: String) {
    fun validate() {
        apiRequire(Regex("^[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$").matches(installationId))
        apiInstant(registeredAt)
    }
}

@Serializable
data class ApiPushData(
    val type: String,
    val brandId: String,
    val membershipId: String,
    val parcelId: String,
    val noticeId: String,
) {
    fun validate() {
        apiRequire(type == "parcel_update")
        listOf(brandId, membershipId, parcelId, noticeId).forEach(::apiId)
    }
}

@Serializable
data class ApiFieldError(val field: String, val code: String)

/** The client must select safe local copy by code, never display server detail. */
@Serializable
data class ApiProblem(
    val type: String,
    val title: String,
    val status: Int,
    val code: String,
    val requestId: String,
    val detail: String? = null,
    val instance: String? = null,
    val fieldErrors: List<ApiFieldError> = emptyList(),
    val retryAt: String? = null,
)
