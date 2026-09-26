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
        ApiChannels(enabled, enabled, enabled, ApiChannelCapability(false), enabled))
    private val member get() = ApiMembership("membership", "location", "Estação Central", null, null,
        "America/Sao_Paulo", capabilities)
    private val page get() = ApiPageInfo(null, start, end)
    private val parcel get() = ApiParcel("parcel", "user", "membership", "Correios", null, "waiting",
        ApiLocker("locker", "Armário central", "Praça principal", true), "01", null,
        start, start, end, null, null, "active", ApiParcelActions(true, false, null, true),
        listOf(ApiTimelineEvent("deposited", start)), 3)

    @Test fun challengeUsesAuthoritativeTimingAndPurposeAndIgnoresExtraFields() {
        val raw = """{"id":"challenge","expiresAt":"$end","resendAt":"$start","channel":"sms",
            "maskedDestination":"+55 ** *****-4321","codeLength":6,"purpose":"login","futureField":true}"""
        val dto = ApiJson.decodeFromString<ApiChallenge>(raw)
        val mapped = dto.toDomain("login")
        assertEquals(LoginChannel.SMS, mapped.channel)
        assertEquals("+55 ** *****-4321", mapped.maskedDestination)
        assertEquals(Instant.parse(end).toEpochMilliseconds(), mapped.expiresAt)
        failsSafely { dto.toDomain("contact_change") }
        failsSafely { dto.copy(channel = "future_channel").toDomain() }
        failsSafely { dto.copy(codeLength = 8).toDomain() }
