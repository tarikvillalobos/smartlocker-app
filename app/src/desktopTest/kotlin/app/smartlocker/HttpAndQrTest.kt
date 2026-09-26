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
