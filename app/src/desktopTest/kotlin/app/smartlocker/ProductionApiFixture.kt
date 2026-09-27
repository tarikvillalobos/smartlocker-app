package app.smartlocker

import app.smartlocker.api.data.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Clock
import kotlin.time.Instant

/** Stateful HTTP fixture, independent from the demo repository and private contract-test fixtures. */
internal class ProductionApiFixture(private val authMethods: List<String>? = null) {
    val requests = CopyOnWriteArrayList<HttpRequestData>()
    @Volatile var unavailable = false
    @Volatile private var manual = false
    private val now = Clock.System.now().toEpochMilliseconds()
    private fun at(offset: Long) = Instant.fromEpochMilliseconds(now + offset).toString()
    private val yes = ApiChannelCapability(true)
    private val no = ApiChannelCapability(false)
    private val capabilities = ApiCapabilities(ApiFeatures(true, true, false, false, false, false),
        ApiChannels(yes, yes, yes, no, no))
    private val membership = ApiMembership("member-api", "location-api", "Residencial API", "unit-api", "42",
        "America/Sao_Paulo", capabilities)
    private val profile = ApiProfile("user-api", "Ana API", "+5511987654321", at(0), null, null,
        ApiCommunicationPreferences(true, true, false), listOf(membership))
    private fun page() = ApiPageInfo(null, at(0), at(600_000))
    private fun parcel() = ApiParcel("parcel-api", "recipient-api", "member-api", "Transportadora API", "BR123",
        if (manual) "manual" else "waiting", ApiLocker("locker-api", "Portaria API", "Rua das Flores, 120", true),
        "4", null, at(-3_600_000), at(-3_600_000), at(3_600_000), if (manual) at(0) else null, null,
        if (manual) "revoked" else "active", ApiParcelActions(!manual, manual, if (manual) at(600_000) else null, false),
        listOf(ApiTimelineEvent("deposited", at(-3_600_000))), if (manual) 4 else 3)

    val engine = MockEngine { request ->
        requests += request
        val path = request.url.encodedPath.removePrefix("/v1")
        if (unavailable && path == "/me") {
            respond("""{"type":"about:blank","title":"Indisponível","status":503,"code":"SERVICE_UNAVAILABLE","requestId":"test-1"}""",
                HttpStatusCode.ServiceUnavailable, headersOf(HttpHeaders.ContentType, "application/problem+json"))
        } else when (path) {
            "/configuration" -> json(ApiBrandConfiguration("smartlocker", "Marca da API", capabilities, null, null, null,
                authMethods))
            "/auth/challenges" -> json(ApiChallenge("login-api", at(600_000), at(30_000), "sms", "+55 ** *****-4321", 6, "login"), HttpStatusCode.Accepted)
            "/auth/challenges/login-api/verify" -> json(ApiSessionTokens("Bearer", "access-api", at(3_600_000),
                "refresh-api", at(86_400_000), "user-api", "smartlocker", "session-api", listOf("parcels:read", "parcels:manual")))
            "/auth/password/login" -> json(ApiSessionTokens("Bearer", "access-api", at(3_600_000),
                "refresh-api", at(86_400_000), "user-api", "smartlocker", "session-api", listOf("parcels:read")))
            "/me" -> json(profile)
            "/me/memberships" -> json(ApiMembershipList(listOf(membership)))
            "/memberships/member-api/parcels" -> {
                val status = request.url.parameters["status"]
                val show = status == "all" || (status == "waiting" && !manual) || (status == "collected" && manual)
                json(ApiParcelPage(if (show) listOf(parcel()) else emptyList(), page()))
            }
            "/memberships/member-api/parcel-metrics" -> json(ApiParcelMetrics(at(-86_400_000), at(0), at(0), true, 1, 0, null))
            "/memberships/member-api/notifications" -> json(ApiNoticePage(emptyList(), page(), 0))
            "/memberships/member-api/parcels/parcel-api" -> json(parcel())
            "/memberships/member-api/parcels/parcel-api/pickup-credential" -> json(ApiPickupCredential("parcel-api",
                "member-api", "004321", "opaque-server-credential", "active", at(0), at(600_000), at(300_000)))
            "/memberships/member-api/parcels/parcel-api/manual-pickup" -> {
                check(request.method == HttpMethod.Post && request.headers["If-Match"] == "\"3\"")
                check(request.headers["Idempotency-Key"] != null)
                manual = true
                json(parcel())
            }
            else -> error("Unexpected documented path: $path")
        }
    }
    private inline fun <reified T> MockRequestHandleScope.json(value: T, status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData = respond(
        ApiJson.encodeToString(value), status, headersOf(HttpHeaders.ContentType, "application/json"))
}
