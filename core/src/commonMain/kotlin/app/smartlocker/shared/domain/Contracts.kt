package app.smartlocker.shared.domain

import app.smartlocker.auth.domain.*
import app.smartlocker.parcels.domain.*
import app.smartlocker.profile.domain.*

fun interface AppClock { fun now(): Long }
interface LocalStorage {
    fun read(key: String): String?
    fun write(key: String, value: String?)
}
interface SecureStorage {
    suspend fun read(key: String): String?
    suspend fun write(key: String, value: String?)
}

enum class FailureKind {
    VALIDATION, NETWORK, EXPIRED_SESSION, DENIED, EXPIRED_CODE,
    INVALID_CODE, ATTEMPTS_EXCEEDED, UNAVAILABLE, CONFLICT, MISSING_CONTRACT,
}
