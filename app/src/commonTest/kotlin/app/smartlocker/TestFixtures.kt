package app.smartlocker

import app.smartlocker.auth.domain.*
import app.smartlocker.shared.domain.*

class MemoryStorage : LocalStorage {
    val values = mutableMapOf<String, String>()
    override fun read(key: String): String? = values[key]
    override fun write(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }
}
class MemorySecure : SecureStorage {
    val values = mutableMapOf<String, String>()
    override suspend fun read(key: String): String? = values[key]
    override suspend fun write(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }
}
class TestClock(var time: Long = 1_800_000_000_000) : AppClock {
    override fun now() = time
}
val demoLogin = LoginRequest("11987654321", "52998224725", LoginChannel.SMS)
suspend fun LockerRepository.signIn() {
    val challenge = requestLogin(demoLogin)
    verifyLogin(challenge.id, "123456")
}
