package app.smartlocker.api.data

import app.smartlocker.auth.domain.LoginChannel
import app.smartlocker.config.Brands
import app.smartlocker.parcels.domain.*
import app.smartlocker.profile.domain.CommunicationPreferences
import app.smartlocker.shared.data.HttpTransport
import app.smartlocker.shared.domain.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*
import kotlin.time.Instant

/** Contract tests use the actual session client, headers, transport, DTOs and repository. */
class ApiLockerRepositoryTest {
    @Test fun profileIntersectsCapabilitiesAndRejectsAnotherUser() = runTest {
        val public = configuration.copy(capabilities = capabilities.copy(features = capabilities.features.copy(recipients = false)))
        withRepository(public) { request ->
            assertEquals("/v1/me", request.url.encodedPath)
            json(profile)
        }.useSuspend { fixture ->
            val value = fixture.repo.profile()
            assertEquals("member-1", value.memberships.single().id)
            assertEquals("America/Sao_Paulo", value.memberships.single().timeZone)
            assertFalse(value.memberships.single().features.residents)
            assertEquals(setOf("app", "sms", "email"), value.memberships.single().channels)
            assertEquals(1, fixture.requests.count { it.url.encodedPath == "/v1/me/memberships" })
            fixture.repo.brandConfiguration()
            assertEquals(1, fixture.requests.count { it.url.encodedPath == "/v1/configuration" })
        }
        withRepository { json(profile.copy(id = "another-user")) }.useSuspend {
            assertEquals(FailureKind.UNAVAILABLE, assertFailsWith<AppFailure> { it.repo.profile() }.kind)
            assertFalse(it.requests.any { request -> request.url.encodedPath == "/v1/me/memberships" })
        }
    }

    @Test fun manualMutationUsesReviewedVersionAndDoesNotRetryConflict() = runTest {
        withRepository { request ->
            if (request.method == HttpMethod.Get) json(parcel()) else {
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("/v1/memberships/member-1/parcels/parcel-1/manual-pickup", request.url.encodedPath)
                assertEquals("\"3\"", request.headers["If-Match"])
                assertNotNull(request.headers["Idempotency-Key"])
                respond("""{"type":"about:blank","title":"Versão alterada","status":412,"code":"VERSION_CONFLICT","requestId":"request-1"}""",
                    HttpStatusCode.PreconditionFailed, headersOf(HttpHeaders.ContentType, "application/problem+json"))
            }
        }.useSuspend {
            it.repo.parcel("member-1", "parcel-1")
            assertEquals(FailureKind.CONFLICT, assertFailsWith<AppFailure> { it.repo.markCollected("member-1", "parcel-1") }.kind)
            val requests = it.requests.filter { request -> request.url.encodedPath.contains("/parcels/") }
            assertEquals(listOf(HttpMethod.Get, HttpMethod.Post), requests.map { request -> request.method })
            assertTrue(requests.all { request -> request.headers["X-Brand-Id"] == "smartlocker" })
            assertTrue(requests.all { request -> request.headers[HttpHeaders.Authorization] == "Bearer access-1" })
        }
    }

    @Test fun successfulManualAndUndoUseEachReturnedVersion() = runTest {
        withRepository { request ->
            when (request.method) {
                HttpMethod.Get -> json(parcel())
                HttpMethod.Post -> {
                    assertEquals("\"3\"", request.headers["If-Match"])
                    json(parcel(status = "manual", version = 4))
                }
                HttpMethod.Delete -> {
                    assertEquals("\"4\"", request.headers["If-Match"])
                    json(parcel(version = 5).copy(credentialStatus = "revoked"))
                }
                else -> error("Unexpected method")
            }
        }.useSuspend {
            assertEquals(ParcelStatus.MANUAL, it.repo.markCollected("member-1", "parcel-1").status)
            val undone = it.repo.undoManual("member-1", "parcel-1")
            assertEquals(ParcelStatus.WAITING, undone.status)
            assertEquals(CredentialStatus.REVOKED, undone.credentialStatus)
            assertEquals(1, it.requests.count { request -> request.method == HttpMethod.Get && request.url.encodedPath.contains("/parcels/") })
        }
    }

