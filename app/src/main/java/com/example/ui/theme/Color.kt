package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.AccentColorOption

// Dark Liquid Glass Base Palette
val DarkSpaceBg = Color(0xFF060B18)
val DarkIndigoDepth = Color(0xFF0C1633)
val DarkSurfaceGlass = Color(0xFF132042)
val DarkTextPrimary = Color(0xFFF6F8FF)
val DarkTextSecondary = Color(0xFFB3C0DE)

// Light Liquid Glass Base Palette
val LightMistBg = Color(0xFFF3F6FC)
val LightPorcelain = Color(0xFFFFFFFF)
val LightGlassTint = Color(0xFFE7EEF9)
val LightTextPrimary = Color(0xFF10182C)
val LightTextSecondary = Color(0xFF53617F)

// Accent Tokens
data class AccentPalette(
    val primary: Color,
    val secondary: Color,
    val glow: Color,
    val onPrimary: Color
)

fun accentPaletteFor(option: AccentColorOption, isDark: Boolean): AccentPalette = when (option) {
    AccentColorOption.LIQUID_BLUE -> AccentPalette(
        primary = if (isDark) Color(0xFF54A8FF) else Color(0xFF1E6BFF),
        secondary = if (isDark) Color(0xFF7B61FF) else Color(0xFF5B4BFF),
        glow = Color(0xFF38BDF8),
        onPrimary = Color.White
    )
    AccentColorOption.ELECTRIC_VIOLET -> AccentPalette(
        primary = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9),
        secondary = if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6),
        glow = Color(0xFFC084FC),
        onPrimary = Color.White
    )
    AccentColorOption.CYAN_MIST -> AccentPalette(
        primary = if (isDark) Color(0xFF22D3EE) else Color(0xFF0891B2),
        secondary = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
        glow = Color(0xFF67E8F9),
        onPrimary = if (isDark) Color(0xFF06202A) else Color.White
    )
    AccentColorOption.CORAL_SUNSET -> AccentPalette(
        primary = if (isDark) Color(0xFFFF7A45) else Color(0xFFEA580C),
        secondary = if (isDark) Color(0xFFFFB26B) else Color(0xFFF59E0B),
        glow = Color(0xFFFB923C),
        onPrimary = Color.White
    )
    AccentColorOption.EMERALD -> AccentPalette(
        primary = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
        secondary = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488),
        glow = Color(0xFF6EE7B7),
        onPrimary = if (isDark) Color(0xFF052E1E) else Color.White
    )
}
