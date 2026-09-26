package app.smartlocker.demo.data

import app.smartlocker.auth.domain.*
import app.smartlocker.shared.domain.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class StoredSession(val token: String, val userId: String, val expiresAt: Long)

class DemoAuth(private val secure: SecureStorage, brandId: String, private val clock: AppClock) {
    private val key = "demo.$brandId.session"
    private var challenge: Challenge? = null
    private var attempts = 0
    private var serial = 0
    var session: Session? = null
        private set

    fun request(request: LoginRequest): Challenge {
        val valid = when (request.channel) {
