package app.smartlocker

import app.smartlocker.config.*
import app.smartlocker.demo.data.DemoRepository
import app.smartlocker.platform.PlatformServices
import app.smartlocker.shared.data.UnconfiguredRepository
import app.smartlocker.shared.domain.AppClock
import app.smartlocker.shared.presentation.AppController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

data class RuntimeState(val configuration: AppConfiguration, val controller: AppController)

/** Composition root: explicitly chooses one implementation; production has no demo fallback. */
class AppRuntime(val platform: PlatformServices, initial: AppConfiguration? = null) {
    private val clock = AppClock { Clock.System.now().toEpochMilliseconds() }
    private val initialConfig = initial ?: AppConfiguration(
        Brands.all.find { it.id == platform.local.read("brand") } ?: Brands.smartLocker,
