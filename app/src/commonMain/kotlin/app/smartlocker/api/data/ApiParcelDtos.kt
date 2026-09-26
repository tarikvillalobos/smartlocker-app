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
