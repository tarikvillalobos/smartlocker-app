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
