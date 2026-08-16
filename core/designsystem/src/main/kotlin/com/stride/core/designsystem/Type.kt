package com.stride.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// TODO(Phase 7): swap FontFamily.Default for the condensed display face + humanist body face
// specced in docs/foundation.md once licensed/variable font assets are sourced. System default
// keeps every screen buildable and readable in the meantime — never block on missing assets.

private val displayFontFamily = FontFamily.Default
private val bodyFontFamily = FontFamily.Default

val StrideTypography = Typography(
    displayLarge = TextStyle(fontFamily = displayFontFamily, fontWeight = FontWeight.Bold, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = displayFontFamily, fontWeight = FontWeight.Bold, fontSize = 45.sp),
    headlineLarge = TextStyle(fontFamily = displayFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = displayFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    titleLarge = TextStyle(fontFamily = displayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = bodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = bodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = bodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = bodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = bodyFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

/**
 * Live run numerals (pace, distance, time) MUST use tabular figures — a proportional "1" and
 * "4" jitter the layout every tick otherwise. See docs/foundation.md "Color & typography".
 */
val TabularNumeralsStyle = TextStyle(
    fontFeatureSettings = "tnum",
    fontWeight = FontWeight.Bold,
)
