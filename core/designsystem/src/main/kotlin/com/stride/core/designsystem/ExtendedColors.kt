package com.stride.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Effort/recovery aren't M3 roles, so they don't belong shoehorned into `primary`/`secondary` —
 * a custom theme extension via CompositionLocal, per M3 theming-consistency guidance, rather
 * than a global object that can't be swapped per-theme.
 */
data class StrideExtendedColors(
    val action: Color,
    val onAction: Color,
    val actionContainer: Color,
    val recovery: Color,
    val onRecovery: Color,
    val recoveryContainer: Color,
)

internal val LightExtendedColors = StrideExtendedColors(
    action = ActionLight,
    onAction = Color.White,
    actionContainer = ActionContainerLight,
    recovery = RecoveryLight,
    onRecovery = Color.White,
    recoveryContainer = RecoveryContainerLight,
)

internal val DarkExtendedColors = StrideExtendedColors(
    action = ActionDark,
    onAction = Color(0xFF14181A),
    actionContainer = ActionContainerDark,
    recovery = RecoveryDark,
    onRecovery = Color(0xFF14181A),
    recoveryContainer = RecoveryContainerDark,
)

val LocalStrideExtendedColors = staticCompositionLocalOf { LightExtendedColors }
