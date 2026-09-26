package app.smartlocker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.smartlocker.config.*
import app.smartlocker.parcels.domain.ParcelFilter
import app.smartlocker.shared.domain.DemoScenario
import app.smartlocker.shared.presentation.Route
import java.io.File
import javax.imageio.ImageIO
import org.junit.Test
import kotlin.test.*

