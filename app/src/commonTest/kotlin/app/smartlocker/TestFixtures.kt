package app.smartlocker

import app.smartlocker.auth.domain.*
import app.smartlocker.demo.data.DemoRepository
import app.smartlocker.shared.domain.*

class MemoryStorage : LocalStorage, SecureStorage {
    val values = mutableMapOf<String, String>()
    override fun read(key: String): String? = values[key]
    override fun write(key: String, value: String?) { if (value == null) values.remove(key) else values[key] = value }
}
