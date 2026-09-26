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
