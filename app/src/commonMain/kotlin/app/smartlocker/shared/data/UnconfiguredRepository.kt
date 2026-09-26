package app.smartlocker.shared.data

import app.smartlocker.auth.domain.*
import app.smartlocker.parcels.domain.*
import app.smartlocker.profile.domain.*
import app.smartlocker.shared.domain.*

/** Fail closed: production never delegates to a demo repository. */
class UnconfiguredRepository : LockerRepository {
    private fun unavailable(): Nothing = throw AppFailure(FailureKind.MISSING_CONTRACT,
        "API externa não configurada. Aguardando documentação e homologação.")
    override suspend fun requestLogin(request: LoginRequest): Challenge = unavailable()
    override suspend fun verifyLogin(challengeId: String, code: String): Session = unavailable()
    override suspend fun restoreSession(): Session? = null
    override suspend fun logout() = Unit
    override suspend fun profile(): Profile = unavailable()
    override suspend fun parcels(locationId: String, filter: ParcelFilter, cursor: String?): ParcelPage = unavailable()
    override suspend fun parcel(locationId: String, id: String): Parcel = unavailable()
    override suspend fun statistics(locationId: String): Statistics = unavailable()
    override suspend fun credential(locationId: String, parcelId: String): PickupCredential = unavailable()
