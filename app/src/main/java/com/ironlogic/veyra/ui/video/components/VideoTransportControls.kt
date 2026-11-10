package com.ironlogic.veyra.ui.video.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Reusable transport controls. The owning phone or tablet screen decides
 * placement, sizing, styling, and how player state is observed.
 */
@Composable
fun VideoTransportControls(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long?,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = durationMs?.takeIf { it > 0L }
    val safePosition = positionMs.coerceAtLeast(0L).coerceAtMost(duration ?: Long.MAX_VALUE)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (duration != null) {
            Slider(
                value = safePosition.toFloat().coerceIn(0f, duration.toFloat()),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text("Duration unavailable", style = MaterialTheme.typography.labelSmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(formatClock(safePosition), style = MaterialTheme.typography.labelSmall)
            Button(onClick = onPlayPause) {
                Text(if (isPlaying) "Pause" else "Play")
            }
            Text(duration?.let(::formatClock) ?: "--:--", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun formatClock(positionMs: Long): String {
    val totalSeconds = positionMs.coerceAtLeast(0L) / 1_000L
    val seconds = totalSeconds % 60L
    val minutes = totalSeconds / 60L
    return String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}
