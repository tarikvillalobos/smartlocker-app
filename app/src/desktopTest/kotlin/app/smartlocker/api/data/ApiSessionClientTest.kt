package app.smartlocker.api.data

import app.smartlocker.MemorySecure
import app.smartlocker.TestClock
import app.smartlocker.auth.domain.LoginChannel
import app.smartlocker.auth.domain.LoginRequest
import app.smartlocker.config.Brands
import app.smartlocker.shared.data.HttpTransport
import app.smartlocker.shared.domain.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.IOException
import java.util.UUID
import kotlin.test.*
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ApiSessionClientTest {
    @Test fun loginNormalizesCpfAndBrazilianPhoneAndSuppliesBrandAndIdempotencyHeaders() = runTest {
        val clock = TestClock()
        var challenges = 0
        val client = client(clock) { request ->
            assertEquals("smartlocker", request.headers["X-Brand-Id"])
            assertNull(request.headers[HttpHeaders.Authorization])
            when (request.path()) {
                "/configuration" -> respond(configuration(), headers = JSON)
                "/auth/challenges" -> {
                    challenges++
                    assertEquals(HttpMethod.Post, request.method)
                    assertNotNull(UUID.fromString(request.headers["Idempotency-Key"]))
                    val body = request.jsonBody()
                    assertEquals("+5511987654321", body.getValue("contact").jsonPrimitive.content)
                    assertEquals("52998224725", body.getValue("cpf").jsonPrimitive.content)
                    assertEquals("sms", body.getValue("channel").jsonPrimitive.content)
                    respond(challenge(clock), HttpStatusCode.Accepted, JSON)
                }
                else -> error("Unexpected fixture path")
            }
        }
        try {
            val result = client.requestLogin(LoginRequest("(11) 98765-4321", "529.982.247-25", LoginChannel.SMS))
            assertEquals("challenge-1", result.id)
            assertEquals(1, challenges)
        } finally { client.close() }
    }

    @Test fun invalidOtpUsesSafeLocalTextAndInvalidInputDoesNotReachTheServer() = runTest {
        val clock = TestClock()
        val secure = MemorySecure()
        var calls = 0
        val client = client(clock, secure) {
            calls++
            respond("""{"status":422,"code":"INVALID_OTP","detail":"SECRET_FIXTURE_BODY"}""",
                HttpStatusCode.UnprocessableEntity, PROBLEM)
        }
        try {
            assertEquals(FailureKind.VALIDATION, assertFailsWith<AppFailure> { client.verifyLogin("challenge-1", "12345") }.kind)
            assertEquals(0, calls)
            val failure = assertFailsWith<AppFailure> { client.verifyLogin("challenge-1", "123456") }
            assertEquals(FailureKind.INVALID_CODE, failure.kind)
            assertFalse(failure.message.contains("SECRET_FIXTURE_BODY"))
            assertEquals(1, calls)
            assertNull(client.currentSession())
            assertTrue(secure.values.isEmpty())
        } finally { client.close() }
    }

    @Test fun verifiedSessionUsesBoundedSecureEntriesAndRestoresWithoutAnotherLogin() = runTest {
        val clock = TestClock()
        val secure = MemorySecure()
        val access = "access-" + "A".repeat(8000)
        val refresh = "refresh-" + "R".repeat(8000)
        val first = client(clock, secure) { respond(tokens(clock, access = access, refresh = refresh), headers = JSON) }
        first.verifyLogin("challenge-1", "123456")
        first.close()
        assertTrue(secure.values.isNotEmpty())
        assertTrue(secure.values.values.all { it.length <= 1500 && !it.contains(access) && !it.contains(refresh) })
        var requests = 0
        val restored = client(clock, secure) { requests++; error("Restore must use the vault") }
        try {
            assertTrue(restored.restoreSession()?.token == access)
            assertEquals("ana", restored.currentSession()?.userId)
            assertEquals(0, requests)
        } finally { restored.close() }
    }

    @Test fun concurrentReadsShareOneRotationAndBothUseTheNewAccessToken() = runTest {
        val clock = TestClock()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var rotations = 0
        val authorizations = mutableListOf<String?>()
        val client = client(clock) { request ->
            when (request.path()) {
                "/auth/challenges/challenge-1/verify" -> respond(tokens(clock, accessIn = 10_000), headers = JSON)
                "/auth/refresh" -> {
                    rotations++
                    assertNull(request.headers[HttpHeaders.Authorization])
                    assertEquals("refresh-old", request.jsonBody().getValue("refreshToken").jsonPrimitive.content)
                    entered.complete(Unit)
                    release.await()
                    respond(tokens(clock, access = "access-new", refresh = "refresh-new"), headers = JSON)
                }
                "/me" -> {
                    authorizations += request.headers[HttpHeaders.Authorization]
                    respond("{}", headers = JSON)
                }
                else -> error("Unexpected fixture path")
            }
        }
        try {
            client.verifyLogin("challenge-1", "123456")
            val reads = List(2) { async { client.request("/me") } }
            entered.await()
            runCurrent()
            assertEquals(1, rotations)
            release.complete(Unit)
            reads.awaitAll()
            assertEquals(1, rotations)
            assertEquals(listOf<String?>("Bearer access-new", "Bearer access-new"), authorizations)
            assertTrue(client.currentSession()?.token == "access-new")
        } finally { release.complete(Unit); client.close() }
    }

    @Test fun uncertainRefreshKeepsItsKeyAcrossExplicitRetryAndNewClientRestore() = runTest {
        val clock = TestClock()
        val secure = MemorySecure()
        val keys = mutableListOf<String?>()
        var refreshCalls = 0
        val first = client(clock, secure) { request ->
            if (request.path().endsWith("/verify")) respond(tokens(clock, accessIn = 10_000), headers = JSON)
            else {
                assertEquals("/auth/refresh", request.path())
                refreshCalls++
                keys += request.headers["Idempotency-Key"]
                throw IOException("Synthetic uncertain refresh")
            }
        }
        try {
            first.verifyLogin("challenge-1", "123456")
            repeat(2) { assertEquals(FailureKind.NETWORK, assertFailsWith<AppFailure> { first.request("/me") }.kind) }
            assertEquals(2, refreshCalls)
            assertNotNull(keys.first())
            assertEquals(keys[0], keys[1])
        } finally { first.close() }
        val second = client(clock, secure) { request ->
            assertEquals("/auth/refresh", request.path())
            keys += request.headers["Idempotency-Key"]
            assertEquals("refresh-old", request.jsonBody().getValue("refreshToken").jsonPrimitive.content)
            respond(tokens(clock, access = "access-rotated", refresh = "refresh-rotated"), headers = JSON)
        }
        try {
            assertTrue(second.restoreSession()?.token == "access-rotated")
            assertEquals(keys[0], keys[2])
        } finally { second.close() }
    }

    @Test fun localWriteFailureAfterOtpResponsePreservesTheVerificationKey() = runTest {
        val clock = TestClock()
        val memory = MemorySecure()
        val secure = object : SecureStorage by memory {
            var fail = true
            override suspend fun write(key: String, value: String?) {
                if (fail && key.endsWith(".head") && value != null) throw AppFailure(FailureKind.UNAVAILABLE, "Synthetic vault failure")
                memory.write(key, value)
            }
        }
        val keys = mutableListOf<String?>()
        val client = client(clock, secure) { request ->
            keys += request.headers["Idempotency-Key"]
            respond(tokens(clock), headers = JSON)
        }
        try {
            assertFailsWith<AppFailure> { client.verifyLogin("challenge-1", "123456") }
            assertNull(client.currentSession())
            secure.fail = false
            client.verifyLogin("challenge-1", "123456")
            assertEquals(2, keys.size)
            assertNotNull(keys[0])
            assertEquals(keys[0], keys[1])
            assertEquals("ana", client.currentSession()?.userId)
        } finally { client.close() }
    }

    @Test fun logoutNetworkFailureStillClearsMemoryAndVaultAndReportsUnconfirmedRevocation() = runTest {
        val clock = TestClock()
        val secure = MemorySecure()
        var revocations = 0
        val client = client(clock, secure) { request ->
            if (request.path().endsWith("/verify")) respond(tokens(clock), headers = JSON)
            else {
                assertEquals("/auth/logout", request.path())
                assertEquals("Bearer access-old", request.headers[HttpHeaders.Authorization])
                revocations++
                throw IOException("Synthetic remote outage")
            }
        }
        try {
            client.verifyLogin("challenge-1", "123456")
            val failure = assertFailsWith<AppFailure> { client.logout() }
            assertEquals(FailureKind.NETWORK, failure.kind)
            assertTrue(failure.message.contains("Não foi possível confirmar a revogação remota"))
            assertEquals(1, revocations)
            assertNull(client.currentSession())
            assertTrue(secure.values.isEmpty())
            assertNull(client.restoreSession())
        } finally { client.close() }
    }

    @Test fun logoutOrCloseRejectsAnUncooperativeLateVerificationResponse() = runTest {
        for (logout in listOf(false, true)) {
            val clock = TestClock()
            val secure = MemorySecure()
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val client = client(clock, secure) {
                entered.complete(Unit)
                withContext(NonCancellable) { release.await() }
                respond(tokens(clock), headers = JSON)
            }
            try {
                val verification = async { runCatching { client.verifyLogin("challenge-1", "123456") } }
                entered.await()
                val leaving = if (logout) async { client.logout() } else null
                if (!logout) client.close()
                runCurrent()
                release.complete(Unit)
                assertTrue(verification.await().exceptionOrNull() is CancellationException)
                leaving?.await()
                assertNull(client.currentSession())
                assertTrue(secure.values.isEmpty())
            } finally { release.complete(Unit); client.close() }
        }
    }

    @Test fun wrongBrandOrChangedUserAndSessionCannotReplaceVerifiedIdentity() = runTest {
        val clock = TestClock()
        val secure = MemorySecure()
        val wrongBrand = client(clock, secure) { respond(tokens(clock, brand = "another-brand"), headers = JSON) }
        try {
            assertFailsWith<AppFailure> { wrongBrand.verifyLogin("challenge-1", "123456") }
            assertNull(wrongBrand.currentSession())
            assertTrue(secure.values.isEmpty())
        } finally { wrongBrand.close() }
        for (changedUser in listOf(false, true)) {
            var protectedReads = 0
            val client = client(clock) { request ->
                when (request.path()) {
                    "/auth/challenges/challenge-1/verify" -> respond(tokens(clock, accessIn = 10_000), headers = JSON)
                    "/auth/refresh" -> respond(tokens(clock, user = if (changedUser) "other" else "ana",
                        session = if (changedUser) "session-1" else "other-session"), headers = JSON)
                    else -> { protectedReads++; respond("{}", headers = JSON) }
                }
            }
            try {
                client.verifyLogin("challenge-1", "123456")
                assertFailsWith<AppFailure> { client.request("/me") }
                assertEquals("ana", client.currentSession()?.userId)
                assertEquals(0, protectedReads)
            } finally { client.close() }
        }
    }

    @Test fun differentBackendCannotRestoreOrTransmitAnotherOriginsSession() = runTest {
        val clock = TestClock()
        val secure = MemorySecure()
        val first = client(clock, secure) { respond(tokens(clock), headers = JSON) }
        first.verifyLogin("challenge-1", "123456")
        first.close()
        var calls = 0
        val second = client(clock, secure, "https://different.example.test/v1") { calls++; error("No request is authorized") }
        try {
            assertNull(second.restoreSession())
            assertEquals(0, calls)
            assertTrue(secure.values.isEmpty())
        } finally { second.close() }
    }

    @Test fun uncertainMutationRequiresExplicitRetryWithSameKeyAndSuccessStartsANewIntent() = runTest {
        val clock = TestClock()
        val keys = mutableListOf<String?>()
        val client = client(clock) { request ->
            if (request.path().endsWith("/verify")) respond(tokens(clock), headers = JSON)
            else {
                keys += request.headers["Idempotency-Key"]
                assertEquals("\"3\"", request.headers[HttpHeaders.IfMatch])
                if (keys.size == 1) throw IOException("Synthetic lost response")
                if (keys.size == 2) respond("""{"status":409,"code":"OPERATION_IN_PROGRESS"}""", HttpStatusCode.Conflict, PROBLEM)
                else respond("{}", headers = JSON)
            }
        }
        try {
            client.verifyLogin("challenge-1", "123456")
            suspend fun mutation() = client.request("/memberships/home/parcels/parcel-1/manual-pickup",
                HttpMethod.Post, headers = mapOf(HttpHeaders.IfMatch to "\"3\""))
            assertFailsWith<AppFailure> { mutation() }
            assertEquals(1, keys.size)
            assertEquals(FailureKind.UNAVAILABLE, assertFailsWith<AppFailure> { mutation() }.kind)
            assertEquals(2, keys.size)
            assertNotNull(keys[0])
            assertEquals(keys[0], keys[1])
            mutation()
            assertEquals(3, keys.size)
            assertEquals(keys[1], keys[2])
            mutation()
            assertEquals(4, keys.size)
            assertNotEquals(keys[2], keys[3])
        } finally { client.close() }
    }

    @Test fun malformedUnauthorizedProblemStillExpiresTheSession() = runTest {
        val clock = TestClock()
        val secure = MemorySecure()
        val client = client(clock, secure) { request ->
            if (request.path().endsWith("/verify")) respond(tokens(clock), headers = JSON)
            else respond("""{"status":{},"code":[],"detail":"SECRET_FIXTURE_BODY"}""", HttpStatusCode.Unauthorized, PROBLEM)
        }
        try {
            client.verifyLogin("challenge-1", "123456")
            val failure = assertFailsWith<AppFailure> { client.request("/me") }
            assertEquals(FailureKind.EXPIRED_SESSION, failure.kind)
            assertFalse(failure.message.contains("SECRET_FIXTURE_BODY"))
            assertNull(client.currentSession())
            assertTrue(secure.values.isEmpty())
        } finally { client.close() }
    }

    @Test fun lateUnauthorizedResponseFromOldAccessTokenCannotEraseARotatedSession() = runTest {
        val clock = TestClock()
        val secure = MemorySecure()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var olderCalls = 0
        val client = client(clock, secure) { request ->
            when (request.path()) {
                "/auth/challenges/challenge-1/verify" -> respond(tokens(clock, accessIn = 20_000), headers = JSON)
                "/older" -> {
                    olderCalls++
                    assertEquals("Bearer access-old", request.headers[HttpHeaders.Authorization])
                    entered.complete(Unit)
                    release.await()
                    respond("""{"status":401,"code":"SESSION_EXPIRED"}""", HttpStatusCode.Unauthorized, PROBLEM)
                }
                "/auth/refresh" -> respond(tokens(clock, access = "access-new", refresh = "refresh-new"), headers = JSON)
                "/newer" -> {
                    assertEquals("Bearer access-new", request.headers[HttpHeaders.Authorization])
                    respond("{}", headers = JSON)
                }
                else -> error("Unexpected fixture path")
            }
        }
        try {
            client.verifyLogin("challenge-1", "123456")
            val older = async { runCatching { client.request("/older") } }
            entered.await()
            clock.time += 6_000
            client.request("/newer")
            release.complete(Unit)
            val failure = older.await().exceptionOrNull()
            assertTrue(failure is CancellationException || (failure is AppFailure && failure.kind != FailureKind.EXPIRED_SESSION))
            assertEquals(1, olderCalls)
            assertTrue(client.currentSession()?.token == "access-new")
            assertTrue(secure.values.isNotEmpty())
        } finally { release.complete(Unit); client.close() }
    }

    @Test fun unauthorizedStillExpiresMemoryWhenProtectedSessionCannotBeRemoved() = runTest {
        val clock = TestClock()
        val secure = DeleteFailureSecure()
        val client = client(clock, secure) { request ->
            if (request.path().endsWith("/verify")) respond(tokens(clock), headers = JSON)
            else respond("""{"status":401,"code":"SESSION_EXPIRED"}""", HttpStatusCode.Unauthorized, PROBLEM)
        }
        try {
            client.verifyLogin("challenge-1", "123456")
            secure.failClearing = true
            val failure = assertFailsWith<AppFailure> { client.request("/me") }
            assertEquals(FailureKind.EXPIRED_SESSION, failure.kind)
            assertTrue(failure.message.contains("Não foi possível remover a sessão protegida"))
            assertNull(client.currentSession())
            assertNull(client.restoreSession())
            assertTrue(secure.memory.values.keys.any { it.endsWith(".head") })
        } finally {
            secure.failClearing = false
            try { client.logout() } finally { client.close() }
        }
    }

    @Test fun logoutAttemptsRevocationWhenLocalCleanupFailsAndReportsBothOutcomesHonestly() = runTest {
        for (remoteFails in listOf(false, true)) {
            val clock = TestClock()
            val secure = DeleteFailureSecure()
            var revocations = 0
            val client = client(clock, secure) { request ->
                if (request.path().endsWith("/verify")) respond(tokens(clock), headers = JSON)
                else {
                    assertEquals("/auth/logout", request.path())
                    assertEquals("Bearer access-old", request.headers[HttpHeaders.Authorization])
                    revocations++
                    if (remoteFails) throw IOException("Synthetic remote outage")
                    respond("", HttpStatusCode.NoContent)
                }
            }
            try {
                client.verifyLogin("challenge-1", "123456")
                secure.failClearing = true
                val failure = assertFailsWith<AppFailure> { client.logout() }
                assertEquals(FailureKind.UNAVAILABLE, failure.kind)
                assertEquals(1, revocations)
                assertNull(client.currentSession())
                assertTrue(secure.memory.values.keys.any { it.endsWith(".head") })
                assertFalse(failure.message.contains("Sessão removida deste dispositivo"))
                if (remoteFails) assertTrue(failure.message.contains("nem confirmar a revogação remota"))
                else assertTrue(failure.message.contains("revogada no servidor, mas não foi possível remover"))
            } finally {
                secure.failClearing = false
                try { client.logout() } finally { client.close() }
            }
        }
    }

    private class DeleteFailureSecure : SecureStorage {
        val memory = MemorySecure()
        var failClearing = false
        override suspend fun read(key: String): String? = memory.read(key)
        override suspend fun write(key: String, value: String?) {
    private fun TestScope.client(
        clock: TestClock, secure: SecureStorage = MemorySecure(), baseUrl: String = BASE,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): ApiSessionClient {
        val engine = MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler(handler)
        })
        return ApiSessionClient(HttpTransport(engine, baseUrl), Brands.smartLocker, baseUrl, secure, clock)
    }

    private fun HttpRequestData.path() = url.encodedPath.removePrefix("/v1")
    private suspend fun HttpRequestData.jsonBody() = ApiJson.parseToJsonElement(body.toByteArray().decodeToString()).jsonObject
    private fun instant(value: Long) = Instant.fromEpochMilliseconds(value).toString()
    private fun tokens(
        clock: TestClock, access: String = "access-old", refresh: String = "refresh-old", accessIn: Long = 60_000,
        user: String = "ana", brand: String = "smartlocker", session: String = "session-1",
    ) = ApiJson.encodeToString(ApiSessionTokens("Bearer", access, instant(clock.time + accessIn), refresh,
        instant(clock.time + 3_600_000), user, brand, session, listOf("profile:read")))

    private fun challenge(clock: TestClock) = ApiJson.encodeToString(ApiChallenge("challenge-1",
        instant(clock.time + 300_000), instant(clock.time + 30_000), "sms", "+55 ** *****-4321", 6, "login"))

    private fun configuration(): String {
        val channel = ApiChannelCapability(true)
        val capabilities = ApiCapabilities(ApiFeatures(true, true, true, true, true, true),
            ApiChannels(channel, channel, channel, channel, channel))
        return ApiJson.encodeToString(ApiBrandConfiguration("smartlocker", "SmartLocker", capabilities, null, null, null))
    }

    private companion object {
        const val BASE = "https://api.example.test/v1"
        val JSON = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        val PROBLEM = headersOf(HttpHeaders.ContentType, "application/problem+json")
    }
}
