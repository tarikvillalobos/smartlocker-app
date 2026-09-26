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
