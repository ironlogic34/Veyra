package com.ironlogic.veyra.ui.video.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ironlogic.veyra.data.video.VideoSortOrder

/**
 * Reusable search and sort controls for a video collection.
 *
 * State stays with the screen so phone and tablet layouts can place these
 * controls independently and share the same query state.
 */
@Composable
fun VideoLibraryControls(
    searchQuery: String,
    sortOrder: VideoSortOrder,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (VideoSortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search videos") },
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box {
                TextButton(onClick = { sortMenuExpanded = true }) {
                    Text("Sort: ${sortOrder.label()}")
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    VideoSortOrder.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label()) },
                            onClick = {
                                onSortOrderChange(option)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun VideoSortOrder.label(): String = when (this) {
    VideoSortOrder.NEWEST_FIRST -> "Newest"
    VideoSortOrder.OLDEST_FIRST -> "Oldest"
    VideoSortOrder.NAME_A_TO_Z -> "Name A–Z"
    VideoSortOrder.NAME_Z_TO_A -> "Name Z–A"
    VideoSortOrder.DURATION_SHORTEST_FIRST -> "Shortest"
    VideoSortOrder.DURATION_LONGEST_FIRST -> "Longest"
    VideoSortOrder.SIZE_SMALLEST_FIRST -> "Smallest"
    VideoSortOrder.SIZE_LARGEST_FIRST -> "Largest"
}
