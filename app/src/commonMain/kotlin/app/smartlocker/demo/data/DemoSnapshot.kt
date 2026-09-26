package app.smartlocker.demo.data

import app.smartlocker.parcels.domain.*
import app.smartlocker.profile.domain.*
import app.smartlocker.shared.domain.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ParcelRecord(
    val id: String, val recipient: String, val location: String,
    val carrier: String, val tracking: String?, val locker: String,
    val address: String, val compartment: String, val size: String?,
    val deposited: Long, val notified: Long?, val deadline: Long,
    val manual: Long? = null, val collected: Long? = null,
    val credential: String = "ACTIVE", val available: Boolean = true,
) {
    fun toDomain(now: Long) = Parcel(
        id, recipient, location, carrier, tracking, locker, address, compartment,
        size, deposited, notified, deadline, manual, collected,
        if (deadline <= now && credential == "ACTIVE") CredentialStatus.EXPIRED
        else CredentialStatus.valueOf(credential), available,
        canUndo = manual != null && collected == null && now - manual < 600_000,
    )
}

@Serializable
data class NoticeRecord(val id: String, val parcel: String, val title: String, val at: Long, val read: Boolean = false) {
    fun toDomain() = DeliveryNotice(id, parcel, title, at, read)
}
@Serializable
data class IssueRecord(val id: String, val parcel: String, val message: String, val at: Long) {
    fun toDomain() = SupportIssue(id, parcel, message, at)
}
@Serializable
data class DemoSnapshot(
    val parcels: List<ParcelRecord>,
    val notices: List<NoticeRecord>,
    val issues: List<IssueRecord> = emptyList(),
    val phone: String = "11987654321",
    val email: String = "ana@example.test",
    val inApp: Boolean = true,
    val sms: Boolean = true,
    val whatsapp: Boolean = false,
    val sequence: Int = 10,
)

class DemoDatabase(private val storage: LocalStorage, brandId: String, private val clock: AppClock) {
    private val key = "demo.$brandId.ana.snapshot.v1"
    var snapshot: DemoSnapshot = storage.read(key)?.let {
        runCatching { Json.decodeFromString<DemoSnapshot>(it) }.getOrNull()
    } ?: seed(clock.now())
        private set

    fun update(change: (DemoSnapshot) -> DemoSnapshot) {
        val next = change(snapshot)
        storage.write(key, Json.encodeToString(next))
        snapshot = next
    }

