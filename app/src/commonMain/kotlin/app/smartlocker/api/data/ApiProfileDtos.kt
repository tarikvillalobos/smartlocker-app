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
