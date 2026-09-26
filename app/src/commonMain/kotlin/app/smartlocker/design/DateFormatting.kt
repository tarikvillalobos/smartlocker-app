package app.smartlocker.design

import androidx.compose.runtime.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
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
    fun Int.pad() = toString().padStart(2, '0')
    return "${value.day.pad()}/${value.month.number.pad()} · ${value.hour.pad()}:${value.minute.pad()}"
}
