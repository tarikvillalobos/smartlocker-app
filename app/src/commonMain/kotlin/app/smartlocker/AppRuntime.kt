package app.smartlocker

import app.smartlocker.config.*
import app.smartlocker.api.data.*
import app.smartlocker.shared.data.HttpTransport
import app.smartlocker.demo.data.DemoRepository
import app.smartlocker.platform.PlatformServices
import app.smartlocker.shared.data.UnconfiguredRepository
import app.smartlocker.shared.domain.AppClock
import app.smartlocker.shared.presentation.AppController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

data class RuntimeState(val configuration: AppConfiguration, val controller: AppController)

/** Composition root: explicitly chooses one implementation; production has no demo fallback. */
class AppRuntime(
    val platform: PlatformServices, initial: AppConfiguration? = null,
    private val engineFactory: () -> io.ktor.client.engine.HttpClientEngine = ::createApiEngine,
) {
    private val clock = AppClock { Clock.System.now().toEpochMilliseconds() }
    private val endpoint = initial?.apiBaseUrl ?: platform.apiBaseUrl
    private val initialConfig = (initial ?: AppConfiguration(
        Brands.all.find { it.id == platform.local.read("brand") } ?: Brands.smartLocker,
        platform.local.read("environment")?.let { runCatching { Environment.valueOf(it) }.getOrNull() } ?: Environment.PRODUCTION,
    )).let { config -> config.copy(apiBaseUrl = endpoint,
        brand = config.brand.copy(termsVersion = platform.termsVersion ?: config.brand.termsVersion)) }
    private val mutable = MutableStateFlow(create(initialConfig))
    val state = mutable.asStateFlow()

    private fun create(configuration: AppConfiguration): RuntimeState {
        val baseUrl = configuredApiEndpoint(configuration.apiBaseUrl)
        val repository = when (configuration.environment) {
            Environment.DEMO -> DemoRepository(platform.local, platform.secure, configuration.brand, clock)
            Environment.PRODUCTION -> if (baseUrl == null) UnconfiguredRepository() else {
                val transport = HttpTransport(engineFactory(), baseUrl)
                ApiLockerRepository(ApiSessionClient(transport, configuration.brand, baseUrl, platform.secure, clock))
            }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        return RuntimeState(configuration, AppController(configuration, repository, clock, scope))
    }

    fun configure(brand: Brand = state.value.configuration.brand, environment: Environment = state.value.configuration.environment) {
        if (state.value.controller.state.value.session != null || state.value.controller.state.value.busy) return
        state.value.controller.close()
        platform.local.write("brand", brand.id)
        platform.local.write("environment", environment.name)
        mutable.value = create(AppConfiguration(brand.copy(termsVersion = platform.termsVersion ?: brand.termsVersion), environment, endpoint))
    }

    fun close() = state.value.controller.close()
}
