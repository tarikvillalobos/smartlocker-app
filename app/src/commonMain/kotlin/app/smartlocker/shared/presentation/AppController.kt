package app.smartlocker.shared.presentation

import app.smartlocker.auth.domain.*
import app.smartlocker.config.*
import app.smartlocker.parcels.domain.*
import app.smartlocker.profile.domain.*
import app.smartlocker.shared.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class AppController(
    val configuration: AppConfiguration,
    private val repository: LockerRepository,
    private val clock: AppClock,
    private val scope: CoroutineScope,
) {
    private val mutable = MutableStateFlow(AppState(now = clock.now()))
    val state = mutable.asStateFlow()
    private var epoch = 0
    private var readJob: Job? = null
    private var previousUser: String? = null
    private var lastLogin: LoginRequest? = null

    init {
        scope.launch {
            while (isActive) {
                delay(1_000)
                mutable.update { it.copy(now = clock.now()) }
            }
        }
        execute {
            val session = repository.restoreSession()
            mutable.update { it.copy(session = session, initialized = true) }
            if (session != null) load()
        }
    }

    private fun execute(block: suspend () -> Unit) {
        val generation = epoch
        scope.launch {
            mutable.update { it.copy(busy = true, error = null) }
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation == epoch) handle(error)
            } finally {
                if (generation == epoch) mutable.update { it.copy(busy = false, initialized = true) }
            }
        }
    }

    private fun handle(error: Exception) {
        if (error is AppFailure && error.kind == FailureKind.EXPIRED_SESSION) {
            previousUser = state.value.session?.userId
            epoch++
            val old = state.value
            mutable.value = AppState(initialized = true, route = old.route,
                selectedId = old.selectedId, filter = old.filter, now = clock.now(), error = error.message)
