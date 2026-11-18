package com.ironlogic.veyra.ui.video.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ironlogic.veyra.data.video.VideoContinueWatchingEntry
import com.ironlogic.veyra.data.video.VideoDisplayFormatters
import com.ironlogic.veyra.data.video.VideoItem

/** A caller-placed row of recent, incomplete local videos. */
@Composable
internal fun VideoContinueWatchingContent(
    items: List<VideoContinueWatchingEntry<VideoItem>>,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Continue watching", style = MaterialTheme.typography.titleSmall)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = items,
                key = { it.record.mediaUri }
            ) { entry ->
                TextButton(
                    onClick = { onVideoClick(entry.item) },
                    modifier = Modifier.widthIn(max = 220.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(entry.item.displayName, maxLines = 1)
                        Text(
                            text = "${VideoDisplayFormatters.formatDuration(entry.record.positionMs)} / " +
                                (entry.record.durationMs?.let(VideoDisplayFormatters::formatDuration) ?: "Unknown"),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
