package com.ironlogic.veyra.ui.video.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.ironlogic.veyra.data.video.VideoMetadataRepository
import com.ironlogic.veyra.data.video.VideoThumbnailDataSource

/**
 * Reusable video card. The caller controls grid/list geometry and all
 * navigation; this component only presents one indexed video.
 */
@Composable
fun VideoLibraryCard(
    video: VideoItem,
    thumbnailDataSource: VideoThumbnailDataSource,
    metadataRepository: VideoMetadataRepository,
    onClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var thumbnail by remember(video.contentUri) { mutableStateOf<android.graphics.Bitmap?>(null) }

    var enrichedVideo by remember(video.contentUri) { mutableStateOf(video) }

    LaunchedEffect(video.contentUri, thumbnailDataSource) {
        thumbnail = thumbnailDataSource.load(video.contentUri)
    }

    LaunchedEffect(video, metadataRepository) {
        enrichedVideo = video
        enrichedVideo = metadataRepository.enrichOne(video)
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
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Text(
                text = video.displayName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2
            )
            val resolution = VideoDisplayFormatters.formatResolution(
                enrichedVideo.width,
                enrichedVideo.height,
                enrichedVideo.rotationDegrees
            )
            Text(
                text = buildList {
                    add(VideoDisplayFormatters.formatDuration(enrichedVideo.durationMs))
                    if (resolution != "Unknown") add(resolution)
                    add(VideoDisplayFormatters.formatFileSize(enrichedVideo.sizeBytes))
                    add(enrichedVideo.relativePath ?: "Device library")
                }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
        }
    }
}
