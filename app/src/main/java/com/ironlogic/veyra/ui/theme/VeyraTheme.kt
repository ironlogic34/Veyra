package com.ironlogic.veyra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Temporary theme entry point.
 *
 * The final colour, typography, shape, and motion systems will be defined
 * after the phone and tablet design specifications are transferred.
 */
@Composable
fun VeyraTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
