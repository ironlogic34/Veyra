package com.ironlogic.veyra.ui.video.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ironlogic.veyra.data.video.VideoDisplayFormatters
import com.ironlogic.veyra.data.video.VideoItem
import com.ironlogic.veyra.data.video.VideoThumbnailDataSource

/**
 * Reusable video card. The caller controls grid/list geometry and all
 * navigation; this component only presents one indexed video.
 */
@Composable
fun VideoLibraryCard(
    video: VideoItem,
    thumbnailDataSource: VideoThumbnailDataSource,
    onClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var thumbnail by remember(video.contentUri) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(video.contentUri, thumbnailDataSource) {
        thumbnail = thumbnailDataSource.load(video.contentUri)
    }

    Card(
        onClick = { onClick(video) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VideoThumbnailFrame(modifier = Modifier.fillMaxWidth()) {
                thumbnail?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Preview of ${video.displayName}",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Text(
                text = video.displayName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2
            )
            Text(
                text = listOf(
                    VideoDisplayFormatters.formatDuration(video.durationMs),
                    video.relativePath ?: "Device library"
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
        }
    }
}
