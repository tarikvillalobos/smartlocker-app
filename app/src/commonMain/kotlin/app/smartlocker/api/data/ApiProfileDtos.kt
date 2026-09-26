package app.smartlocker.api.data

import app.smartlocker.config.Brand
import app.smartlocker.config.Features
import app.smartlocker.profile.domain.CommunicationPreferences
import app.smartlocker.profile.domain.Membership
import app.smartlocker.profile.domain.Profile
import app.smartlocker.profile.domain.Recipient
import io.ktor.http.Url
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable

@Serializable
data class ApiChannelCapability(val available: Boolean, val reason: String? = null)

@Serializable
data class ApiChannels(
    val inApp: ApiChannelCapability,
    val sms: ApiChannelCapability,
    val email: ApiChannelCapability,
    val whatsapp: ApiChannelCapability,
    val push: ApiChannelCapability,
) {
    fun availableChannels(): Set<String> = listOf(
        "app" to inApp, "sms" to sms, "email" to email, "whatsapp" to whatsapp, "push" to push,
    ).filter { it.second.available }.map { it.first }.toSet()
}

@Serializable
data class ApiFeatures(
    val manualPickup: Boolean,
    val undoManualPickup: Boolean,
    val contactEditing: Boolean,
    val recipients: Boolean,
    val supportIssues: Boolean,
    val pushRegistration: Boolean,
) {
    fun toDomain() = Features(residents = recipients, issues = supportIssues,
        manualPickup = manualPickup, contactEditing = contactEditing)
}

@Serializable
data class ApiCapabilities(val features: ApiFeatures, val channels: ApiChannels)

@Serializable
data class ApiBrandConfiguration(
    val brandId: String,
    val appName: String,
    val capabilities: ApiCapabilities,
    val supportEmail: String?,
    val termsUrl: String?,
    val privacyUrl: String?,
) {
    fun validate(expectedBrandId: String) {
        apiRequire(apiId(brandId) == expectedBrandId)
        apiText(appName, 100)
        supportEmail?.let(::validateApiEmail)
        listOfNotNull(termsUrl, privacyUrl).forEach { value ->
            val parsed = try { Url(value) } catch (_: IllegalArgumentException) { invalidApiResponse() }
            apiRequire(parsed.protocol.name == "https" && parsed.host.isNotBlank() && value.none(Char::isWhitespace))
            apiRequire(!value.substringAfter("://").substringBefore('/').contains('@'))
        }
    }

    fun toDomain(base: Brand): Brand {
        validate(base.id)
        val supplied = capabilities.features
        val allowed = Features(residents = base.features.residents && supplied.recipients,
            issues = base.features.issues && supplied.supportIssues,
            manualPickup = base.features.manualPickup && supplied.manualPickup,
            contactEditing = base.features.contactEditing && supplied.contactEditing)
        return base.copy(name = appName, supportEmail = supportEmail, termsUrl = termsUrl,
            privacyUrl = privacyUrl, features = allowed,
            channels = base.channels intersect capabilities.channels.availableChannels())
    }
}

@Serializable
data class ApiMembership(
    val id: String,
    val locationId: String,
    val locationName: String,
    val unitId: String?,
    val unitLabel: String?,
    val timeZone: String,
    val capabilities: ApiCapabilities,
) {
    fun toDomain(): Membership {
        apiId(locationId)
        apiRequire((unitId == null) == (unitLabel == null))
        unitId?.let(::apiId)
        apiText(timeZone, 100)
        try { TimeZone.of(timeZone) } catch (_: IllegalArgumentException) { invalidApiResponse() }
        return Membership(apiId(id), apiText(locationName, 200, true), apiText(unitLabel.orEmpty(), 200, true),
            timeZone = timeZone, features = capabilities.features.toDomain(), channels = capabilities.channels.availableChannels())
    }
}

@Serializable
data class ApiMembershipList(val items: List<ApiMembership>) {
