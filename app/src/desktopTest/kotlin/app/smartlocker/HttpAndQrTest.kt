package app.smartlocker

import app.smartlocker.design.qrMatrix
import app.smartlocker.shared.data.HttpTransport
import app.smartlocker.shared.data.UnconfiguredRepository
import app.smartlocker.shared.domain.*
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class HttpAndQrTest {
    @Test fun qrRoundTripsAuthorizedPayloadWithQuietZone() {
        val payload = "SMARTLOCKER-DEMO|home|demo-0|123456"
        val matrix = qrMatrix(payload)
        val size = (matrix.size + 8) * 8
        val pixels = IntArray(size * size) { -1 }
        matrix.forEachIndexed { row, values -> values.forEachIndexed { col, dark ->
            if (dark) for (dy in 0..7) for (dx in 0..7) {
                pixels[((row + 4) * 8 + dy) * size + (col + 4) * 8 + dx] = 0xFF000000.toInt()
            }
        } }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(size, size, pixels)))
        assertEquals(payload, MultiFormatReader().decode(bitmap).text)
    }
    @Test fun transportUsesOnlyDocumentedRequestAndDoesNotRetryMutations() = runTest {
        var calls = 0
        val engine = MockEngine { request ->
            calls++
            assertEquals("/fixture", request.url.encodedPath)
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("contract-defined", request.headers["X-Fixture-Auth"])
            respond("sensitive response that must not reach UI", HttpStatusCode.Conflict)
        }
        val transport = HttpTransport(engine, "https://api.example.test")
        val error = assertFailsWith<AppFailure> {
            transport.execute("/fixture", HttpMethod.Post, mapOf("X-Fixture-Auth" to "contract-defined"), "{}")
        }
        assertEquals(FailureKind.CONFLICT, error.kind)
        assertFalse(error.message.contains("sensitive"))
        assertEquals(1, calls)
        transport.close()
    }
    @Test fun mapsSessionAndAccessErrorsAndPreservesCancellation() = runTest {
        for ((status, kind) in listOf(401 to FailureKind.EXPIRED_SESSION, 403 to FailureKind.DENIED,
            429 to FailureKind.UNAVAILABLE, 503 to FailureKind.UNAVAILABLE)) {
            val transport = HttpTransport(MockEngine { respond("{}", HttpStatusCode.fromValue(status)) }, "https://api.example.test")
            assertEquals(kind, assertFailsWith<AppFailure> { transport.execute("/fixture", HttpMethod.Get) }.kind)
            transport.close()
        }
        val transport = HttpTransport(MockEngine { delay(10_000); respond("{}") }, "https://api.example.test")
        assertFailsWith<CancellationException> { withTimeout(50) { transport.execute("/fixture", HttpMethod.Get) } }
        transport.close()
    }
    @Test fun productionCannotSilentlyUseDemo() = runTest {
        assertEquals(FailureKind.MISSING_CONTRACT,
            assertFailsWith<AppFailure> { UnconfiguredRepository().requestLogin(demoLogin) }.kind)
