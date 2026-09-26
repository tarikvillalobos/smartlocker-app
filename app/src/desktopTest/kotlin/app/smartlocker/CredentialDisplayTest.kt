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
