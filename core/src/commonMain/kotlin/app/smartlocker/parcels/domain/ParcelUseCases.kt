package app.smartlocker.parcels.domain

import app.smartlocker.shared.domain.*

data class HistoryResult(val page: ParcelPage, val statistics: Statistics)

/** Aggregates remain independent from the current page. */
class LoadHistory(private val repository: LockerRepository) {
    suspend operator fun invoke(location: String, filter: ParcelFilter, cursor: String? = null): HistoryResult {
        val page = repository.parcels(location, filter, cursor)
        val statistics = repository.statistics(location)
        return HistoryResult(page, statistics)
    }
}

/** Validates presentation scope; authorization remains a server responsibility. */
class LoadPickupCredential(private val repository: LockerRepository, private val clock: AppClock) {
    suspend operator fun invoke(location: String, parcel: Parcel): PickupCredential {
        if (parcel.locationId != location || parcel.status != ParcelStatus.WAITING) {
            throw AppFailure(FailureKind.CONFLICT, "Selecione uma encomenda aguardando retirada neste local.")
        }
        val credential = repository.credential(location, parcel.id)
        if (credential.parcelId != parcel.id || !credential.canDisplay(clock.now(), true) ||
            credential.code.isBlank() || credential.payload.isBlank() || credential.payload.length > 2048) {
            throw AppFailure(FailureKind.EXPIRED_CODE, "Não foi possível confirmar a validade do código. Atualize os dados.")
        }
        return credential
    }
}
