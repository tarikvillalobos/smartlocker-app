package app.smartlocker.api.data

import app.smartlocker.shared.domain.AppFailure
import app.smartlocker.shared.domain.FailureKind
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.time.Instant

@OptIn(ExperimentalSerializationApi::class)
val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = false
    coerceInputValues = false
    allowSpecialFloatingPointValues = false
}

internal fun invalidApiResponse(): Nothing =
    throw AppFailure(FailureKind.UNAVAILABLE, "O serviço retornou dados incompatíveis. Tente atualizar novamente.")

internal fun apiRequire(condition: Boolean) {
    if (!condition) invalidApiResponse()
}

internal fun apiId(value: String): String {
    apiRequire(value.length in 1..128 && Regex("^[A-Za-z0-9_-]+$").matches(value))
    return value
}

internal fun apiText(value: String, maximum: Int, allowBlank: Boolean = false): String {
    apiRequire(value.length <= maximum && (allowBlank || value.isNotBlank()))
    return value
}

internal fun apiInstant(value: String): Long {
    apiRequire(Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?Z$").matches(value))
    return try { Instant.parse(value).toEpochMilliseconds() }
    catch (_: IllegalArgumentException) { invalidApiResponse() }
}

internal fun apiUniqueIds(ids: List<String>) {
    ids.forEach { apiId(it) }
    apiRequire(ids.size == ids.toSet().size)
}