    @Test fun noticePagesPreserveGlobalUnreadCountAndOpaqueCursor() = runTest {
        val cursor = "opaque /&+=?é"
        withRepository { request ->
            assertEquals("/v1/memberships/member-1/notifications", request.url.encodedPath)
            assertEquals("20", request.url.parameters["limit"])
            val next = request.url.parameters["cursor"]
            if (next == null) json(ApiNoticePage(listOf(notice), page(cursor), 74))
            else {
                assertEquals(cursor, next)
                json(ApiNoticePage(listOf(notice.copy(id = "notice-2", readAt = NOW)), page(null), 74))
            }
        }.useSuspend {
            val first = it.repo.noticePage("member-1")
            assertEquals(74, first.unreadCount)
            assertEquals(cursor, first.nextCursor)
            assertEquals(1, it.requests.count { request -> request.url.encodedPath.endsWith("/notifications") })
            val second = it.repo.noticePage("member-1", first.nextCursor)
            assertEquals(74, second.unreadCount)
            assertNull(second.nextCursor)
        }
    }

    @Test fun responseScopeAndRepeatingCursorAreRejected() = runTest {
        withRepository { request ->
            when {
                request.url.encodedPath.endsWith("/pickup-credential") -> json(ApiPickupCredential("parcel-other", "member-1", "004321", "opaque", "active", NOW, LATER, SOON))
                request.url.encodedPath.endsWith("/parcels/parcel-1") -> json(parcel().copy(membershipId = "member-other"))
                request.url.encodedPath.endsWith("/parcels") -> json(ApiParcelPage(listOf(parcel()), page("same-cursor")))
                else -> error("Unexpected path")
            }
        }.useSuspend {
            assertEquals(FailureKind.UNAVAILABLE, assertFailsWith<AppFailure> { it.repo.parcel("member-1", "parcel-1") }.kind)
            assertEquals(FailureKind.UNAVAILABLE, assertFailsWith<AppFailure> { it.repo.credential("member-1", "parcel-1") }.kind)
            assertEquals(FailureKind.UNAVAILABLE, assertFailsWith<AppFailure> { it.repo.parcels("member-1", ParcelFilter.ALL, "same-cursor") }.kind)
            assertEquals(FailureKind.VALIDATION, assertFailsWith<AppFailure> { it.repo.parcel("../other", "parcel-1") }.kind)
        }
    }

    @Test fun preferencesAndContactUseContractBodiesAndExplicitResend() = runTest {
        withRepository { request ->
            when (request.url.encodedPath) {
                "/v1/me/preferences" -> {
                    assertEquals(HttpMethod.Patch, request.method)
                    assertEquals(ApiJson.encodeToString(ApiCommunicationPreferences(false, true, false)), request.bodyText())
                    json(profile.copy(preferences = ApiCommunicationPreferences(false, true, false)))
                }
                "/v1/me/contact-challenges" -> {
                    val body = ApiJson.parseToJsonElement(request.bodyText()).jsonObject
                    assertEquals("+5511987654321", body.getValue("contact").jsonPrimitive.content)
                    assertEquals("sms", body.getValue("channel").jsonPrimitive.content)
                    json(challenge)
                }
                "/v1/me/contact-challenges/contact-1/resend" -> {
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals("", request.bodyText())
                    json(challenge.copy(id = "contact-2"))
                }
                "/v1/me/contact-challenges/contact-2/verify" -> {
                    assertEquals("{\"code\":\"123456\"}", request.bodyText())
                    json(profile)
                }
                else -> error("Unexpected path")
            }
        }.useSuspend {
            assertFalse(it.repo.updatePreferences(CommunicationPreferences(false, true, false)).preferences.inApp)
            assertEquals("contact-1", it.repo.requestContactChange("(11) 98765-4321", LoginChannel.SMS).id)
            assertEquals("contact-2", it.repo.resendContactChange("contact-1", "ignored@example.test", LoginChannel.EMAIL).id)
            assertEquals("user-1", it.repo.verifyContactChange("contact-2", "123456").id)
        }
    }

