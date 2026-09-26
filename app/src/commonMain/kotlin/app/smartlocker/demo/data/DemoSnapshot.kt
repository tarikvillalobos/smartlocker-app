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
