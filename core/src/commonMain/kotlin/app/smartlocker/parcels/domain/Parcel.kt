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
    val version: String? = null,
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
    val revalidateAt: Long = verifiedAt + 60_000,
) {
    fun canDisplay(now: Long, fresh: Boolean): Boolean =
        fresh && status == CredentialStatus.ACTIVE && now < expiresAt &&
            now >= verifiedAt && now < revalidateAt
}

data class ParcelPage(val items: List<Parcel>, val nextCursor: String?)

fun List<Parcel>.filtered(filter: ParcelFilter): List<Parcel> = filter {
    when (filter) {
        ParcelFilter.ALL -> true
        ParcelFilter.WAITING -> it.status == ParcelStatus.WAITING
        ParcelFilter.COLLECTED -> it.status != ParcelStatus.WAITING
    }
}

fun calculateStatistics(completeData: List<Parcel>, since: Long, until: Long): Statistics {
    val period = completeData.filter { it.depositedAt in since..until }
    val durations = period.mapNotNull { parcel ->
        parcel.collectedAt?.takeIf { it in parcel.depositedAt..until }
            ?.minus(parcel.depositedAt)
    }
    return Statistics(period.size, durations.takeIf { it.isNotEmpty() }?.average()?.toLong(), since, until)
}
