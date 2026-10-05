package com.fourdfit.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** 4D FIT palette. Accents are shared by rings, glows and the kinetic figure. */
@Immutable
data class FourDColors(
    val isDark: Boolean,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val orbA: Color,
    val orbB: Color,
    val orbC: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val glassFill: Color,
    val glassFillLow: Color,
    val glassFillStrong: Color,
    val glassBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val cyan: Color,
    val violet: Color,
    val pink: Color,
    val mint: Color,
    val amber: Color,
    val blue: Color,
    val ringTrack: Color,
    val danger: Color,
    val gridLine: Color,
    val onAccent: Color,
) {
    /** Primary action gradient; white text keeps >= 3:1 contrast at button sizes. */
    val primaryGradient: List<Color> get() = listOf(violet, blue)
    val glowColors: List<Color> get() = listOf(cyan, violet, pink, cyan)
}

val DarkPalette =
    FourDColors(
        isDark = true,
        backgroundTop = Color(0xFF070B18),
        backgroundBottom = Color(0xFF0C0820),
        orbA = Color(0xFF00D4FF),
        orbB = Color(0xFF7C4DFF),
        orbC = Color(0xFFFF3FD1),
        surface = Color(0xFF0E1324),
        surfaceHigh = Color(0xFF182040),
        glassFill = Color(0x17FFFFFF),
        glassFillLow = Color(0x08FFFFFF),
        glassFillStrong = Color(0xE6121830),
        glassBorder = Color(0x24FFFFFF),
        textPrimary = Color(0xFFF5F7FF),
        textSecondary = Color(0xFFB4BCD8),
        textMuted = Color(0xFF8A93B3),
        cyan = Color(0xFF22E4FF),
        violet = Color(0xFF7C5CFF),
        pink = Color(0xFFFF5AD9),
        mint = Color(0xFF3DF5B0),
        amber = Color(0xFFFFC15A),
        blue = Color(0xFF3F7BFF),
        ringTrack = Color(0x14FFFFFF),
        danger = Color(0xFFFF6B81),
        gridLine = Color(0x1222E4FF),
        onAccent = Color(0xFF061018),
    )

val LightPalette =
    FourDColors(
        isDark = false,
        backgroundTop = Color(0xFFF4F6FF),
        backgroundBottom = Color(0xFFE9E6FF),
        orbA = Color(0xFF7FE9FF),
        orbB = Color(0xFFB9A6FF),
        orbC = Color(0xFFFFB3EE),
        surface = Color(0xFFFFFFFF),
        surfaceHigh = Color(0xFFF0EEFF),
        glassFill = Color(0xB8FFFFFF),
        glassFillLow = Color(0x73FFFFFF),
        glassFillStrong = Color(0xF2FFFFFF),
        glassBorder = Color(0x2E5B3FE0),
        textPrimary = Color(0xFF0D1230),
        textSecondary = Color(0xFF3F4766),
        textMuted = Color(0xFF5E6585),
        cyan = Color(0xFF007FA0),
        violet = Color(0xFF5B3FE0),
        pink = Color(0xFFB0158C),
        mint = Color(0xFF007F5C),
        amber = Color(0xFF955900),
        blue = Color(0xFF2F62E0),
        ringTrack = Color(0x140D1230),
        danger = Color(0xFFC62845),
        gridLine = Color(0x0F5B3FE0),
        onAccent = Color(0xFFFFFFFF),
    )

val HighContrastDarkPalette =
    DarkPalette.copy(
        backgroundTop = Color(0xFF000000),
        backgroundBottom = Color(0xFF000000),
        orbA = Color.Transparent,
        orbB = Color.Transparent,
        orbC = Color.Transparent,
        surface = Color(0xFF0A0A0A),
        surfaceHigh = Color(0xFF1A1A1A),
        glassFill = Color(0xFF121212),
        glassFillLow = Color(0xFF0C0C0C),
        glassFillStrong = Color(0xFF121212),
        glassBorder = Color(0xCCFFFFFF),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFF0F0F0),
        textMuted = Color(0xFFDADADA),
        cyan = Color(0xFF6CF3FF),
        violet = Color(0xFFB9A3FF),
        pink = Color(0xFFFF9BEA),
        mint = Color(0xFF7DFFCC),
        amber = Color(0xFFFFD88A),
        blue = Color(0xFF9DC0FF),
        ringTrack = Color(0x40FFFFFF),
        danger = Color(0xFFFF9AA8),
        gridLine = Color.Transparent,
    )

val HighContrastLightPalette =
    LightPalette.copy(
        backgroundTop = Color(0xFFFFFFFF),
        backgroundBottom = Color(0xFFFFFFFF),
        orbA = Color.Transparent,
        orbB = Color.Transparent,
        orbC = Color.Transparent,
        glassFill = Color(0xFFFFFFFF),
        glassFillLow = Color(0xFFFFFFFF),
        glassFillStrong = Color(0xFFFFFFFF),
        glassBorder = Color(0xFF000000),
        textPrimary = Color(0xFF000000),
        textSecondary = Color(0xFF111111),
        textMuted = Color(0xFF2A2A2A),
        cyan = Color(0xFF005C75),
        violet = Color(0xFF3A1FB8),
        pink = Color(0xFF7D0A62),
        mint = Color(0xFF005C42),
        amber = Color(0xFF6B4000),
        blue = Color(0xFF1A3FA8),
        ringTrack = Color(0x33000000),
        danger = Color(0xFF9E0020),
        gridLine = Color.Transparent,
    )
