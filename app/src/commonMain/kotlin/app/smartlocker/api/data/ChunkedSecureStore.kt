package app.smartlocker.api.data

import app.smartlocker.shared.domain.AppFailure
import app.smartlocker.shared.domain.FailureKind
import app.smartlocker.shared.domain.SecureStorage
import kotlinx.coroutines.CancellationException
import kotlin.io.encoding.Base64
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** All calls, including calls through other instances with this key, must be serialized by the owner. */
@OptIn(ExperimentalUuidApi::class)
class ChunkedSecureStore(private val storage: SecureStorage, private val key: String) {
    private val headKey = "$key.head"
    private val journalKey = "$key.journal"

    init {
        if (!key.matches(Regex("[A-Za-z0-9._-]{1,100}"))) throw failure()
    }

