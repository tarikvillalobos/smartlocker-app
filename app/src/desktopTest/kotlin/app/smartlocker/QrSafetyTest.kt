package app.smartlocker

import app.smartlocker.design.qrMatrixOrNull
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import kotlin.test.*

class QrSafetyTest {
    @Test fun utf8ByteCapacityIsHandledEvenInsideTheCharacterLimit() {
        val ascii = "a".repeat(2048)
        val unicode = "á".repeat(2048)
        assertEquals(ascii.length, unicode.length)
        assertNotNull(qrMatrixOrNull(ascii))
        assertNull(qrMatrixOrNull(unicode))
        assertNull(qrMatrixOrNull("🔒".repeat(1024)))
    }

    @Test fun supportedUnicodePayloadRoundTripsWithoutChangingTheAuthorizedContent() {
        val payload = "https://locker.example.test/retirada/área-🔒"
        val matrix = assertNotNull(qrMatrixOrNull(payload))
