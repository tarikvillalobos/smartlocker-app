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
    private var actionJob: Job? = null
    private var previousUser: String? = null
    private var lastLogin: LoginRequest? = null
    val brand: Brand get() = state.value.remoteBrand ?: configuration.brand
    val features: Features get() = state.value.features(brand)

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
            val remoteBrand = repository.brandConfiguration()
            if (generation != epoch) return@execute
            mutable.update { it.copy(remoteBrand = remoteBrand) }
            val session = repository.restoreSession()
            if (generation != epoch) return@execute
            mutable.update { it.copy(session = session, initialized = true) }
            if (session != null) load()
        }
    }

    private fun execute(block: suspend (Int) -> Unit) {
        if (actionJob?.isActive == true || readJob?.isActive == true) return
        val generation = epoch
        mutable.update { it.copy(busy = true, error = null) }
        val job = scope.launch(start = CoroutineStart.LAZY) {
            if (generation != epoch) return@launch
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
        actionJob = job
        job.start()
    }

    private fun handle(error: Exception) {
        (error as? AppFailure)?.retryAt?.let { retryAt ->
            mutable.update { it.copy(challenge = it.challenge?.copy(resendAt = maxOf(it.challenge.resendAt, retryAt)),
                contactChallenge = it.contactChallenge?.copy(resendAt = maxOf(it.contactChallenge.resendAt, retryAt))) }
        }
        if (error is AppFailure && error.kind == FailureKind.EXPIRED_SESSION) {
            previousUser = state.value.session?.userId
            epoch++
            readJob?.cancel()
            actionJob?.cancel()
            val old = state.value
            mutable.value = AppState(initialized = true, remoteBrand = old.remoteBrand, route = old.route,
                selectedId = old.selectedId, filter = old.filter, membershipId = old.membershipId,
                now = clock.now(), error = error.message)
        } else if (error is AppFailure && error.kind == FailureKind.DENIED) {
            mutable.update { it.copy(error = error.message, parcels = emptyList(), pending = emptyList(), recent = emptyList(),
                selected = null, selectedId = null, credential = null, notices = emptyList(),
                issues = emptyList(), residents = emptyList(), statistics = null, nextCursor = null,
                noticeCursor = null, serverUnreadCount = null, issueCursor = null, stale = false) }
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
    fun resend() = execute { generation ->
        val request = lastLogin ?: return@execute
        val challenge = state.value.challenge ?: return@execute
        val updated = repository.resendLogin(challenge.id, request)
        if (generation == epoch) mutable.update { it.copy(challenge = updated) }
    }
    fun correctContact() {
        epoch++
        readJob?.cancel()
        actionJob?.cancel()
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

    fun refresh() { if (actionJob?.isActive != true) refreshData(detailOnly = false) }
    private fun refreshData(detailOnly: Boolean) {
        readJob?.cancel()
        val generation = ++epoch
        mutable.update { it.copy(busy = true, credential = null, error = null) }
        readJob = scope.launch {
            try { if (detailOnly) loadDetail(generation) else load(generation) }
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
        val recent = if (context.filter == ParcelFilter.ALL) page.items.take(4)
            else repository.parcels(location, ParcelFilter.ALL, null).items.take(4)
        val statistics = history.statistics
        val notices = repository.noticePage(location)
        val membership = profile.memberships.first { it.id == location }
        val issues = if (brand.features.issues && membership.features.issues) repository.issuePage(location)
            else app.smartlocker.profile.domain.IssuePage(emptyList(), null)
        val selectedId = context.selectedId ?: pending.firstOrNull()?.id
        val detail = readDetail(location, selectedId)
        if (generation != epoch) return
        mutable.update { normalizeCapabilities(it.copy(profile = profile, membershipId = location, parcels = page.items,
            pending = pending, recent = recent, nextCursor = page.nextCursor, statistics = statistics, notices = notices.items,
            noticeCursor = notices.nextCursor, serverUnreadCount = notices.unreadCount,
            issues = issues.items, issueCursor = issues.nextCursor,
            residents = if (brand.features.residents && membership.features.residents && context.membershipId == location) it.residents else emptyList(),
            selectedId = detail.parcel?.id, selected = detail.parcel, credential = detail.credential,
            credentialMessage = detail.message, stale = false, now = clock.now(), lastUpdated = clock.now())) }
    }

    private fun normalizeCapabilities(value: AppState): AppState {
        val allowed = value.features(value.remoteBrand ?: configuration.brand)
        val blocked = (value.route == Route.RESIDENTS && !allowed.residents) ||
            (value.route == Route.ISSUES && !allowed.issues) || (value.route == Route.CONTACT && !allowed.contactEditing)
        return value.copy(route = if (blocked) Route.PROFILE else value.route,
            residents = if (allowed.residents) value.residents else emptyList(),
            issues = if (allowed.issues) value.issues else emptyList(),
            contactChallenge = if (allowed.contactEditing) value.contactChallenge else null)
    }

    private data class Detail(val parcel: Parcel?, val credential: PickupCredential?, val message: String?)
    private suspend fun readDetail(location: String, selectedId: String?): Detail {
        val selected = selectedId?.let { id ->
            try { repository.parcel(location, id) }
            catch (error: AppFailure) { if (error.kind == FailureKind.DENIED) null else throw error }
        }
        var credential: PickupCredential? = null
        var message: String? = null
        if (selected?.status == ParcelStatus.WAITING) {
            try { credential = LoadPickupCredential(repository, clock)(location, selected) }
            catch (error: AppFailure) {
                if (error.kind in setOf(FailureKind.EXPIRED_CODE, FailureKind.UNAVAILABLE)) message = error.message
                else throw error
            }
        }
        return Detail(selected, credential, message)
    }
    private suspend fun loadDetail(generation: Int) {
        val context = state.value
        val detail = readDetail(context.membershipId, context.selectedId)
        if (generation != epoch) return
        mutable.update { it.copy(selectedId = detail.parcel?.id, selected = detail.parcel,
            credential = detail.credential, credentialMessage = detail.message, now = clock.now()) }
    }
    fun select(id: String, openDetail: Boolean = true) {
        if (actionJob?.isActive == true) return
        mutable.update { it.copy(selectedId = id, selected = null, credential = null,
            route = if (openDetail) Route.DETAIL else it.route) }
        refreshData(detailOnly = true)
    }
    fun navigate(route: Route) {
        if (route == Route.RESIDENTS && state.value.busy) {
            feedback("Aguarde a operação em andamento para abrir os moradores.")
            return
        }
        val features = features
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
        if (actionJob?.isActive == true) return
        mutable.update { it.copy(filter = value, parcels = emptyList(), nextCursor = null) }
        refresh()
    }
    fun membership(id: String) {
        if (actionJob?.isActive == true) return
        if (state.value.profile?.memberships?.none { it.id == id } != false) return
        epoch++
        readJob?.cancel()
        mutable.update { it.copy(membershipId = id, selectedId = null, selected = null, credential = null,
            parcels = emptyList(), pending = emptyList(), recent = emptyList(), notices = emptyList(), issues = emptyList(),
            statistics = null, nextCursor = null, noticeCursor = null, serverUnreadCount = null,
            issueCursor = null, residents = emptyList(), route = Route.HOME) }
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
            mutable.update { it.copy(feedback = message, credential = null, stale = true) }
            refreshAfterMutation(generation)
        }
    }
    private suspend fun refreshAfterMutation(generation: Int) {
        try { load(generation) }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) {
            if (generation == epoch) {
                handle(error)
                if (state.value.session != null) mutable.update {
                    it.copy(error = "A operação foi concluída, mas a atualização falhou. Atualize para conferir.")
                }
            }
        }
    }
    fun moreNotices() = execute { generation ->
        val context = state.value
        val cursor = context.noticeCursor ?: return@execute
        val page = repository.noticePage(context.membershipId, cursor)
        if (generation == epoch) mutable.update { it.copy(notices = (it.notices + page.items).distinctBy { notice -> notice.id },
            noticeCursor = page.nextCursor, serverUnreadCount = page.unreadCount) }
    }
    fun moreIssues() = execute { generation ->
        val context = state.value
        val cursor = context.issueCursor ?: return@execute
        val page = repository.issuePage(context.membershipId, cursor)
        if (generation == epoch) mutable.update { it.copy(issues = (it.issues + page.items).distinctBy { issue -> issue.id },
            issueCursor = page.nextCursor) }
    }
    fun preferences(value: CommunicationPreferences) = execute {
        val generation = epoch
        val profile = repository.updatePreferences(value)
        if (generation == epoch) mutable.update { normalizeCapabilities(it.copy(profile = profile, feedback = "Preferências salvas.")) }
    }
    fun contact(value: String, channel: LoginChannel) = execute { generation ->
        val challenge = repository.requestContactChange(value, channel)
        if (generation != epoch) return@execute
        mutable.update { it.copy(contactChallenge = challenge, contactValue = value, contactChannel = channel) }
    }
    fun resendContact() = execute { generation ->
        val context = state.value
        val challenge = context.contactChallenge ?: return@execute
        val updated = repository.resendContactChange(challenge.id, context.contactValue, context.contactChannel)
        if (generation == epoch) mutable.update { it.copy(contactChallenge = updated) }
    }
    fun correctProfileContact() {
        epoch++
        readJob?.cancel()
        actionJob?.cancel()
        mutable.update { it.copy(contactChallenge = null, error = null, busy = false) }
    }
    fun verifyContact(code: String) = execute { generation ->
        val challenge = state.value.contactChallenge ?: return@execute
        val profile = repository.verifyContactChange(challenge.id, code)
        if (generation != epoch) return@execute
        mutable.update { it.copy(profile = profile, contactChallenge = null, contactValue = "", route = Route.PROFILE,
            feedback = "Contato verificado e atualizado.") }
    }
    fun report(message: String) = execute { generation ->
        val context = state.value
        val selectedId = context.selectedId ?: return@execute
        val issue = repository.reportIssue(context.membershipId, selectedId, message)
        if (generation != epoch) return@execute
        mutable.update { it.copy(issues = it.issues + issue, issueSubmission = it.issueSubmission + 1,
            feedback = "Solicitação ${issue.reference} recebida. Acompanhe nesta tela.", stale = true, credential = null) }
        refreshAfterMutation(generation)
    }
    fun notice(value: DeliveryNotice) = execute { generation ->
        repository.markNoticeRead(state.value.membershipId, value.id)
        if (generation != epoch) return@execute
        mutable.update { it.copy(notices = it.notices.map { notice ->
            if (notice.id == value.id) notice.copy(read = true) else notice
        }, serverUnreadCount = it.serverUnreadCount?.let { count ->
            if (it.notices.any { notice -> notice.id == value.id && !notice.read }) (count - 1).coerceAtLeast(0) else count
        }) }
        mutable.update { it.copy(selectedId = value.parcelId, selected = null, credential = null, route = Route.DETAIL) }
        loadDetail(generation)
    }
    fun demoScenario(value: DemoScenario) = execute { generation ->
        val demo = repository as? DemoControls ?: return@execute
        demo.scenario(value)
        if (generation != epoch) return@execute
        mutable.update { it.copy(selectedId = null, selected = null, credential = null) }
        load()
    }
    fun deposit() = execute { generation ->
        val demo = repository as? DemoControls ?: return@execute
        demo.deposit(state.value.membershipId)
        if (generation != epoch) return@execute
        load()
        if (generation == epoch) feedback("Depósito fictício criado. Veja a nova encomenda e o aviso.")
    }
    }
    fun logout() {
        epoch++
        readJob?.cancel()
        actionJob?.cancel()
        previousUser = null
        lastLogin = null
        mutable.value = AppState(initialized = true, remoteBrand = state.value.remoteBrand, now = clock.now())
        execute {
            try { repository.logout() }
            finally {
                previousUser = null
                lastLogin = null
                mutable.value = AppState(initialized = true, remoteBrand = state.value.remoteBrand, now = clock.now())
            }
        }
    }
    fun feedback(message: String?) { mutable.update { it.copy(feedback = message) } }
    fun close() { scope.cancel(); repository.close() }
}
