package app.smartlocker

import app.smartlocker.design.formatDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DateFormattingTest {
    @Test fun formatsTheSameInstantInTheMembershipTimeZoneIncludingCalendarRollover() {
        val instant = Instant.parse("2026-09-26T01:15:00Z").toEpochMilliseconds()
        assertEquals("26/09 · 01:15", formatDateTime(instant, TimeZone.UTC))
        assertEquals("25/09 · 22:15", formatDateTime(instant, TimeZone.of("America/Sao_Paulo")))
        assertEquals("26/09 · 10:15", formatDateTime(instant, TimeZone.of("Asia/Tokyo")))
    }
}
