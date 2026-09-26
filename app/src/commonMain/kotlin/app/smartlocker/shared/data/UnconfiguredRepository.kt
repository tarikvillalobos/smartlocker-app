package app.smartlocker.shared.data

import app.smartlocker.auth.domain.*
import app.smartlocker.parcels.domain.*
import app.smartlocker.profile.domain.*
import app.smartlocker.shared.domain.*

/** Fail closed: production never delegates to a demo repository. */
class UnconfiguredRepository : LockerRepository {
    private fun unavailable(): Nothing = throw AppFailure(FailureKind.MISSING_CONTRACT,
        "API externa não configurada. Configure um endereço HTTPS válido para esta distribuição.")
    override suspend fun requestLogin(request: LoginRequest): Challenge = unavailable()
    override suspend fun verifyLogin(challengeId: String, code: String): Session = unavailable()
    override suspend fun restoreSession(): Session? = null
    override suspend fun logout() = Unit
    override suspend fun profile(): Profile = unavailable()
    override suspend fun parcels(locationId: String, filter: ParcelFilter, cursor: String?): ParcelPage = unavailable()
    override suspend fun parcel(locationId: String, id: String): Parcel = unavailable()
    override suspend fun statistics(locationId: String): Statistics = unavailable()
    override suspend fun credential(locationId: String, parcelId: String): PickupCredential = unavailable()
    override suspend fun markCollected(locationId: String, parcelId: String): Parcel = unavailable()
    override suspend fun undoManual(locationId: String, parcelId: String): Parcel = unavailable()
    override suspend fun updatePreferences(value: CommunicationPreferences): Profile = unavailable()
    override suspend fun requestContactChange(contact: String, channel: LoginChannel): Challenge = unavailable()
    override suspend fun verifyContactChange(challengeId: String, code: String): Profile = unavailable()
    override suspend fun notifications(locationId: String): List<DeliveryNotice> = unavailable()
    override suspend fun markNoticeRead(locationId: String, id: String): Unit = unavailable()
    override suspend fun reportIssue(locationId: String, parcelId: String, message: String): SupportIssue = unavailable()
    override suspend fun issues(locationId: String): List<SupportIssue> = unavailable()
    override suspend fun recipients(locationId: String): List<Recipient> = unavailable()
}
