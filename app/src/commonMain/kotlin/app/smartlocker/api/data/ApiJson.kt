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

