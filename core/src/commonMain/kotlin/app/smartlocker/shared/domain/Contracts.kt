package app.smartlocker.shared.domain

import app.smartlocker.auth.domain.*
import app.smartlocker.parcels.domain.*
import app.smartlocker.profile.domain.*

fun interface AppClock { fun now(): Long }
interface LocalStorage {
    fun read(key: String): String?
    fun write(key: String, value: String?)
}
interface SecureStorage {
    suspend fun read(key: String): String?
    suspend fun write(key: String, value: String?)
}

enum class FailureKind {
    VALIDATION, NETWORK, EXPIRED_SESSION, DENIED, EXPIRED_CODE,
    INVALID_CODE, ATTEMPTS_EXCEEDED, UNAVAILABLE, CONFLICT, MISSING_CONTRACT,
}
class AppFailure(
    val kind: FailureKind, override val message: String, val retryAt: Long? = null,
) : Exception(message)

interface LockerRepository {
    suspend fun brandConfiguration(): app.smartlocker.config.Brand? = null
    suspend fun requestLogin(request: LoginRequest): Challenge
    suspend fun loginWithPassword(identifier: String, password: String): Session =
        throw AppFailure(FailureKind.UNAVAILABLE, "Login por senha indisponível para esta marca.")
    suspend fun requestPasswordRecovery(identifier: String, channel: LoginChannel): Challenge =
        throw AppFailure(FailureKind.UNAVAILABLE, "Recuperação de senha indisponível para esta marca.")
    suspend fun verifyPasswordRecovery(challengeId: String, code: String, newPassword: String): Session =
        throw AppFailure(FailureKind.UNAVAILABLE, "Recuperação de senha indisponível para esta marca.")
    suspend fun resendLogin(challengeId: String, request: LoginRequest): Challenge = requestLogin(request)
    suspend fun verifyLogin(challengeId: String, code: String): Session
    suspend fun restoreSession(): Session?
    suspend fun logout()
    suspend fun profile(): Profile
    suspend fun parcels(locationId: String, filter: ParcelFilter, cursor: String?): ParcelPage
    suspend fun parcel(locationId: String, id: String): Parcel
    suspend fun delegateCandidates(locationId: String): List<DelegateCandidate> =
        throw AppFailure(FailureKind.UNAVAILABLE, "Delegação indisponível para este local.")
    suspend fun delegateParcel(locationId: String, parcelId: String, delegateMembershipId: String): Parcel =
        throw AppFailure(FailureKind.UNAVAILABLE, "Delegação indisponível para este local.")
    suspend fun removeDelegate(locationId: String, parcelId: String, delegateMembershipId: String): Parcel =
        throw AppFailure(FailureKind.UNAVAILABLE, "Delegação indisponível para este local.")
    suspend fun statistics(locationId: String): Statistics
    suspend fun credential(locationId: String, parcelId: String): PickupCredential
    suspend fun markCollected(locationId: String, parcelId: String): Parcel
    suspend fun undoManual(locationId: String, parcelId: String): Parcel
    suspend fun updatePreferences(value: CommunicationPreferences): Profile
    suspend fun requestContactChange(contact: String, channel: LoginChannel): Challenge
    suspend fun resendContactChange(challengeId: String, contact: String, channel: LoginChannel): Challenge =
        requestContactChange(contact, channel)
    suspend fun verifyContactChange(challengeId: String, code: String): Profile
    suspend fun notifications(locationId: String): List<DeliveryNotice>
    suspend fun noticePage(locationId: String, cursor: String? = null): NoticePage {
        require(cursor == null)
        val values = notifications(locationId)
        return NoticePage(values, null, values.count { !it.read })
    }
    suspend fun markNoticeRead(locationId: String, id: String)
    suspend fun reportIssue(locationId: String, parcelId: String, message: String): SupportIssue
    suspend fun issues(locationId: String): List<SupportIssue>
    suspend fun issuePage(locationId: String, cursor: String? = null): IssuePage {
        require(cursor == null)
        return IssuePage(issues(locationId), null)
    }
    suspend fun recipients(locationId: String): List<Recipient>
    fun close() = Unit
}

interface DemoControls {
    suspend fun deposit(locationId: String)
    suspend fun physicalPickup(locationId: String, parcelId: String)
    suspend fun scenario(value: DemoScenario)
}
enum class DemoScenario { NORMAL, EMPTY, MANY, LONG_TEXT, NETWORK, DENIED, EXPIRED_SESSION, EXPIRED_CODE, LOCKER_OFFLINE }
