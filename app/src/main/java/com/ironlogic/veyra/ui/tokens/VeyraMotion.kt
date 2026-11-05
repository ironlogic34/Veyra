package com.ironlogic.veyra.ui.tokens

/**
 * Semantic motion durations in milliseconds.
 *
 * These are reference starting points for shared transitions. Components
 * may use a spring or a different duration when their interaction requires it.
 * Do not use these values to override screen-specific motion decisions.
 */
object VeyraMotion {
    const val Quick = 160
    const val Standard = 300
    const val Emphasized = 440
}
