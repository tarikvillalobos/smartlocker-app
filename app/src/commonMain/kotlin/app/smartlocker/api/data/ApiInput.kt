package app.smartlocker.api.data

import app.smartlocker.auth.domain.*
import app.smartlocker.shared.domain.*
import kotlinx.serialization.SerializationException

internal inline fun <reified T> decodeApi(value: String): T = try { ApiJson.decodeFromString<T>(value) }
catch (_: SerializationException) { invalidApiResponse() }
catch (_: IllegalArgumentException) { invalidApiResponse() }

internal fun invalidInput(message: String): Nothing = throw AppFailure(FailureKind.VALIDATION, message)
internal fun LoginChannel.apiValue() = if (this == LoginChannel.SMS) "sms" else "email"
internal fun normalizedApiContact(value: String, channel: LoginChannel): String {
    if (channel == LoginChannel.EMAIL) {
        if (!InputValidation.email(value)) invalidInput("Confira o endereço de e-mail informado.")
        return value.trim()
    }
    if (!InputValidation.phone(value)) invalidInput("Confira o celular informado.")
    val digits = value.filter(Char::isDigit)
    return if (digits.length == 11) "+55$digits" else "+$digits"
