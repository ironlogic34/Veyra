package com.ironlogic.veyra.ui.interaction

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Shared press-response scale references informed by the LastWave interaction study.
 * Choose a value according to the individual component specification.
 */
object VeyraPressScale {
    const val Subtle = 0.98f
    const val Standard = 0.96f
    const val Expressive = 0.88f
}

/**
 * Adds an animated scale response driven by an interaction source.
 *
 * Pass the same [InteractionSource] to this modifier and the component's
 * clickable modifier. This observes input state but does not make the
 * component clickable or add haptic feedback.
 */
@Composable
fun Modifier.veyraPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = VeyraPressScale.Standard,
    enabled: Boolean = true
): Modifier {
    require(pressedScale > 0f && pressedScale <= 1f) {
        "pressedScale must be greater than 0 and no greater than 1."
    }

    val isPressed by interactionSource.collectIsPressedAsState()
    val targetScale = if (enabled && isPressed) pressedScale else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "veyraPressScale"
    )

    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
