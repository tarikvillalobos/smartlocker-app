package app.smartlocker.demo.data

import app.smartlocker.parcels.domain.*
import app.smartlocker.shared.domain.*

class DemoParcels(private val db: DemoDatabase, private val clock: AppClock) {
    fun all(location: String): List<Parcel> {
        requireLocation(location)
        return db.snapshot.parcels.filter { it.recipient == "ana" && it.location == location }
            .map { it.toDomain(clock.now()) }.sortedByDescending { it.depositedAt }
    }

    fun get(location: String, id: String): Parcel = all(location).find { it.id == id }
        ?: throw AppFailure(FailureKind.DENIED, "Encomenda não disponível para este vínculo.")

    fun page(location: String, filter: ParcelFilter, cursor: String?): ParcelPage {
        val offset = cursor?.toIntOrNull() ?: 0
        if (offset < 0) throw AppFailure(FailureKind.VALIDATION, "Página inválida.")
        val items = all(location).filtered(filter)
        return ParcelPage(items.drop(offset).take(20),
            if (offset + 20 < items.size) (offset + 20).toString() else null)
    }

    fun credential(location: String, id: String): PickupCredential {
        val item = get(location, id)
        if (!item.lockerAvailable) throw AppFailure(FailureKind.UNAVAILABLE, "Armário indisponível no momento.")
        if (item.status != ParcelStatus.WAITING || item.credentialStatus != CredentialStatus.ACTIVE) {
            throw AppFailure(FailureKind.EXPIRED_CODE, "Código expirado, revogado ou já utilizado.")
        }
        val digits = (100000 + (id.hashCode().toLong().let { if (it < 0) -it else it } % 900000)).toString()
        return PickupCredential(id, digits, "SMARTLOCKER-DEMO|$location|$id|$digits",
            item.deadline, clock.now(), CredentialStatus.ACTIVE)
    }

    fun mark(location: String, id: String, physical: Boolean): Parcel {
        val item = get(location, id)
        if (item.collectedAt != null) return item
        if (physical) credential(location, id)
        else if (item.manualAt != null) return item
        change(id) {
