package app.smartlocker.shared.data

import app.smartlocker.shared.domain.LocalStorage

data class CacheScope(val brand: String, val user: String, val membership: String)

/** Stores non-secret metadata only. Pickup credentials must never enter this cache. */
class ScopedCache(private val storage: LocalStorage) {
    private fun key(scope: CacheScope): String {
        val parts = listOf(scope.brand, scope.user, scope.membership)
        return "metadata." + parts.joinToString(".") { "${it.length}:$it" }
    }
    fun read(scope: CacheScope): String? = storage.read(key(scope))
    fun write(scope: CacheScope, value: String) = storage.write(key(scope), value)
    fun clear(scope: CacheScope) = storage.write(key(scope), null)
}
