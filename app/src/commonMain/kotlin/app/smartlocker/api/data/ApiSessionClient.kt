package app.smartlocker.api.data

import app.smartlocker.auth.domain.*
import app.smartlocker.config.Brand
import app.smartlocker.shared.data.HttpTransport
import app.smartlocker.shared.domain.*
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
private data class StoredApiSession(
    val baseUrl: String, val tokens: ApiSessionTokens, val pendingRefreshKey: String? = null,
) {
    override fun toString() = "StoredApiSession(redacted)"
}

/** Rotating tokens live only in the native vault; mutations are never automatically replayed. */
@OptIn(ExperimentalUuidApi::class)
class ApiSessionClient(
    private val transport: HttpTransport,
    val brand: Brand,
    private val baseUrl: String,
    secure: SecureStorage,
    private val clock: AppClock,
) {
    private val store = ChunkedSecureStore(secure, "api.${apiId(brand.id)}.session")
    private val mutex = Mutex()
    private var stored: StoredApiSession? = null
    private var restored = false
    private var generation = 0
    private var publicConfiguration: ApiBrandConfiguration? = null
    private data class Intent(val method: String, val path: String, val body: String?, val headers: Map<String, String>)
    private val uncertain = mutableMapOf<Intent, String>()
    private val verificationKeys = mutableMapOf<Pair<String, String>, String>()

    fun currentSession(): Session? = stored?.tokens?.toDomain()

    suspend fun configuration(): ApiBrandConfiguration {
        publicConfiguration?.let { return it }
        val value = decodeApi<ApiBrandConfiguration>(request("/configuration", authenticated = false))
        value.toDomain(brand)
        publicConfiguration = value
        return value
    }

    suspend fun requestLogin(value: LoginRequest): Challenge {
        if (!InputValidation.cpf(value.cpf)) invalidInput("Confira o CPF informado.")
        val contact = normalizedApiContact(value.contact, value.channel)
        val capabilities = configuration().capabilities.channels
        if (!(if (value.channel == LoginChannel.SMS) capabilities.sms else capabilities.email).available) {
            throw AppFailure(FailureKind.UNAVAILABLE, "Este canal de login está indisponível. Escolha outro canal.")
        }
        val body = ApiJson.encodeToString(ApiLoginRequest(contact, value.cpf.filter(Char::isDigit), value.channel.apiValue()))
        return decodeApi<ApiChallenge>(request("/auth/challenges", HttpMethod.Post, body, authenticated = false)).toDomain("login")
    }

    suspend fun resendLogin(challengeId: String): Challenge = decodeApi<ApiChallenge>(request(
        "/auth/challenges/${apiId(challengeId)}/resend", HttpMethod.Post, authenticated = false,
    )).toDomain("login")

    suspend fun verifyLogin(challengeId: String, code: String): Session = mutex.withLock {
        validateApiOtp(code)
        val epoch = generation
        val verification = challengeId to code
        val key = verificationKeys.getOrPut(verification) { Uuid.random().toString() }
        val response = try {
            request("/auth/challenges/${apiId(challengeId)}/verify", HttpMethod.Post,
                ApiJson.encodeToString(ApiOtpVerification(code)), mapOf("Idempotency-Key" to key), authenticated = false)
        } catch (error: AppFailure) {
            if (error.kind !in setOf(FailureKind.NETWORK, FailureKind.UNAVAILABLE)) verificationKeys.remove(verification)
            throw error
        }
        val tokens = decodeApi<ApiSessionTokens>(response)
        validateTokens(tokens)
        ensureCurrent(epoch)
        save(StoredApiSession(baseUrl, tokens), epoch)
        restored = true
        verificationKeys.clear()
        tokens.toDomain()
    }

    suspend fun restoreSession(): Session? = mutex.withLock {
        restoreLocked()
        if (stored == null) return@withLock null
        accessLocked()
        currentSession()
    }

    private suspend fun restoreLocked() {
        if (restored) return
        val epoch = generation
        val raw = store.read()
        ensureCurrent(epoch)
        val value = raw?.let { runCatching { ApiJson.decodeFromString<StoredApiSession>(it) }.getOrNull() }
        val valid = value != null && value.baseUrl == baseUrl &&
            runCatching { validateTokens(value.tokens); value.pendingRefreshKey?.let(Uuid::parse) }.isSuccess
        if (!valid) {
            if (raw != null) store.write(null)
            stored = null
        } else stored = value
        ensureCurrent(epoch)
        restored = true
    }

    private fun validateTokens(tokens: ApiSessionTokens) {
        tokens.toDomain()
        apiRequire(tokens.brandId == brand.id)
        apiRequire(apiInstant(tokens.accessExpiresAt) <= apiInstant(tokens.refreshExpiresAt))
        apiRequire(tokens.accessToken.all { it.code in 33..126 })
    }

    private suspend fun accessLocked(): ApiSessionTokens {
        restoreLocked()
        var value = stored ?: expired()
        if (apiInstant(value.tokens.refreshExpiresAt) <= clock.now()) expired()
        if (apiInstant(value.tokens.accessExpiresAt) > clock.now() + 15_000) return value.tokens
        val epoch = generation
        if (value.pendingRefreshKey == null) {
            value = value.copy(pendingRefreshKey = Uuid.random().toString())
            save(value, epoch)
        }
        try {
            val response = request("/auth/refresh", HttpMethod.Post,
                ApiJson.encodeToString(ApiRefreshRequest(value.tokens.refreshToken)),
                mapOf("Idempotency-Key" to value.pendingRefreshKey!!), authenticated = false)
            val tokens = decodeApi<ApiSessionTokens>(response)
            validateTokens(tokens)
            apiRequire(tokens.userId == value.tokens.userId && tokens.sessionId == value.tokens.sessionId)
            apiRequire(apiInstant(tokens.accessExpiresAt) > clock.now())
            save(StoredApiSession(baseUrl, tokens), epoch)
            return tokens
        } catch (error: AppFailure) {
            if (error.kind == FailureKind.EXPIRED_SESSION) expired()
            throw error
        }
    }

    private suspend fun save(value: StoredApiSession, epoch: Int) {
        ensureCurrent(epoch)
        store.write(ApiJson.encodeToString(value))
        ensureCurrent(epoch)
        stored = value
    }

    private suspend fun expired(): Nothing {
        stored = null
        store.write(null)
        throw AppFailure(FailureKind.EXPIRED_SESSION, "Sua sessão expirou. Entre novamente.")
    }

    suspend fun request(
        path: String, method: HttpMethod = HttpMethod.Get, body: String? = null,
        headers: Map<String, String> = emptyMap(), authenticated: Boolean = true,
    ): String {
        val epoch = generation
        val token = if (authenticated) mutex.withLock { accessLocked().accessToken } else null
        ensureCurrent(epoch)
        val requestHeaders = headers.toMutableMap()
        requestHeaders["X-Brand-Id"] = brand.id
        if (token != null) requestHeaders["Authorization"] = "Bearer $token"
        val intent = if (method != HttpMethod.Get && "Idempotency-Key" !in headers) Intent(method.value, path, body, headers) else null
        if (intent != null) {
            if (intent !in uncertain && uncertain.size >= 64) {
                throw AppFailure(FailureKind.UNAVAILABLE, "Há operações sem confirmação. Atualize antes de tentar novamente.")
            }
            requestHeaders["Idempotency-Key"] = uncertain.getOrPut(intent) { Uuid.random().toString() }
        }
        try {
            val response = transport.execute(path, method, requestHeaders, body)
            ensureCurrent(epoch)
            if (intent != null) uncertain.remove(intent)
            return response
        } catch (error: AppFailure) {
            ensureCurrent(epoch)
            if (intent != null && error.kind !in setOf(FailureKind.NETWORK, FailureKind.UNAVAILABLE)) uncertain.remove(intent)
            if (authenticated && error.kind == FailureKind.EXPIRED_SESSION) mutex.withLock { expired() }
            throw error
        }
    }

    suspend fun logout() {
        generation++
        val token = stored?.tokens?.accessToken
        mutex.withLock {
            stored = null
            restored = true
            uncertain.clear()
            verificationKeys.clear()
            withContext(NonCancellable) { store.write(null) }
        }
        if (token != null) {
            try {
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) {
                throw AppFailure(FailureKind.NETWORK, "Sessão removida deste dispositivo. Não foi possível confirmar a revogação remota.")
            }
        }
    }

    private fun ensureCurrent(epoch: Int) {
        if (generation != epoch) throw CancellationException("API context changed")
    }

    fun close() {
        generation++
        stored = null
        uncertain.clear()
        verificationKeys.clear()
        transport.close()
    }
}
