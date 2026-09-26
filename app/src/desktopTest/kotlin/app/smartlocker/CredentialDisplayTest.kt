package app.smartlocker

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import app.smartlocker.config.*
import app.smartlocker.design.*
import app.smartlocker.parcels.domain.*
import app.smartlocker.parcels.presentation.PickupCard
import app.smartlocker.shared.presentation.AppState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class CredentialDisplayTest {
    @Test fun changingTheSelectedMembershipTimeZoneUpdatesVisibleDates() = runDesktopComposeUiTest(width = 390, height = 300) {
        var timeZoneId by mutableStateOf<String?>("America/Sao_Paulo")
        val instant = Instant.parse("2026-09-26T01:15:00Z").toEpochMilliseconds()
        setContent {
            MembershipTimeZone(timeZoneId) { Text(dateTime(instant)) }
        }
        onNodeWithText("25/09 · 22:15").assertExists()
        runOnIdle { timeZoneId = "Asia/Tokyo" }
        onNodeWithText("26/09 · 10:15").assertExists()
        onNodeWithText("25/09 · 22:15").assertDoesNotExist()
        runOnIdle { timeZoneId = null }
        onNodeWithText("26/09 · 01:15").assertExists()
    }

    @Test fun oversizedQrKeepsTheAuthorizedNumericCodeAndCopyActionUsable() = runDesktopComposeUiTest(width = 390, height = 900) {
        val platform = TestPlatform()
        val runtime = AppRuntime(platform, AppConfiguration(Brands.smartLocker, Environment.DEMO))
        val now = 1_000_000L
        val parcel = Parcel("parcel", "recipient", "membership", "Transportadora", null,
            "Armário", "Endereço", "7", null, now - 60_000, now - 30_000, now + 60_000)
        val credential = PickupCredential(parcel.id, "001234", "🔒".repeat(1024), now + 60_000, now, CredentialStatus.ACTIVE)
        val state = AppState(initialized = true, selected = parcel, credential = credential, now = now)
        try {
