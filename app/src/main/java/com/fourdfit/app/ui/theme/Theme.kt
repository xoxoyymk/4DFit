package com.fourdfit.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fourdfit.app.domain.model.ThemeMode

val LocalFourDColors = staticCompositionLocalOf { DarkPalette }

/** When true, decorative and looping animations are disabled (Settings › Reduce motion). */
val LocalReduceMotion = staticCompositionLocalOf { false }

object FourD {
    val colors: FourDColors
        @Composable @ReadOnlyComposable
        get() = LocalFourDColors.current
    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalReduceMotion.current
}

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean =
    when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

private val FourDShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(26.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

private fun FourDColors.toColorScheme(): ColorScheme =
    if (isDark) {
        darkColorScheme(
            primary = violet,
            onPrimary = Color.White,
            primaryContainer = surfaceHigh,
            onPrimaryContainer = textPrimary,
            secondary = cyan,
            onSecondary = onAccent,
            secondaryContainer = surfaceHigh,
            onSecondaryContainer = textPrimary,
            tertiary = pink,
            onTertiary = onAccent,
            background = backgroundTop,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceHigh,
            onSurfaceVariant = textSecondary,
            surfaceTint = violet,
            outline = textMuted,
            outlineVariant = glassBorder,
            error = danger,
            onError = Color.Black,
            surfaceContainerLowest = backgroundTop,
            surfaceContainerLow = surface,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceHigh,
            surfaceContainerHighest = surfaceHigh,
            surfaceBright = surfaceHigh,
            surfaceDim = backgroundTop,
        )
    } else {
        lightColorScheme(
            primary = violet,
            onPrimary = Color.White,
            primaryContainer = surfaceHigh,
            onPrimaryContainer = textPrimary,
            secondary = cyan,
            onSecondary = Color.White,
            secondaryContainer = surfaceHigh,
            onSecondaryContainer = textPrimary,
            tertiary = pink,
            onTertiary = Color.White,
            background = backgroundTop,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceHigh,
            onSurfaceVariant = textSecondary,
            surfaceTint = violet,
            outline = textMuted,
            outlineVariant = glassBorder,
            error = danger,
            onError = Color.White,
            surfaceContainerLowest = surface,
            surfaceContainerLow = surface,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceHigh,
            surfaceContainerHighest = surfaceHigh,
            surfaceBright = surface,
            surfaceDim = surfaceHigh,
        )
    }

@Composable
fun FourDFitTheme(
    darkTheme: Boolean = true,
    highContrast: Boolean = false,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette =
        when {
            darkTheme && highContrast -> HighContrastDarkPalette
            darkTheme -> DarkPalette
            highContrast -> HighContrastLightPalette
            else -> LightPalette
        }
    CompositionLocalProvider(
        LocalFourDColors provides palette,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = FourDTypography,
            shapes = FourDShapes,
        ) {
            // The app draws its own backgrounds, so provide the default text colour explicitly.
            CompositionLocalProvider(LocalContentColor provides palette.textPrimary, content = content)
        }
    }
}
