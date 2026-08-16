package com.stride.core.designsystem

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * M3 duration ladder — pair a token with the matching easing rather than picking arbitrary
 * millis, so every screen's motion feels like one system instead of N ad-hoc animations.
 */
object StrideMotion {
    // Micro-interactions: state changes, selection, the effort/recovery color cross-fade.
    const val DURATION_SHORT = 150

    // Standard transitions: screen enter/exit, card expansion, nav transitions.
    const val DURATION_MEDIUM = 350

    // Container transforms, larger reveals.
    const val DURATION_LONG = 500

    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0f, 1.0f)
    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.2f, 1.0f)
}

/**
 * Compose has no built-in reduced-motion signal — read the system's animator-duration-scale
 * once per Activity and provide it, so any screen can gate its own AnimatedVisibility/animate*
 * calls on it instead of every screen re-deriving the same Settings.Global read.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}
