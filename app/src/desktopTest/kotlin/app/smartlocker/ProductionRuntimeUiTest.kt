package app.smartlocker

import androidx.compose.ui.test.*
import app.smartlocker.config.*
import app.smartlocker.parcels.domain.ParcelStatus
import app.smartlocker.shared.domain.DemoScenario
import app.smartlocker.shared.presentation.Route
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class ProductionRuntimeUiTest {
    @Test fun productionLoginPickupAndHistoryUseHttpAndKeepProductionAfterFailure() =
        runDesktopComposeUiTest(width = 390, height = 1100) {
            val fixture = ProductionApiFixture()
            val platform = TestPlatform()
            val runtime = AppRuntime(platform,
                AppConfiguration(Brands.smartLocker, Environment.PRODUCTION, "https://api.example.test/v1"),
