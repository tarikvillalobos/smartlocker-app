package app.smartlocker.api.data

import app.smartlocker.auth.domain.*
import app.smartlocker.config.Brand
import app.smartlocker.config.Features
import app.smartlocker.parcels.domain.*
import app.smartlocker.profile.domain.*
import app.smartlocker.shared.domain.*
import io.ktor.http.HttpMethod
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Uses only endpoints described by the agreed app API contract. */
class ApiLockerRepository(private val session: ApiSessionClient) : LockerRepository {
    private val cacheMutex = Mutex()
    private var cacheUser: String? = null
    private val memberships = mutableMapOf<String, ApiMembership>()
    private val reviewedParcels = mutableMapOf<Pair<String, String>, Parcel>()

    suspend fun configuration(): ApiBrandConfiguration = session.configuration()
    override suspend fun brandConfiguration(): Brand = configuration().toDomain(session.brand)

    override suspend fun requestLogin(request: LoginRequest): Challenge = session.requestLogin(request)
    override suspend fun resendLogin(challengeId: String, request: LoginRequest): Challenge =
        session.resendLogin(identifier(challengeId))
    override suspend fun verifyLogin(challengeId: String, code: String): Session {
        val result = session.verifyLogin(identifier(challengeId), otp(code))
        clearCache()
        return result
    }
    override suspend fun restoreSession(): Session? = session.restoreSession()
    override suspend fun logout() {
        clearCache()
        session.logout()
    }
    override fun close() = session.close()

    override suspend fun profile(): Profile {
        val user = currentUser()
        val value = request<ApiProfile>(user, "/me")
        requireResponse(value.id == user)
        val list = request<ApiMembershipList>(user, "/me/memberships")
        return profile(value, list.items, user)
    }

    override suspend fun parcels(locationId: String, filter: ParcelFilter, cursor: String?): ParcelPage {
        val user = currentUser()
        val membership = membership(locationId, user)
        val status = when (filter) {
            ParcelFilter.ALL -> "all"
            ParcelFilter.WAITING -> "waiting"
            ParcelFilter.COLLECTED -> "collected"
        }
        val path = pagePath("${membershipPath(locationId)}/parcels", cursor) + "&status=$status"
        val response = request<ApiParcelPage>(user, path)
        requireResponse(response.items.all { it.membershipId == locationId })
        val page = response.toDomain()
        requireResponse(page.items.all { when (filter) {
            ParcelFilter.ALL -> true
            ParcelFilter.WAITING -> it.status == ParcelStatus.WAITING
            ParcelFilter.COLLECTED -> it.status != ParcelStatus.WAITING
        } })
        validateCursor(cursor, page.nextCursor)
        val global = configuration().capabilities
        val items = page.items.map { restrictActions(it, membership.capabilities, global) }
        cache(user) { items.forEach { reviewedParcels[locationId to it.id] = it } }
        return page.copy(items = items)
    }

    override suspend fun parcel(locationId: String, id: String): Parcel {
        val user = currentUser()
        val membership = membership(locationId, user)
        val response = request<ApiParcel>(user, parcelPath(locationId, id))
        return parcel(response, locationId, id, membership.capabilities, user)
    }

    override suspend fun statistics(locationId: String): Statistics {
        val user = currentUser()
        membership(locationId, user)
        // Omitting both dates requests the server's complete rolling 30-day window.
        return request<ApiParcelMetrics>(user, "${membershipPath(locationId)}/parcel-metrics").toDomain()
    }

    override suspend fun credential(locationId: String, parcelId: String): PickupCredential {
        val user = currentUser()
        membership(locationId, user)
        val response = request<ApiPickupCredential>(user, "${parcelPath(locationId, parcelId)}/pickup-credential")
        requireResponse(response.membershipId == locationId && response.parcelId == parcelId)
        return response.toDomain()
    }

    override suspend fun markCollected(locationId: String, parcelId: String): Parcel =
        manualPickup(locationId, parcelId, undo = false)

    override suspend fun undoManual(locationId: String, parcelId: String): Parcel =
        manualPickup(locationId, parcelId, undo = true)

