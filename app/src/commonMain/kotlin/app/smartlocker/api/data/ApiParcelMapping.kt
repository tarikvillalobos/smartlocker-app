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
    apiRequire(recipientKind == null || recipientKind in setOf("membership", "node"))
    node?.let { apiId(it.id) }
    delegates.forEach { it.membershipId?.let(::apiId) }
    collectedBy?.membershipId?.let(::apiId)
    return Parcel(id = apiId(id), recipientId = apiId(recipientId), locationId = apiId(membershipId),
        carrier = apiText(carrier, 200), tracking = tracking, locker = apiText(locker.name, 200, true),
        address = apiText(locker.address, 500, true), compartment = apiText(compartment, 100), size = size,
        depositedAt = deposited, notifiedAt = notified, deadline = expiration, manualAt = manual,
        collectedAt = collected, credentialStatus = credential, lockerAvailable = locker.available,
        canMarkManually = actions.canMarkManually, canUndo = actions.canUndoManual, version = version.toString(),
        canReportIssue = actions.canReportIssue, recipientKind = recipientKind,
        nodeLabel = node?.label, delegateNames = delegates.map { it.name },
        delegateIds = delegates.map { it.membershipId },
        collectedByName = collectedBy?.name)
}

fun ApiParcelPage.toDomain(): ParcelPage {
    apiUniqueIds(items.map { it.id })
    return ParcelPage(items.map { it.toDomain() }, pageInfo.validatedCursor())
}

fun ApiParcelMetrics.toDomain(): Statistics {
    val start = apiInstant(since)
    val endExclusive = apiInstant(until)
    apiRequire(start < endExclusive && apiInstant(generatedAt) >= endExclusive)
    if (!complete) {
        apiRequire(totalReceived == null && physicalPickupCount == null && averagePickupDurationSeconds == null)
        return Statistics(null, null, start, endExclusive, complete = false)
    }
    val total = totalReceived ?: invalidApiResponse()
    val physical = physicalPickupCount ?: invalidApiResponse()
    apiRequire(total >= 0 && physical in 0..total)
    apiRequire((physical == 0) == (averagePickupDurationSeconds == null))
    val average = averagePickupDurationSeconds?.let { seconds ->
        apiRequire(seconds.isFinite() && seconds >= 0 && seconds <= (endExclusive - start).toDouble() / 1000)
        val millis = seconds * 1000
        apiRequire(millis <= Long.MAX_VALUE.toDouble())
        millis.toLong()
    }
    return Statistics(total, average, start, endExclusive, complete = true)
}

fun ApiPickupCredential.toDomain(): PickupCredential {
    apiId(membershipId)
    apiRequire(status == "active" && Regex("^[0-9]{4,12}$").matches(code))
    apiText(qrPayload, 2048)
    val verified = apiInstant(verifiedAt)
    val expires = apiInstant(expiresAt)
    val revalidate = apiInstant(revalidateAfter)
    apiRequire(verified <= revalidate && revalidate <= expires && verified < expires)
    return PickupCredential(apiId(parcelId), code, qrPayload, expires, verified, CredentialStatus.ACTIVE,
        revalidateAt = revalidate)
}
