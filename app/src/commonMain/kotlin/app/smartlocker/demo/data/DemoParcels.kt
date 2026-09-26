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
