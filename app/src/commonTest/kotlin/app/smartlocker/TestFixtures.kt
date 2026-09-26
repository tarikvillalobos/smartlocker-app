package app.smartlocker

import app.smartlocker.auth.domain.*
import app.smartlocker.shared.domain.*

class MemoryStorage : LocalStorage {
    val values = mutableMapOf<String, String>()
    override fun read(key: String): String? = values[key]
}
