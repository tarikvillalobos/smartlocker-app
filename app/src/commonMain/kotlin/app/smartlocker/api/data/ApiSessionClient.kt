package app.smartlocker.api.data

import app.smartlocker.auth.domain.*
import app.smartlocker.config.Brand
import app.smartlocker.shared.data.HttpTransport
import app.smartlocker.shared.domain.*
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
private data class StoredApiSession(
    val baseUrl: String, val tokens: ApiSessionTokens, val pendingRefreshKey: String? = null,
) {
