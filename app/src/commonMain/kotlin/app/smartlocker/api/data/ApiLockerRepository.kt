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

