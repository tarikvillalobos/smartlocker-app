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
            assertEquals(listOf("Bearer access-new", "Bearer access-new"), authorizations)
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
