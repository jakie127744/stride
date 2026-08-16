package com.stride.core.designsystem

import androidx.compose.ui.graphics.Color

// Palette fixed in docs/foundation.md — warm = action/effort, cool = recovery.
// These are the *raw* values; StrideColorScheme below is what actually gets consumed.

// Light
internal val LightBackground = Color(0xFFF6F5F1)
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightSurfaceVariant = Color(0xFFEFEDE7)
internal val LightOutline = Color(0xFFDAD6CD)
internal val LightOnBackground = Color(0xFF1B211E)
internal val LightOnSurfaceMuted = Color(0xFF5C665F)

// Dark
internal val DarkBackground = Color(0xFF14181A)
internal val DarkSurface = Color(0xFF1B2123)
internal val DarkSurfaceVariant = Color(0xFF20272A)
internal val DarkOutline = Color(0xFF2A3234)
internal val DarkOnBackground = Color(0xFFEBEEEC)
internal val DarkOnSurfaceMuted = Color(0xFF93A19B)

// Action (effort) — orange/red family
internal val ActionLight = Color(0xFFE15A28)
internal val ActionDark = Color(0xFFFF7A45)
internal val ActionContainerLight = Color(0xFFFBE7DD)
internal val ActionContainerDark = Color(0xFF33241C)

// Recovery — teal family
internal val RecoveryLight = Color(0xFF1C7A6E)
internal val RecoveryDark = Color(0xFF48C2B2)
internal val RecoveryContainerLight = Color(0xFFE4F1EE)
internal val RecoveryContainerDark = Color(0xFF1B3230)

internal val ErrorLight = Color(0xFFBA1A1A)
internal val ErrorDark = Color(0xFFFFB4AB)
