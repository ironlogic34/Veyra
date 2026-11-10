package com.ironlogic.veyra.ui.video.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/**
 * A reusable video-preview frame.
 *
 * It intentionally owns only clipping, aspect ratio, and the fallback surface.
 * Phone and tablet screens supply their own content, sizing, and styling.
 */
@Composable
fun VideoThumbnailFrame(
    modifier: Modifier = Modifier,
    aspectRatio: Float = 16f / 9f,
    shape: Shape = RoundedCornerShape(12.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable BoxScope.() -> Unit
) {
    require(aspectRatio > 0f) { "Aspect ratio must be greater than zero." }

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(shape)
            .background(containerColor),
        content = content
    )
}
