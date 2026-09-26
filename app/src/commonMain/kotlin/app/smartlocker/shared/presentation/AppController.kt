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
        } else {
            mutable.update { it.copy(error = (error as? AppFailure)?.message ?: "Não foi possível concluir. Tente novamente.",
                stale = it.profile != null, credential = null) }
        }
    }

    fun login(request: LoginRequest) = execute {
        lastLogin = request
        val challenge = repository.requestLogin(request)
        mutable.update { it.copy(challenge = challenge) }
    }
    fun resend() { lastLogin?.let(::login) }
    fun correctContact() { mutable.update { it.copy(challenge = null, error = null) } }
    fun verify(code: String) = execute {
        val challenge = state.value.challenge ?: return@execute
        val session = repository.verifyLogin(challenge.id, code)
        mutable.update { it.copy(session = session, challenge = null,
            route = if (previousUser == null || previousUser == session.userId) it.route else Route.HOME,
            selectedId = if (previousUser == null || previousUser == session.userId) it.selectedId else null) }
        load()
    }

    fun refresh() {
        readJob?.cancel()
        val generation = ++epoch
        readJob = scope.launch {
            mutable.update { it.copy(busy = true, credential = null, error = null) }
            try { load(generation) }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { if (generation == epoch) handle(error) }
            finally { if (generation == epoch) mutable.update { it.copy(busy = false) } }
        }
    }

    private suspend fun load(generation: Int = epoch) {
        val context = state.value
        val profile = repository.profile()
        val location = profile.memberships.find { it.id == context.membershipId }?.id
            ?: profile.memberships.firstOrNull()?.id
            ?: throw AppFailure(FailureKind.DENIED, "Nenhum local autorizado para esta conta.")
        val page = repository.parcels(location, context.filter, null)
        val pending = repository.parcels(location, ParcelFilter.WAITING, null).items
        val statistics = repository.statistics(location)
        val notices = repository.notifications(location)
        val issues = if (configuration.brand.features.issues) repository.issues(location) else emptyList()
        val selectedId = context.selectedId ?: pending.firstOrNull()?.id
        val selected = selectedId?.let { id ->
            try { repository.parcel(location, id) }
            catch (error: AppFailure) { if (error.kind == FailureKind.DENIED) null else throw error }
        }
        var credential: PickupCredential? = null
        var credentialMessage: String? = null
        if (selected?.status == ParcelStatus.WAITING) {
            try { credential = repository.credential(location, selected.id) }
            catch (error: AppFailure) {
                if (error.kind in setOf(FailureKind.EXPIRED_CODE, FailureKind.UNAVAILABLE)) credentialMessage = error.message
                else throw error
            }
        }
        if (generation != epoch) return
