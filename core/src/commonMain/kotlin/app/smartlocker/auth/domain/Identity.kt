package app.smartlocker.auth.domain

data class Session(val token: String, val userId: String, val expiresAt: Long)
data class Challenge(val id: String, val expiresAt: Long, val resendAt: Long)
enum class LoginChannel { SMS, EMAIL }
data class LoginRequest(val contact: String, val cpf: String, val channel: LoginChannel)

object InputValidation {
    fun phone(value: String): Boolean {
        val raw = value.filter(Char::isDigit)
        val digits = if (raw.length == 13 && raw.startsWith("55")) raw.drop(2) else raw
        return digits.length == 11 && digits[2] == '9' && digits.take(2).toInt() >= 11
    }

    fun email(value: String): Boolean =
        Regex("^[A-Za-z0-9.!#%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
            .matches(value.trim()) && value.length <= 254

    fun cpf(value: String): Boolean {
        val digits = value.filter(Char::isDigit)
        if (digits.length != 11 || digits.toSet().size == 1) return false
        return (9..10).all { length ->
            val sum = (0 until length).sumOf { digits[it].digitToInt() * (length + 1 - it) }
            val check = (sum * 10 % 11).let { if (it == 10) 0 else it }
            check == digits[length].digitToInt()
        }
    }
}
