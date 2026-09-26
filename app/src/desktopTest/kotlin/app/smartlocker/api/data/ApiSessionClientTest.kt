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
