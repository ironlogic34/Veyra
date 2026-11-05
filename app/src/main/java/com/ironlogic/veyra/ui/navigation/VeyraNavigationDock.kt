package com.ironlogic.veyra.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A destination supplied by the screen/navigation owner.
 *
 * Veyra does not hard-code destination names or icons here. Phone and tablet
 * navigation can provide different destination sets and arrangements.
 */
data class VeyraNavigationDestination(
    val id: String,
    val label: String,
    val icon: @Composable (selected: Boolean) -> Unit
)

/**
 * Reusable floating navigation dock.
 *
 * This first implementation provides tap selection and an animated selected
 * item. Glass rendering, drag-to-select, and final device-specific geometry
 * are intentionally separate follow-up work.
 */
@Composable
fun VeyraNavigationDock(
    destinations: List<VeyraNavigationDestination>,
    selectedId: String,
    onDestinationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    selectedItemColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    shape: Shape = RoundedCornerShape(percent = 50),
    itemSpacing: Dp = 4.dp,
    selectedHorizontalPadding: Dp = 18.dp,
    unselectedHorizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 12.dp
) {
    require(destinations.map { it.id }.distinct().size == destinations.size) {
        "Navigation destination IDs must be unique."
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEach { destination ->
                val isSelected = destination.id == selectedId

                Surface(
                    modifier = Modifier
                        .animateContentSize()
                        .semantics { selected = isSelected }
                        .clickable(role = Role.Tab) {
                            onDestinationSelected(destination.id)
                        },
                    shape = RoundedCornerShape(percent = 50),
                    color = if (isSelected) selectedItemColor else Color.Transparent,
                    contentColor = if (isSelected) selectedContentColor else contentColor
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = if (isSelected) {
                                selectedHorizontalPadding
                            } else {
                                unselectedHorizontalPadding
                            },
                            vertical = verticalPadding
                        ),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        destination.icon(isSelected)

                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            Text(
                                text = destination.label,
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
