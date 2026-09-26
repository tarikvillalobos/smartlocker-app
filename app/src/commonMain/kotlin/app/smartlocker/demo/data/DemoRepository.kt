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
