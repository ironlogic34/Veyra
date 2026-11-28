package com.ironlogic.veyra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VeyraDarkColors = darkColorScheme(
    primary = Color(0xFFFF9E5E),
    onPrimary = Color(0xFF321300),
    background = Color(0xFF111014),
    onBackground = Color(0xFFF4EDF5),
    surface = Color(0xFF211E25),
    onSurface = Color(0xFFF4EDF5),
)

@Composable
fun VeyraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VeyraDarkColors,
        typography = Typography(),
        content = content,
    )
}
