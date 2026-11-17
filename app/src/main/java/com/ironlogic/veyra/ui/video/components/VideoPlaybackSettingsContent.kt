package com.ironlogic.veyra.ui.video.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ironlogic.veyra.data.video.VideoCompletionMode
import com.ironlogic.veyra.data.video.VideoPlaybackSettings

/**
 * Settings content without screen-level navigation or form-factor geometry.
 * The phone and tablet hosts can place this shared behaviour independently.
 */
@Composable
fun VideoPlaybackSettingsContent(
    settings: VideoPlaybackSettings,
    onSettingsChange: (VideoPlaybackSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Resume playback", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Continue videos from their last saved position.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = settings.resumePlayback,
                onCheckedChange = {
                    onSettingsChange(settings.copy(resumePlayback = it))
                }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("When a video ends", style = MaterialTheme.typography.titleMedium)
            Text(
                "Choose what happens at the end of the current video.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        VideoCompletionMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSettingsChange(settings.copy(completionMode = mode))
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = settings.completionMode == mode,
                    onClick = {
                        onSettingsChange(settings.copy(completionMode = mode))
                    }
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(mode.title(), style = MaterialTheme.typography.bodyLarge)
                    Text(mode.description(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun VideoCompletionMode.title(): String = when (this) {
    VideoCompletionMode.STOP -> "Stop"
    VideoCompletionMode.REPEAT -> "Repeat current video"
    VideoCompletionMode.PLAY_NEXT -> "Play next video"
}

private fun VideoCompletionMode.description(): String = when (this) {
    VideoCompletionMode.STOP -> "Stop playback when this video ends."
    VideoCompletionMode.REPEAT -> "Start this video again from the beginning."
    VideoCompletionMode.PLAY_NEXT -> "Continue with the next video in the queue."
}
