package app.smartlocker.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.smartlocker.parcels.domain.*
import kotlinx.datetime.*
import kotlin.time.Instant

@Composable
fun Panel(modifier: Modifier = Modifier, dark: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = if (dark) Tokens.hero else Tokens.card,
        color = if (dark) MaterialTheme.colorScheme.secondary else Color.White,
        contentColor = if (dark) Color.White else Tokens.text,
        border = if (dark) null else BorderStroke(1.dp, Tokens.border)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun PrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = Tokens.fieldHeight), enabled = enabled,
        shape = Tokens.control, contentPadding = PaddingValues(14.dp)) { Text(label) }
}

@Composable
fun PageTitle(title: String, back: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (back != null) IconButton(back) { AppIcon(Symbol.BACK, "Voltar") }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary)
        action?.invoke()
    }
}

@Composable
fun StatusBadge(parcel: Parcel) {
    val (label, background, foreground) = when (parcel.status) {
        ParcelStatus.WAITING -> Triple("Aguardando", Tokens.warning, Tokens.warningText)
        ParcelStatus.MANUAL -> Triple("Informada por você", Tokens.success, Tokens.successText)
        ParcelStatus.COLLECTED -> Triple("Retirada", Tokens.success, Tokens.successText)
    }
    Surface(color = background, contentColor = foreground, shape = CircleShape) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun EmptyState(title: String, description: String) {
    Panel {
        AppIcon(Symbol.PARCEL, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, color = Tokens.secondary)
    }
}

@Composable
fun MenuRow(label: String, symbol: Symbol = Symbol.NEXT, onClick: () -> Unit) {
    TextButton(onClick, Modifier.fillMaxWidth().heightIn(min = Tokens.touch),
        shape = Tokens.control, contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp)) {
        AppIcon(symbol, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), color = Tokens.text)
        AppIcon(Symbol.NEXT, modifier = Modifier.size(18.dp), tint = Tokens.secondary)
    }
}

@Composable
fun Metadata(label: String, value: String, light: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = if (light) Tokens.soft else Tokens.secondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

fun dateTime(timestamp: Long): String {
    val value = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.of("America/Sao_Paulo"))
    fun Int.pad() = toString().padStart(2, '0')
}
fun durationLabel(millis: Long?): String {
    if (millis == null) return "Sem dados"
    val minutes = millis / 60_000
    return "${minutes / 60}h ${minutes % 60}min"
}
