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
        readAt?.let { apiRequire(apiInstant(it) >= created) }
        return DeliveryNotice(apiId(id), apiId(parcelId), apiText(title, 200), created, readAt != null)
    }
}

@Serializable
data class ApiNoticePage(val items: List<ApiDeliveryNotice>, val pageInfo: ApiPageInfo, val unreadCount: Int) {
    fun toDomain(): NoticePage {
        apiUniqueIds(items.map { it.id })
        apiRequire(unreadCount >= 0 && unreadCount >= items.count { it.readAt == null })
        return NoticePage(items.map { it.toDomain() }, pageInfo.validatedCursor(), unreadCount)
    }
}

@Serializable
data class ApiIssueRequest(val parcelId: String, val message: String)

@Serializable
data class ApiSupportIssue(
    val id: String,
    val reference: String,
    val membershipId: String,
    val parcelId: String,
    val message: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val resolution: String?,
) {
    fun toDomain(): SupportIssue {
        apiId(membershipId)
        apiText(reference, 100, true)
        val label = when (status) {
            "received" -> "Recebida"
            "in_progress" -> "Em atendimento"
            "resolved" -> "Resolvida"
            "closed" -> "Encerrada"
            else -> invalidApiResponse()
        }
        val created = apiInstant(createdAt)
        apiRequire(apiInstant(updatedAt) >= created && message.length in 10..2000)
        resolution?.let { apiText(it, 2000, true) }
        return SupportIssue(apiId(id), apiId(parcelId), message, created, label,
            reference = reference, updatedAt = apiInstant(updatedAt), resolution = resolution)
    }
}

@Serializable
data class ApiIssuePage(val items: List<ApiSupportIssue>, val pageInfo: ApiPageInfo) {
    fun toDomain(): IssuePage {
        apiUniqueIds(items.map { it.id })
        return IssuePage(items.map { it.toDomain() }, pageInfo.validatedCursor())
    }
}
