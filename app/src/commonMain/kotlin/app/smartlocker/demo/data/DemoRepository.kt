package app.smartlocker.demo.data

import app.smartlocker.auth.domain.*
import app.smartlocker.config.Brand
import app.smartlocker.parcels.domain.*
import app.smartlocker.profile.domain.*
import app.smartlocker.shared.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DemoRepository(
    storage: LocalStorage,
    secure: SecureStorage,
    private val brand: Brand,
    private val clock: AppClock,
    private val latency: Long = 180,
) : LockerRepository, DemoControls {
    private val db = DemoDatabase(storage, brand.id, clock)
    private val auth = DemoAuth(secure, brand.id, clock)
    private val parcels = DemoParcels(db, clock)
    private val mutex = Mutex()
    private var scenario = DemoScenario.NORMAL
    private var pendingContact: Pair<String, LoginChannel>? = null

    private suspend fun check() {
        delay(latency)
        when (scenario) {
            DemoScenario.NETWORK -> throw AppFailure(FailureKind.NETWORK, "Sem conexão. Tente atualizar novamente.")
            DemoScenario.DENIED -> throw AppFailure(FailureKind.DENIED, "Acesso negado para este contexto.")
            DemoScenario.EXPIRED_SESSION -> throw AppFailure(FailureKind.EXPIRED_SESSION, "Sua sessão expirou.")
            else -> Unit
        }
        auth.requireSession()
    }

    override suspend fun requestLogin(request: LoginRequest): Challenge = mutex.withLock {
        delay(latency)
        auth.request(request)
    }
    override suspend fun verifyLogin(challengeId: String, code: String): Session = mutex.withLock {
        delay(latency)
        auth.login(challengeId, code).also { scenario = DemoScenario.NORMAL }
    }
    override suspend fun restoreSession() = auth.restore()
    override suspend fun logout() = mutex.withLock {
        db.clear()
        pendingContact = null
        auth.logout()
    }
    override suspend fun profile(): Profile {
        check()
        val data = db.snapshot
        return Profile("ana", "Ana Souza", data.phone, data.email,
            listOf(Membership("home", "Residencial Jardim", "Apto 42 · Bloco B"),
                Membership("office", "Edifício Horizonte", "Sala 204")),
            CommunicationPreferences(data.inApp, data.sms, data.whatsapp))
    }
    override suspend fun parcels(locationId: String, filter: ParcelFilter, cursor: String?): ParcelPage {
        check()
        return parcels.page(locationId, filter, cursor)
    }
    override suspend fun parcel(locationId: String, id: String): Parcel {
        check()
        return parcels.get(locationId, id)
    }
    override suspend fun statistics(locationId: String): Statistics {
        check()
        return calculateStatistics(parcels.all(locationId), clock.now() - 30 * 86_400_000L, clock.now())
    }
    override suspend fun credential(locationId: String, parcelId: String): PickupCredential {
        check()
        return parcels.credential(locationId, parcelId)
    }
    override suspend fun markCollected(locationId: String, parcelId: String): Parcel = mutex.withLock {
        check()
        if (!brand.features.manualPickup) throw AppFailure(FailureKind.DENIED, "Marcação manual não habilitada.")
        parcels.mark(locationId, parcelId, false)
    }
    override suspend fun undoManual(locationId: String, parcelId: String): Parcel = mutex.withLock {
        check()
        if (!brand.features.manualPickup) throw AppFailure(FailureKind.DENIED, "Recurso não habilitado.")
        parcels.undo(locationId, parcelId)
    }
    override suspend fun updatePreferences(value: CommunicationPreferences): Profile {
        mutex.withLock {
            check()
            db.update { it.copy(inApp = value.inApp, sms = value.sms, whatsapp = value.whatsapp) }
        }
        return profile()
    }
    override suspend fun requestContactChange(contact: String, channel: LoginChannel): Challenge {
        check()
        if (!brand.features.contactEditing) throw AppFailure(FailureKind.DENIED, "Edição indisponível.")
        return mutex.withLock {
            auth.request(LoginRequest(contact, "52998224725", channel)).also { pendingContact = contact to channel }
        }
    }
    override suspend fun verifyContactChange(challengeId: String, code: String): Profile {
        mutex.withLock {
            check()
            val (contact, channel) = pendingContact ?: throw AppFailure(FailureKind.CONFLICT, "Solicite um código.")
            auth.verify(challengeId, code)
            db.update { if (channel == LoginChannel.SMS) it.copy(phone = contact) else it.copy(email = contact) }
            pendingContact = null
        }
        return profile()
    }
    override suspend fun notifications(locationId: String): List<DeliveryNotice> {
        check()
        val ids = parcels.all(locationId).map { it.id }.toSet()
        return db.snapshot.notices.filter { it.parcel in ids }.map { it.toDomain() }
    }
    override suspend fun markNoticeRead(locationId: String, id: String) = mutex.withLock {
        val allowed = notifications(locationId).any { it.id == id }
        if (!allowed) throw AppFailure(FailureKind.DENIED, "Aviso indisponível.")
        db.update { it.copy(notices = it.notices.map { n -> if (n.id == id) n.copy(read = true) else n }) }
    }
    override suspend fun reportIssue(locationId: String, parcelId: String, message: String): SupportIssue = mutex.withLock {
        check()
        if (!brand.features.issues) throw AppFailure(FailureKind.DENIED, "Suporte não habilitado.")
        parcels.get(locationId, parcelId)
        if (message.trim().length !in 10..2000) throw AppFailure(FailureKind.VALIDATION, "Descreva o problema em 10 a 2.000 caracteres.")
        val issue = IssueRecord("SL-${db.snapshot.sequence}", parcelId, message.trim(), clock.now())
        db.update { it.copy(issues = it.issues + issue, sequence = it.sequence + 1) }
        issue.toDomain()
    }
    override suspend fun issues(locationId: String): List<SupportIssue> {
        check()
        if (!brand.features.issues) throw AppFailure(FailureKind.DENIED, "Suporte não habilitado.")
        val ids = parcels.all(locationId).map { it.id }.toSet()
        return db.snapshot.issues.filter { it.parcel in ids }.map { it.toDomain() }
    }
    override suspend fun recipients(locationId: String): List<Recipient> {
        check()
        requireLocation(locationId)
        if (!brand.features.residents) throw AppFailure(FailureKind.DENIED, "Destinatários não habilitados.")
        return listOf(Recipient("ana", "Ana Souza", "Você"), Recipient("other", "Rafael Souza", "Destinatário"))
    }
    override suspend fun deposit(locationId: String) = mutex.withLock { check(); parcels.deposit(locationId) }
    override suspend fun physicalPickup(locationId: String, parcelId: String) {
        mutex.withLock { check(); parcels.mark(locationId, parcelId, true) }
    }
    override suspend fun scenario(value: DemoScenario) = mutex.withLock {
        scenario = value
        when (value) {
            DemoScenario.NORMAL -> db.reset()
            DemoScenario.EMPTY -> db.update { it.copy(parcels = emptyList(), notices = emptyList()) }
            DemoScenario.MANY -> repeat(65) { parcels.deposit("home") }
            DemoScenario.EXPIRED_CODE -> db.update { it.copy(parcels = it.parcels.map { p -> p.copy(deadline = clock.now() - 1) }) }
            DemoScenario.LOCKER_OFFLINE -> db.update { it.copy(parcels = it.parcels.map { p -> p.copy(available = false) }) }
            else -> Unit
        }
    }
}
