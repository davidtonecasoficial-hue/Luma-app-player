package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.data.model.AccentColorOption
import com.example.data.model.GlassQuality
import com.example.data.model.ThemeMode

@Immutable
data class LiquidGlassStyle(
    val isDark: Boolean,
    val glassQuality: GlassQuality,
    val reduceMotion: Boolean,
    val hapticsEnabled: Boolean,
    val glassFillTop: Color,
    val glassFillBottom: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val glassHighlight: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val accentGlow: Color,
    val backdropTop: Color,
    val backdropBottom: Color
)

val LocalLiquidGlassStyle = staticCompositionLocalOf {
    LiquidGlassStyle(
        isDark = true,
        glassQuality = GlassQuality.HIGH,
        reduceMotion = false,
        hapticsEnabled = true,
        glassFillTop = Color(0x36FFFFFF),
        glassFillBottom = Color(0x14FFFFFF),
        glassBorderTop = Color(0x55FFFFFF),
        glassBorderBottom = Color(0x18FFFFFF),
        glassHighlight = Color(0x28FFFFFF),
        accentPrimary = Color(0xFF54A8FF),
        accentSecondary = Color(0xFF7B61FF),
        accentGlow = Color(0xFF38BDF8),
        backdropTop = DarkIndigoDepth,
        backdropBottom = DarkSpaceBg
    )
}

private val LumaShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun LumaPlayerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AccentColorOption = AccentColorOption.LIQUID_BLUE,
    glassQuality: GlassQuality = GlassQuality.HIGH,
    reduceMotion: Boolean = false,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val accent = accentPaletteFor(accentColor, isDark)
    val animSpec = tween<Color>(durationMillis = if (reduceMotion) 0 else 350)

    val bgColor by animateColorAsState(
        targetValue = if (isDark) DarkSpaceBg else LightMistBg,
        animationSpec = animSpec,
        label = "bg"
    )
    val surfaceColor by animateColorAsState(
        targetValue = if (isDark) DarkIndigoDepth else LightPorcelain,
        animationSpec = animSpec,
        label = "surface"
    )
    val surfaceVariantColor by animateColorAsState(
        targetValue = if (isDark) DarkSurfaceGlass else LightGlassTint,
        animationSpec = animSpec,
        label = "surfaceVariant"
    )
    val onBgColor by animateColorAsState(
        targetValue = if (isDark) DarkTextPrimary else LightTextPrimary,
        animationSpec = animSpec,
        label = "onBg"
    )
    val onSurfaceVariantColor by animateColorAsState(
        targetValue = if (isDark) DarkTextSecondary else LightTextSecondary,
        animationSpec = animSpec,
        label = "onSurfaceVar"
    )
    val primaryColor by animateColorAsState(
        targetValue = accent.primary,
        animationSpec = animSpec,
        label = "primary"
    )
    val secondaryColor by animateColorAsState(
        targetValue = accent.secondary,
        animationSpec = animSpec,
        label = "secondary"
    )

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = accent.onPrimary,
            secondary = secondaryColor,
            onSecondary = Color.White,
            tertiary = accent.glow,
            background = bgColor,
            onBackground = onBgColor,
            surface = surfaceColor,
            onSurface = onBgColor,
            surfaceVariant = surfaceVariantColor,
            onSurfaceVariant = onSurfaceVariantColor,
            outline = Color(0x44FFFFFF),
            error = Color(0xFFFF6B81),
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = accent.onPrimary,
            secondary = secondaryColor,
            onSecondary = Color.White,
            tertiary = accent.glow,
            background = bgColor,
            onBackground = onBgColor,
            surface = surfaceColor,
            onSurface = onBgColor,
            surfaceVariant = surfaceVariantColor,
            onSurfaceVariant = onSurfaceVariantColor,
            outline = Color(0x3310182C),
            error = Color(0xFFDC2626),
            onError = Color.White
        )
    }

    val glassStyle = when {
        isDark && glassQuality == GlassQuality.REDUCED -> LiquidGlassStyle(
            isDark = true,
            glassQuality = glassQuality,
            reduceMotion = reduceMotion,
            hapticsEnabled = hapticsEnabled,
            glassFillTop = Color(0xE6141F3D),
            glassFillBottom = Color(0xE60D152B),
            glassBorderTop = Color(0x66FFFFFF),
            glassBorderBottom = Color(0x28FFFFFF),
            glassHighlight = Color(0x22FFFFFF),
            accentPrimary = primaryColor,
            accentSecondary = secondaryColor,
            accentGlow = accent.glow,
            backdropTop = DarkIndigoDepth,
            backdropBottom = DarkSpaceBg
        )
        isDark -> LiquidGlassStyle(
            isDark = true,
            glassQuality = glassQuality,
            reduceMotion = reduceMotion,
            hapticsEnabled = hapticsEnabled,
            glassFillTop = Color(0x32FFFFFF),
            glassFillBottom = Color(0x142A3E7A),
            glassBorderTop = Color(0x66FFFFFF),
            glassBorderBottom = Color(0x1CFFFFFF),
            glassHighlight = Color(0x2AFFFFFF),
            accentPrimary = primaryColor,
            accentSecondary = secondaryColor,
            accentGlow = accent.glow,
            backdropTop = DarkIndigoDepth,
            backdropBottom = DarkSpaceBg
        )
        !isDark && glassQuality == GlassQuality.REDUCED -> LiquidGlassStyle(
            isDark = false,
            glassQuality = glassQuality,
            reduceMotion = reduceMotion,
            hapticsEnabled = hapticsEnabled,
            glassFillTop = Color(0xF5FFFFFF),
            glassFillBottom = Color(0xF0F1F5FD),
            glassBorderTop = Color(0x4010182C),
            glassBorderBottom = Color(0x2210182C),
            glassHighlight = Color(0x40FFFFFF),
            accentPrimary = primaryColor,
            accentSecondary = secondaryColor,
            accentGlow = accent.glow,
            backdropTop = Color(0xFFE9F0FC),
            backdropBottom = LightMistBg
        )
        else -> LiquidGlassStyle(
            isDark = false,
            glassQuality = glassQuality,
            reduceMotion = reduceMotion,
            hapticsEnabled = hapticsEnabled,
            glassFillTop = Color(0xCCFFFFFF),
            glassFillBottom = Color(0x94E7EFFD),
            glassBorderTop = Color(0xF0FFFFFF),
            glassBorderBottom = Color(0x407A96C8),
            glassHighlight = Color(0x70FFFFFF),
            accentPrimary = primaryColor,
            accentSecondary = secondaryColor,
            accentGlow = accent.glow,
            backdropTop = Color(0xFFE2ECFD),
            backdropBottom = LightMistBg
        )
    }

    CompositionLocalProvider(LocalLiquidGlassStyle provides glassStyle) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LumaTypography,
            shapes = LumaShapes,
            content = content
        )
    }
}
