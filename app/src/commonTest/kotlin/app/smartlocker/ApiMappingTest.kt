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
    }

    @Test fun presentationSessionExpiresWithRefreshAndItsDiagnosticTextRedactsTokens() {
        val tokens = ApiSessionTokens("Bearer", "opaque-access", start, "opaque-refresh", end,
            "user", "smartlocker", "session", listOf("profile:read"))
        assertEquals(Instant.parse(end).toEpochMilliseconds(), tokens.toDomain().expiresAt)
        assertFalse(tokens.toString().contains(tokens.accessToken))
        assertFalse(tokens.toString().contains(tokens.refreshToken))
        failsSafely { tokens.copy(tokenType = "Basic").toDomain() }
        failsSafely { tokens.copy(accessToken = "token\r\nInjected").toDomain() }
    }

    @Test fun standaloneMembershipAndAbsentContactsDoNotInventAUnitOrContact() {
        val mapped = member.toDomain()
        assertEquals("", mapped.unit)
        assertEquals("America/Sao_Paulo", mapped.timeZone)
        val profile = ApiProfile("user", "Ana", null, null, null, null,
            ApiCommunicationPreferences(true, false, false), listOf(member)).toDomain(listOf(mapped))
        assertEquals("", profile.phone)
        assertEquals("", profile.email)
        failsSafely { member.copy(unitId = "unit", unitLabel = null).toDomain() }
        failsSafely { member.copy(timeZone = "Invalid/Nowhere").toDomain() }
    }

    @Test fun remoteConfigurationCannotEnableBundledDisabledFeatures() {
        val config = ApiBrandConfiguration("aurora", "Aurora Lockers", capabilities, null,
            "https://example.test/terms", "https://example.test/privacy")
        val brand = config.toDomain(Brands.aurora)
        assertFalse(brand.features.residents)
        assertFalse("whatsapp" in brand.channels)
        assertFalse("push" in brand.channels)
        failsSafely { config.copy(brandId = "other").toDomain(Brands.aurora) }
        failsSafely { config.copy(termsUrl = "http://example.test/terms").validate("aurora") }
    }

    @Test fun parcelMappingPreservesScopeVersionAndRejectsImpossibleStatuses() {
        val mapped = parcel.toDomain()
        assertEquals("membership", mapped.locationId)
        assertEquals("3", mapped.version)
        assertEquals(ParcelStatus.WAITING, mapped.status)
