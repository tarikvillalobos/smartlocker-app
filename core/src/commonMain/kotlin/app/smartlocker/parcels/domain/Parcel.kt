package app.smartlocker.parcels.domain

enum class ParcelStatus { WAITING, MANUAL, COLLECTED }
enum class CredentialStatus { ACTIVE, EXPIRED, REVOKED, CONSUMED, UNVERIFIED }
enum class ParcelFilter { ALL, WAITING, COLLECTED }
data class Parcel(
    val id: String,
    val recipientId: String,
    val locationId: String,
    val carrier: String,
    val tracking: String?,
    val locker: String,
    val address: String,
    val compartment: String,
    val size: String?,
    val depositedAt: Long,
    val notifiedAt: Long?,
    val deadline: Long,
    val manualAt: Long? = null,
    val collectedAt: Long? = null,
    val credentialStatus: CredentialStatus = CredentialStatus.ACTIVE,
    val lockerAvailable: Boolean = true,
    val canMarkManually: Boolean = true,
    val canUndo: Boolean = false,
) {
    val status: ParcelStatus get() = when {
        collectedAt != null -> ParcelStatus.COLLECTED
        manualAt != null -> ParcelStatus.MANUAL
        else -> ParcelStatus.WAITING
    }
}

data class PickupCredential(
    val parcelId: String,
    val code: String,
    val payload: String,
    val expiresAt: Long,
    val verifiedAt: Long,
    val status: CredentialStatus,
) {
