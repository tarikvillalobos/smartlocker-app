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
        assertTrue(mapped.canReportIssue)
        failsSafely { parcel.copy(status = "future").toDomain() }
        failsSafely { parcel.copy(credentialStatus = "future").toDomain() }
        failsSafely { parcel.copy(status = "manual", credentialStatus = "revoked").toDomain() }
        failsSafely { parcel.copy(status = "collected", collectedAt = start, credentialStatus = "active").toDomain() }
        failsSafely { parcel.copy(collectedAt = earlier, status = "collected", credentialStatus = "consumed").toDomain() }
        failsSafely { parcel.copy(version = 0).toDomain() }
    }

    @Test fun pickupUsesServerRevalidationAndPreservesLeadingZeroes() {
        val revalidate = "2026-09-26T12:02:00.000Z"
        val dto = ApiPickupCredential("parcel", "membership", "001234", "AUTHORIZED-SYNTHETIC-PAYLOAD",
            "active", start, end, revalidate)
        val mapped = dto.toDomain()
        assertEquals("001234", mapped.code)
        assertEquals(startMillis + 120_000, mapped.revalidateAt)
        assertEquals(CredentialStatus.ACTIVE, mapped.status)
        assertFalse(dto.toString().contains(dto.code))
        failsSafely { dto.copy(status = "revoked").toDomain() }
        failsSafely { dto.copy(revalidateAfter = earlier).toDomain() }
        failsSafely { dto.copy(code = "12AB34").toDomain() }
    }

    @Test fun partialMetricsRemainUnavailableAndCompleteMetricsConvertSecondsExactly() {
        val dto = ApiParcelMetrics(start, end, end, false, null, null, null)
        val missing = dto.toDomain()
        assertFalse(missing.complete)
        assertNull(missing.total)
        assertNull(missing.averageMillis)
        val complete = dto.copy(complete = true, totalReceived = 2, physicalPickupCount = 1,
            averagePickupDurationSeconds = 1.125).toDomain()
        assertEquals(1125L, complete.averageMillis)
        assertEquals(Instant.parse(end).toEpochMilliseconds(), complete.until)
        failsSafely { dto.copy(totalReceived = 0).toDomain() }
        failsSafely { dto.copy(complete = true, totalReceived = 1, physicalPickupCount = 2).toDomain() }
    }

    @Test fun pagesRejectDuplicateIdentifiersAndPreserveAuthoritativeUnreadTotal() {
        val notice = ApiDeliveryNotice("notice", "membership", "parcel", "Sua encomenda chegou", start, null)
        val mapped = ApiNoticePage(listOf(notice), page, 25).toDomain()
        assertEquals(25, mapped.unreadCount)
        assertFalse(mapped.items.single().read)
        failsSafely { ApiNoticePage(listOf(notice), page, 0).toDomain() }
        failsSafely { ApiNoticePage(listOf(notice, notice), page, 25).toDomain() }
        failsSafely { ApiParcelPage(listOf(parcel, parcel), page).toDomain() }
        failsSafely { page.copy(snapshotExpiresAt = earlier).validatedCursor() }
    }

    @Test fun supportStatesAndDatesAreExplicitAndFailuresNeverEchoRemoteInput() {
        val issue = ApiSupportIssue("issue", "SL-001", "membership", "parcel", "A porta não abriu.",
            "resolved", start, end, "Problema resolvido.")
        assertEquals("Resolvida", issue.toDomain().status)
        val invalid = "unexpected-sensitive-remote-value"
        val failure = assertFailsWith<AppFailure> { issue.copy(status = invalid).toDomain() }
        assertFalse(failure.message.contains(invalid))
        failsSafely { issue.copy(updatedAt = earlier).toDomain() }
        failsSafely { issue.copy(createdAt = "2026-09-26T09:00:00-03:00").toDomain() }
    }

    private fun failsSafely(block: () -> Unit) {
