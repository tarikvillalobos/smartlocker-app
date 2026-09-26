package app.smartlocker

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.toAwtImage
import app.smartlocker.config.*
import app.smartlocker.platform.PlatformServices
import app.smartlocker.shared.presentation.Route
import java.io.File
import javax.imageio.ImageIO
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestPlatform : PlatformServices {
    override val local = MemoryStorage()
    override val secure = MemorySecure()
    var copied: String? = null
