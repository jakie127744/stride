package com.stride.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val StrideLightColorScheme: ColorScheme = lightColorScheme(
    primary = ActionLight,
    onPrimary = Color.White,
    primaryContainer = ActionContainerLight,
    secondary = RecoveryLight,
    onSecondary = Color.White,
    secondaryContainer = RecoveryContainerLight,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceMuted,
    outline = LightOutline,
    error = ErrorLight,
)

private val StrideDarkColorScheme: ColorScheme = darkColorScheme(
    primary = ActionDark,
    onPrimary = Color(0xFF14181A),
    primaryContainer = ActionContainerDark,
    secondary = RecoveryDark,
    onSecondary = Color(0xFF14181A),
    secondaryContainer = RecoveryContainerDark,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceMuted,
    outline = DarkOutline,
    error = ErrorDark,
)

/**
 * @param useDynamicColor Material You opt-in — off by default. Stride's brand palette carries
 * real meaning (warm = effort, cool = recovery); a wallpaper-derived scheme would blur that.
 * See docs/foundation.md "Design system".
 */
@Composable
fun StrideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> StrideDarkColorScheme
        else -> StrideLightColorScheme
    }
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val reducedMotion = rememberReducedMotion()

    CompositionLocalProvider(
        LocalStrideExtendedColors provides extendedColors,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StrideTypography,
            content = content,
        )
    }
}

/** `StrideTheme.extendedColors` reads more naturally at call sites than importing the
 * CompositionLocal directly — mirrors how `MaterialTheme.colorScheme` is normally reached for. */
object StrideThemeExtras {
    val extendedColors: StrideExtendedColors
        @Composable get() = LocalStrideExtendedColors.current
}
