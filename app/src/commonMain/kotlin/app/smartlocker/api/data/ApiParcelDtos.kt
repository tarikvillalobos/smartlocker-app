package app.smartlocker.api.data

import kotlinx.serialization.Serializable

@Serializable
data class ApiLocker(val id: String, val name: String, val address: String, val available: Boolean)

@Serializable
data class ApiParcelActions(
    val canMarkManually: Boolean,
    val canUndoManual: Boolean,
    val undoUntil: String?,
    val canReportIssue: Boolean,
)

@Serializable
data class ApiTimelineEvent(val type: String, val at: String)

@Serializable
data class ApiParcel(
    val id: String,
    val recipientId: String,
    val membershipId: String,
    val carrier: String,
    val tracking: String?,
    val status: String,
    val locker: ApiLocker,
    val compartment: String,
    val size: String?,
    val depositedAt: String,
    val notifiedAt: String?,
    val deadline: String,
    val manualAt: String?,
    val collectedAt: String?,
    val credentialStatus: String,
    val actions: ApiParcelActions,
    val timeline: List<ApiTimelineEvent>,
    val version: Long,
)

@Serializable
data class ApiPageInfo(val nextCursor: String?, val snapshotAt: String, val snapshotExpiresAt: String) {
    fun validatedCursor(): String? {
        apiRequire(nextCursor == null || nextCursor.length in 1..2048)
        apiRequire(apiInstant(snapshotAt) <= apiInstant(snapshotExpiresAt))
        return nextCursor
    }
}

@Serializable
data class ApiParcelPage(val items: List<ApiParcel>, val pageInfo: ApiPageInfo)

@Serializable
data class ApiParcelMetrics(
    val since: String,
    val until: String,
    val generatedAt: String,
    val complete: Boolean,
    val totalReceived: Int?,
    val physicalPickupCount: Int?,
