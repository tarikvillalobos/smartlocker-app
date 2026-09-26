package app.smartlocker.api.data

import app.smartlocker.parcels.domain.CredentialStatus
import app.smartlocker.parcels.domain.Parcel
import app.smartlocker.parcels.domain.ParcelPage
import app.smartlocker.parcels.domain.PickupCredential
import app.smartlocker.parcels.domain.Statistics

fun ApiParcel.toDomain(): Parcel {
    val deposited = apiInstant(depositedAt)
    val notified = notifiedAt?.let(::apiInstant)
    val manual = manualAt?.let(::apiInstant)
    val collected = collectedAt?.let(::apiInstant)
    val expiration = apiInstant(deadline)
    val credential = when (credentialStatus) {
        "active" -> CredentialStatus.ACTIVE
        "expired" -> CredentialStatus.EXPIRED
        "revoked" -> CredentialStatus.REVOKED
        "consumed" -> CredentialStatus.CONSUMED
        "unverified" -> CredentialStatus.UNVERIFIED
        else -> invalidApiResponse()
    }
    when (status) {
        "waiting" -> apiRequire(manual == null && collected == null)
        "manual" -> apiRequire(manual != null && collected == null && credential == CredentialStatus.REVOKED)
        "collected" -> apiRequire(collected != null && credential == CredentialStatus.CONSUMED)
        else -> invalidApiResponse()
    }
    apiRequire(expiration >= deposited && listOfNotNull(notified, manual, collected).all { it >= deposited })
    apiRequire(version >= 1)
    val undo = actions.undoUntil?.let(::apiInstant)
    apiRequire(!actions.canUndoManual || (status == "manual" && undo != null))
    timeline.forEach { event ->
        apiRequire(event.type in setOf("deposited", "notification_available", "manual_pickup", "manual_pickup_reverted", "physical_pickup"))
        apiRequire(apiInstant(event.at) >= deposited)
    }
    apiId(locker.id)
    tracking?.let { apiText(it, 200, true) }
    size?.let { apiText(it, 50, true) }
    return Parcel(id = apiId(id), recipientId = apiId(recipientId), locationId = apiId(membershipId),
