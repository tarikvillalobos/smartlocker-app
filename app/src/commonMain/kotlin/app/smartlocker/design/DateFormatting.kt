package app.smartlocker.design

import androidx.compose.runtime.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

val LocalDisplayTimeZone = staticCompositionLocalOf { TimeZone.UTC }

@Composable
fun MembershipTimeZone(timeZoneId: String?, content: @Composable () -> Unit) {
    val timeZone = remember(timeZoneId) { timeZoneId?.let(TimeZone::of) ?: TimeZone.UTC }
    CompositionLocalProvider(LocalDisplayTimeZone provides timeZone, content = content)
}

@Composable
fun dateTime(timestamp: Long): String = formatDateTime(timestamp, LocalDisplayTimeZone.current)

fun formatDateTime(timestamp: Long, timeZone: TimeZone): String {
    val value = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone)
