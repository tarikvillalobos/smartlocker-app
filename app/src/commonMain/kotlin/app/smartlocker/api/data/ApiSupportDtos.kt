package app.smartlocker.api.data

import app.smartlocker.profile.domain.DeliveryNotice
import app.smartlocker.profile.domain.IssuePage
import app.smartlocker.profile.domain.NoticePage
import app.smartlocker.profile.domain.SupportIssue
import kotlinx.serialization.Serializable

@Serializable
data class ApiDeliveryNotice(
    val id: String,
    val membershipId: String,
    val parcelId: String,
    val title: String,
    val createdAt: String,
    val readAt: String?,
) {
    fun toDomain(): DeliveryNotice {
        apiId(membershipId)
        val created = apiInstant(createdAt)
