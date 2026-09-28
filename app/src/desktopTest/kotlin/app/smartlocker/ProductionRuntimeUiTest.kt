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
    @Test fun invitedUserCompletesFirstAccessWithConfiguredTerms() =
        runDesktopComposeUiTest(width = 390, height = 1100) {
            val fixture = ProductionApiFixture(listOf("invitation"))
            val runtime = AppRuntime(TestPlatform(), AppConfiguration(Brands.smartLocker.copy(termsVersion = "2026-09"),
                Environment.PRODUCTION, "https://api.example.test/v1"), engineFactory = { fixture.engine })
            val controller = runtime.state.value.controller
            try {
                setContent { SmartLockerApp(runtime) }
                waitUntil(timeoutMillis = 10_000) { controller.state.value.initialized && !controller.state.value.busy }
                onNodeWithText("Tenho um convite de primeiro acesso").performScrollTo().performClick()
                onNodeWithText("Código do convite").performScrollTo().performTextInput("ABC123")
                onNodeWithText("Conferir convite").performScrollTo().performClick()
                waitUntil(timeoutMillis = 10_000) { controller.state.value.invitationPreview != null && !controller.state.value.busy }
            } finally { runOnIdle { runtime.close() } }
        }

    @Test fun passwordOnlyBrandSignsInThroughDocumentedApi() =
        runDesktopComposeUiTest(width = 390, height = 900) {
            val fixture = ProductionApiFixture(listOf("password"))
            val runtime = AppRuntime(TestPlatform(),
                AppConfiguration(Brands.smartLocker, Environment.PRODUCTION, "https://api.example.test/v1"),
                engineFactory = { fixture.engine })
            val controller = runtime.state.value.controller
            try {
                setContent { SmartLockerApp(runtime) }
                waitUntil(timeoutMillis = 10_000) { controller.state.value.initialized && !controller.state.value.busy }
                onNodeWithText("Receber código por SMS").assertDoesNotExist()
                onNodeWithText("E-mail, celular ou CPF").performScrollTo().performTextInput("ana@example.test")
                onNodeWithText("Senha").performScrollTo().performTextInput("private-password")
                onNodeWithText("Entrar").performScrollTo().performClick()
                waitUntil(timeoutMillis = 15_000) { controller.state.value.profile != null && !controller.state.value.busy }
                assertEquals("Ana API", controller.state.value.profile?.name)
                assertTrue(fixture.requests.any { it.url.encodedPath.endsWith("/auth/password/login") })
            } finally { runOnIdle { runtime.close() } }
        }

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
                waitUntil(timeoutMillis = 10_000) { controller.state.value.initialized && !controller.state.value.busy }
                onAllNodesWithText("Marca da API").onFirst().assertExists()
                onNodeWithText("Preencher dados de demonstração").assertDoesNotExist()
                onNodeWithText("Celular").performScrollTo().performTextInput("11987654321")
                onNodeWithText("CPF").performScrollTo().performTextInput("52998224725")
                onNodeWithText("Receber código por SMS").performScrollTo().performClick()
                waitUntil(timeoutMillis = 10_000) { controller.state.value.challenge != null && !controller.state.value.busy }
                onNodeWithText("Código de 6 dígitos").performScrollTo().performTextInput("123456")
                onNodeWithText("Confirmar código").performScrollTo().performClick()
                waitUntil(timeoutMillis = 15_000) { controller.state.value.profile != null && !controller.state.value.busy }
                assertEquals("user-api", controller.state.value.profile!!.id)
                assertEquals("Ana API", controller.state.value.profile!!.name)
                assertEquals("member-api", controller.state.value.membershipId)
                onNodeWithText("Simular retirada física").assertDoesNotExist()
                onNodeWithText("Copiar código").performScrollTo().performClick()
                assertEquals("004321", platform.copied)
                onNodeWithText("Fechar").performClick()
                onNodeWithContentDescription("Ver detalhes").performScrollTo().performClick()
                waitUntil(timeoutMillis = 10_000) { controller.state.value.route == Route.DETAIL && !controller.state.value.busy }
                onNodeWithText("Já retirei a encomenda").performScrollTo().performClick()
                onNodeWithText("Sim, retirei").performClick()
                waitUntil(timeoutMillis = 10_000) { controller.state.value.selected?.status == ParcelStatus.MANUAL && !controller.state.value.busy }
                assertNull(controller.state.value.credential)
                onAllNodesWithText("Retirada informada").onFirst().assertExists()
                onAllNodesWithText("Histórico").onFirst().performClick()
                onNodeWithText("Retiradas").performScrollTo().performClick()
                waitUntil(timeoutMillis = 10_000) { controller.state.value.route == Route.HISTORY && !controller.state.value.busy }
                assertEquals(listOf(ParcelStatus.MANUAL), controller.state.value.parcels.map { it.status })
                assertTrue(fixture.requests.any { it.url.parameters["status"] == "collected" })
                fixture.unavailable = true
                onNodeWithContentDescription("Atualizar encomendas").performClick()
                waitUntil(timeoutMillis = 10_000) { controller.state.value.error != null && !controller.state.value.busy }
                assertNotNull(controller.state.value.session)
                assertTrue(controller.state.value.stale)
                assertNull(controller.state.value.credential)
                runOnIdle { controller.navigate(Route.DEMO) }
                assertEquals(Route.HISTORY, controller.state.value.route)
                val requestsBeforeDemoActions = fixture.requests.size
                runOnIdle { controller.deposit(); controller.physicalPickup(); controller.demoScenario(DemoScenario.NORMAL) }
                waitUntil(timeoutMillis = 10_000) { !controller.state.value.busy }
                assertEquals(requestsBeforeDemoActions, fixture.requests.size)
                assertEquals(Environment.PRODUCTION, runtime.state.value.configuration.environment)
                onNodeWithText("Demonstração · dados fictícios").assertDoesNotExist()
                assertEquals(1, fixture.requests.count { it.method == HttpMethod.Post && it.url.encodedPath.endsWith("/manual-pickup") })
                assertTrue(fixture.requests.filter { it.url.encodedPath.startsWith("/v1/memberships/") }
                    .all { it.headers[HttpHeaders.Authorization] == "Bearer access-api" && it.headers["X-Brand-Id"] == "smartlocker" })
            } finally { runOnIdle { runtime.close() } }
        }
}
