package app.smartlocker.profile.domain

data class CommunicationPreferences(
    val inApp: Boolean = true,
    val sms: Boolean = true,
    val whatsapp: Boolean = false,
)
data class Profile(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val memberships: List<Membership>,
    val preferences: CommunicationPreferences = CommunicationPreferences(),
)
data class Recipient(val id: String, val name: String, val relationship: String)
data class DeliveryNotice(
    val id: String,
    val parcelId: String,
    val title: String,
    val createdAt: Long,
    val read: Boolean = false,
)
data class SupportIssue(
    val id: String,
    val parcelId: String,
    val message: String,
    val createdAt: Long,
    val status: String = "Recebida",
)
