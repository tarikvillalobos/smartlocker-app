package app.smartlocker

import app.smartlocker.shared.data.HttpTransport
import app.smartlocker.shared.domain.AppFailure
import app.smartlocker.shared.domain.FailureKind
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class HttpTransportLimitTest {
    @Test fun smallUtf8ResponsesRemainUnchanged() = runTest {
        val expected = "{\"message\":\"Encomenda disponível · 🔒\"}"
        val transport = HttpTransport(MockEngine {
            respond(expected.encodeToByteArray(), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }, "https://api.example.test")
        try { assertEquals(expected, transport.execute("/fixture", HttpMethod.Get)) }
        finally { transport.close() }
    }

    @Test fun responseAtTheExactByteLimitIsAccepted() = runTest {
        val expected = "á".repeat(1_000_000)
        assertEquals(2_000_000, expected.encodeToByteArray().size)
        val transport = HttpTransport(MockEngine { respond(expected.encodeToByteArray()) }, "https://api.example.test")
        try { assertEquals(expected, transport.execute("/fixture", HttpMethod.Get)) }
        finally { transport.close() }
    }

    @Test fun multibyteResponsesOverTheLimitAreRejectedWithoutRetryForAnyStatus() = runTest {
        val bytes = "á".repeat(1_000_001).encodeToByteArray()
        for (status in listOf(HttpStatusCode.OK, HttpStatusCode.ServiceUnavailable)) {
            var calls = 0
            val transport = HttpTransport(MockEngine {
                calls++
                respond(bytes, status)
            }, "https://api.example.test")
            try {
