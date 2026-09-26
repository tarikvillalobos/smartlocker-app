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
        val scale = 8
        val size = (matrix.size + 8) * scale
        val pixels = IntArray(size * size) { -1 }
        matrix.forEachIndexed { row, values -> values.forEachIndexed { col, dark ->
            if (dark) for (dy in 0 until scale) for (dx in 0 until scale) {
                pixels[((row + 4) * scale + dy) * size + (col + 4) * scale + dx] = 0xFF000000.toInt()
            }
        } }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(size, size, pixels)))
        val decoded = MultiFormatReader().decode(bitmap, mapOf(DecodeHintType.CHARACTER_SET to "UTF-8"))
        assertEquals(payload, decoded.text)
    }
}