    @Test fun supportAndRecipientsHonorCapabilitiesAndParcelScope() = runTest {
        withRepository { request ->
            when {
                request.url.encodedPath.endsWith("/parcels/parcel-1") -> json(parcel())
                request.url.encodedPath.endsWith("/issues") && request.method == HttpMethod.Post -> {
                    assertEquals("A porta não abriu.", ApiJson.parseToJsonElement(request.bodyText()).jsonObject.getValue("message").jsonPrimitive.content)
                    json(issue)
                }
                request.url.encodedPath.endsWith("/issues") -> json(ApiIssuePage(listOf(issue), page(null)))
                request.url.encodedPath.endsWith("/recipients") -> json(ApiRecipientList(listOf(ApiRecipient("recipient-1", "Ana", "Você"))))
                else -> error("Unexpected path")
            }
        }.useSuspend {
            assertEquals("parcel-1", it.repo.reportIssue("member-1", "parcel-1", "  A porta não abriu.  ").parcelId)
            assertEquals(1, it.repo.issuePage("member-1").items.size)
            assertEquals(1, it.repo.recipients("member-1").size)
        }
        val disabled = configuration.copy(capabilities = capabilities.copy(features = capabilities.features.copy(supportIssues = false, recipients = false)))
        withRepository(disabled) { error("Disabled feature must not reach business endpoint") }.useSuspend {
            assertEquals(FailureKind.DENIED, assertFailsWith<AppFailure> { it.repo.issuePage("member-1") }.kind)
            assertEquals(FailureKind.DENIED, assertFailsWith<AppFailure> { it.repo.recipients("member-1") }.kind)
        }
    }

    @Test fun incompleteMetricsRemainUnknownAndNoticeReadResponseIsValidated() = runTest {
        withRepository { request ->
            when {
                request.url.encodedPath.endsWith("/parcel-metrics") -> {
                    assertNull(request.url.parameters["since"])
                    assertNull(request.url.parameters["until"])
                    json(ApiParcelMetrics(EARLIER, NOW, NOW, false, null, null, null))
                }
                request.url.encodedPath.endsWith("/notifications/notice-1/read") -> {
                    assertEquals(HttpMethod.Put, request.method)
                    json(notice.copy(readAt = NOW))
                }
                else -> error("Unexpected path")
            }
        }.useSuspend {
            val metrics = it.repo.statistics("member-1")
            assertFalse(metrics.complete)
            assertNull(metrics.total)
            assertNull(metrics.averageMillis)
            it.repo.markNoticeRead("member-1", "notice-1")
        }
    }

    private class Fixture(val repo: ApiLockerRepository, val requests: MutableList<HttpRequestData>) {
        suspend fun useSuspend(block: suspend (Fixture) -> Unit) { try { block(this) } finally { repo.close() } }
    }

    private suspend fun withRepository(
        public: ApiBrandConfiguration = configuration,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): Fixture {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            when (request.url.encodedPath) {
                "/v1/auth/challenges/login-1/verify" -> json(tokens)
                "/v1/configuration" -> json(public)
                "/v1/me/memberships" -> json(ApiMembershipList(listOf(membership)))
                else -> handler(request)
            }
        }
        val secure = object : SecureStorage {
            val entries = mutableMapOf<String, String>()
            override suspend fun read(key: String) = entries[key]
            override suspend fun write(key: String, value: String?) { if (value == null) entries.remove(key) else entries[key] = value }
        }
        val base = "https://api.example.test/v1"
        val session = ApiSessionClient(HttpTransport(engine, base), Brands.smartLocker, base, secure,
            AppClock { Instant.parse(NOW).toEpochMilliseconds() })
        val repo = ApiLockerRepository(session)
        repo.verifyLogin("login-1", "123456")
        return Fixture(repo, requests)
    }

    private inline fun <reified T> MockRequestHandleScope.json(value: T): HttpResponseData = respond(
        ApiJson.encodeToString(value), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    private fun HttpRequestData.bodyText() = when (val content = body) {
        is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
        is OutgoingContent.NoContent -> ""
