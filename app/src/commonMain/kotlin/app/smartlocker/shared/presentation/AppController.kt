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
                val now = clock.now()
                mutable.update { it.copy(now = now) }
                if (state.value.session?.expiresAt?.let { now >= it } == true) {
                    handle(AppFailure(FailureKind.EXPIRED_SESSION, "Sua sessão expirou. Entre novamente."))
                }
            }
        }
        execute { generation ->
            val session = repository.restoreSession()
            if (generation != epoch) return@execute
            mutable.update { it.copy(session = session, initialized = true) }
            if (session != null) load()
        }
    }

    private fun execute(block: suspend (Int) -> Unit) {
        val generation = epoch
        scope.launch {
            if (generation != epoch) return@launch
            mutable.update { it.copy(busy = true, error = null) }
            try {
                block(generation)
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
                selectedId = old.selectedId, filter = old.filter, membershipId = old.membershipId,
                now = clock.now(), error = error.message)
        } else if (error is AppFailure && error.kind == FailureKind.DENIED) {
            mutable.update { it.copy(error = error.message, parcels = emptyList(), pending = emptyList(), recent = emptyList(),
                selected = null, selectedId = null, credential = null, notices = emptyList(),
                issues = emptyList(), residents = emptyList(), statistics = null, nextCursor = null, stale = false) }
        } else {
            mutable.update { it.copy(error = (error as? AppFailure)?.message ?: "Não foi possível concluir. Tente novamente.",
                stale = it.profile != null, credential = null) }
        }
    }

    fun login(request: LoginRequest) = execute { generation ->
        lastLogin = request
        val challenge = repository.requestLogin(request)
        if (generation != epoch) return@execute
        mutable.update { it.copy(challenge = challenge) }
    }
    fun resend() { lastLogin?.let(::login) }
    fun correctContact() {
        epoch++
        readJob?.cancel()
        mutable.update { it.copy(challenge = null, error = null, busy = false) }
    }
    fun verify(code: String) = execute { generation ->
        val challenge = state.value.challenge ?: return@execute
        val session = repository.verifyLogin(challenge.id, code)
        if (generation != epoch) return@execute
        mutable.update { it.copy(session = session, challenge = null,
            route = if (previousUser == null || previousUser == session.userId) it.route else Route.HOME,
            selectedId = if (previousUser == null || previousUser == session.userId) it.selectedId else null,
            membershipId = if (previousUser == null || previousUser == session.userId) it.membershipId else "",
            filter = if (previousUser == null || previousUser == session.userId) it.filter else ParcelFilter.ALL) }
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
        val history = LoadHistory(repository)(location, context.filter)
        val page = history.page
        val pending = repository.parcels(location, ParcelFilter.WAITING, null).items
        val statistics = history.statistics
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
            try { credential = LoadPickupCredential(repository, clock)(location, selected) }
            catch (error: AppFailure) {
                if (error.kind in setOf(FailureKind.EXPIRED_CODE, FailureKind.UNAVAILABLE)) credentialMessage = error.message
                else throw error
            }
        }
        if (generation != epoch) return
        mutable.update { it.copy(profile = profile, membershipId = location, parcels = page.items,
            pending = pending, nextCursor = page.nextCursor, statistics = statistics, notices = notices,
            issues = issues, selectedId = selected?.id, selected = selected, credential = credential,
            credentialMessage = credentialMessage, stale = false, now = clock.now(), lastUpdated = clock.now()) }
    }

    fun select(id: String, openDetail: Boolean = true) {
        mutable.update { it.copy(selectedId = id, selected = null, credential = null,
            route = if (openDetail) Route.DETAIL else it.route) }
        refresh()
    }
    fun navigate(route: Route) {
        val features = configuration.brand.features
        if ((route == Route.RESIDENTS && !features.residents) ||
            (route == Route.ISSUES && !features.issues) ||
            (route == Route.CONTACT && !features.contactEditing) ||
            (route == Route.DEMO && configuration.environment != Environment.DEMO)) {
            feedback("Recurso não habilitado para esta marca.")
            return
        }
        mutable.update { it.copy(route = route, error = null) }
        if (route == Route.RESIDENTS) execute {
            val generation = epoch
            val residents = repository.recipients(state.value.membershipId)
            if (generation == epoch) mutable.update { it.copy(residents = residents) }
        }
    }
    fun filter(value: ParcelFilter) {
        mutable.update { it.copy(filter = value, parcels = emptyList(), nextCursor = null) }
        refresh()
    }
    fun membership(id: String) {
        if (state.value.profile?.memberships?.none { it.id == id } != false) return
        epoch++
        readJob?.cancel()
        mutable.update { it.copy(membershipId = id, selectedId = null, selected = null, credential = null,
            parcels = emptyList(), pending = emptyList(), notices = emptyList(), issues = emptyList(),
            statistics = null, nextCursor = null, residents = emptyList(), route = Route.HOME) }
        refresh()
    }
    fun more() = execute {
        val context = state.value
        val generation = epoch
        val cursor = context.nextCursor ?: return@execute
        val page = repository.parcels(context.membershipId, context.filter, cursor)
        if (generation == epoch) mutable.update {
            it.copy(parcels = (it.parcels + page.items).distinctBy(Parcel::id), nextCursor = page.nextCursor)
        }
    }

    fun markCollected() = mutateSelected {
        repository.markCollected(it.membershipId, it.selectedId!!)
        "Retirada informada por você. Sem confirmação física do armário."
    }
    fun undo() = mutateSelected {
        repository.undoManual(it.membershipId, it.selectedId!!)
        "Marcação desfeita. O código anterior continua revogado."
    }
    private fun mutateSelected(action: suspend (AppState) -> String) = execute {
        val context = state.value
        if (context.selectedId == null) return@execute
        val generation = epoch
        val message = action(context)
        if (generation == epoch) {
            load(generation)
            if (generation == epoch) feedback(message)
        }
    }
    fun preferences(value: CommunicationPreferences) = execute {
        val generation = epoch
        val profile = repository.updatePreferences(value)
        if (generation == epoch) mutable.update { it.copy(profile = profile, feedback = "Preferências salvas.") }
    }
    fun contact(value: String, channel: LoginChannel) = execute { generation ->
        val challenge = repository.requestContactChange(value, channel)
        if (generation != epoch) return@execute
        mutable.update { it.copy(contactChallenge = challenge) }
    }
    fun verifyContact(code: String) = execute { generation ->
        val challenge = state.value.contactChallenge ?: return@execute
        val profile = repository.verifyContactChange(challenge.id, code)
        if (generation != epoch) return@execute
        mutable.update { it.copy(profile = profile, contactChallenge = null, route = Route.PROFILE,
            feedback = "Contato verificado e atualizado.") }
    }
    fun report(message: String) = mutateSelected {
        val issue = repository.reportIssue(it.membershipId, it.selectedId!!, message)
        "Solicitação ${issue.id} recebida. Acompanhe nesta tela."
    }
    fun notice(value: DeliveryNotice) = execute { generation ->
        repository.markNoticeRead(state.value.membershipId, value.id)
        if (generation != epoch) return@execute
        select(value.parcelId)
    }
    fun demoScenario(value: DemoScenario) = execute { generation ->
        (repository as? DemoControls)?.scenario(value)
        if (generation != epoch) return@execute
        mutable.update { it.copy(selectedId = null, selected = null, credential = null) }
        load()
    }
    fun deposit() = execute { generation ->
        (repository as? DemoControls)?.deposit(state.value.membershipId)
        if (generation != epoch) return@execute
        load()
        if (generation == epoch) feedback("Depósito fictício criado. Veja a nova encomenda e o aviso.")
    }
    fun physicalPickup() = mutateSelected {
        (repository as? DemoControls)?.physicalPickup(it.membershipId, it.selectedId!!)
        "Retirada física simulada. Nenhum hardware foi acionado."
    }
    fun logout() {
        epoch++
        readJob?.cancel()
        previousUser = null
        lastLogin = null
        mutable.value = AppState(initialized = true, now = clock.now())
        execute {
            try { repository.logout() }
            finally {
                previousUser = null
                lastLogin = null
                mutable.value = AppState(initialized = true, now = clock.now())
            }
        }
    }
    fun feedback(message: String?) { mutable.update { it.copy(feedback = message) } }
    fun close() = scope.cancel()
}
