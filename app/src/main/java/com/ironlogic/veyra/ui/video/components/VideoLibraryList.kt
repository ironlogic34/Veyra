package com.ironlogic.veyra.ui.video.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ironlogic.veyra.data.video.VideoItem
import com.ironlogic.veyra.data.video.VideoThumbnailDataSource

/**
 * Shared list presentation for indexed videos.
 *
 * This component deliberately does not choose screen-level geometry,
 * navigation, filtering controls, or phone/tablet layout.
 */
@Composable
fun VideoLibraryList(
    videos: List<VideoItem>,
    thumbnailDataSource: VideoThumbnailDataSource,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalItemSpacing: Int = 12,
    emptyMessage: String = "No videos found"
) {
    if (videos.isEmpty()) {
        Text(
            text = emptyMessage,
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(verticalItemSpacing.dp)
    ) {
        items(
            items = videos,
            key = { video -> video.contentUri.toString() }
        ) { video ->
            VideoLibraryCard(
                video = video,
                thumbnailDataSource = thumbnailDataSource,
                onClick = onVideoClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
