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
                engineFactory = { fixture.engine })
            val controller = runtime.state.value.controller
            try {
                setContent { SmartLockerApp(runtime) }
                waitUntil(10_000) { controller.state.value.initialized && !controller.state.value.busy }
                onNodeWithText("Preencher dados de demonstração").assertDoesNotExist()
                onNodeWithText("Celular").performScrollTo().performTextInput("11987654321")
                onNodeWithText("CPF").performScrollTo().performTextInput("52998224725")
                onNodeWithText("Receber código por SMS").performScrollTo().performClick()
                waitUntil(10_000) { controller.state.value.challenge != null && !controller.state.value.busy }
                onNodeWithText("Código de 6 dígitos").performScrollTo().performTextInput("123456")
                onNodeWithText("Confirmar código").performScrollTo().performClick()
                waitUntil(15_000) { controller.state.value.profile != null && !controller.state.value.busy }
                assertEquals("user-api", controller.state.value.profile!!.id)
                assertEquals("member-api", controller.state.value.membershipId)
                onNodeWithText("Simular retirada física").assertDoesNotExist()
                onNodeWithText("Copiar código").performScrollTo().performClick()
                assertEquals("004321", platform.copied)
                onNodeWithText("Fechar").performClick()
                onNodeWithContentDescription("Ver detalhes").performScrollTo().performClick()
