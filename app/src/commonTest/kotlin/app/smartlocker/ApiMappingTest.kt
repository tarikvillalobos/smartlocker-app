package app.smartlocker

import app.smartlocker.api.data.*
import app.smartlocker.auth.domain.LoginChannel
import app.smartlocker.config.Brands
import app.smartlocker.parcels.domain.CredentialStatus
import app.smartlocker.parcels.domain.ParcelStatus
import app.smartlocker.shared.domain.AppFailure
import app.smartlocker.shared.domain.FailureKind
import kotlinx.serialization.decodeFromString
import kotlin.test.*
import kotlin.time.Instant

class ApiMappingTest {
    private val start = "2026-09-26T12:00:00.000Z"
    private val end = "2026-09-27T12:00:00.000Z"
    private val earlier = "2026-09-26T11:00:00.000Z"
    private val startMillis = Instant.parse(start).toEpochMilliseconds()
    private val enabled = ApiChannelCapability(true)
    private val capabilities = ApiCapabilities(ApiFeatures(true, true, true, true, true, true),
