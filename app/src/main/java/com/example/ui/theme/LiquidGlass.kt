package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.GlassQuality
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    dynamicPrimary: Color? = null,
    dynamicSecondary: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val style = LocalLiquidGlassStyle.current
    val animSpec = tween<Color>(durationMillis = if (style.reduceMotion) 0 else 550)

    val orb1 by animateColorAsState(
        targetValue = (dynamicPrimary ?: style.accentPrimary).copy(alpha = if (style.isDark) 0.42f else 0.22f),
        animationSpec = animSpec,
        label = "orb1"
    )
    val orb2 by animateColorAsState(
        targetValue = (dynamicSecondary ?: style.accentSecondary).copy(alpha = if (style.isDark) 0.36f else 0.18f),
        animationSpec = animSpec,
        label = "orb2"
    )
    val orb3 by animateColorAsState(
        targetValue = style.accentGlow.copy(alpha = if (style.isDark) 0.22f else 0.14f),
        animationSpec = animSpec,
        label = "orb3"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(style.backdropTop, style.backdropBottom)
                )
            )
    ) {
        val blurModifier = if (
            style.glassQuality == GlassQuality.HIGH &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            Modifier.blur(64.dp)
        } else {
            Modifier
        }

        Canvas(modifier = Modifier.fillMaxSize().then(blurModifier)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb1, Color.Transparent),
                    center = Offset(size.width * 0.18f, size.height * 0.16f),
                    radius = size.minDimension * 0.72f
                ),
                radius = size.minDimension * 0.72f,
                center = Offset(size.width * 0.18f, size.height * 0.16f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb2, Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.42f),
                    radius = size.minDimension * 0.65f
                ),
                radius = size.minDimension * 0.65f,
                center = Offset(size.width * 0.85f, size.height * 0.42f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb3, Color.Transparent),
                    center = Offset(size.width * 0.35f, size.height * 0.88f),
                    radius = size.minDimension * 0.6f
                ),
                radius = size.minDimension * 0.6f,
                center = Offset(size.width * 0.35f, size.height * 0.88f)
            )
        }
        content()
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    onClick: (() -> Unit)? = null,
    tintColor: Color? = null,
    elevation: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val style = LocalLiquidGlassStyle.current
    val haptics = LocalHapticFeedback.current

    val fillBrush = if (tintColor != null) {
        Brush.linearGradient(
            colors = listOf(
                tintColor.copy(alpha = if (style.isDark) 0.42f else 0.78f),
                style.glassFillBottom
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(style.glassFillTop, style.glassFillBottom)
        )
    }

    val borderBrush = Brush.linearGradient(
        colors = listOf(style.glassBorderTop, style.glassBorderBottom)
    )

    val shadowMod = if (style.glassQuality != GlassQuality.REDUCED) {
        Modifier.shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = style.accentPrimary.copy(alpha = 0.22f),
            spotColor = style.accentSecondary.copy(alpha = 0.25f)
        )
    } else {
        Modifier
    }

    val clickMod = if (onClick != null) {
        Modifier.clickable(role = Role.Button) {
            if (style.hapticsEnabled) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            onClick()
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(shadowMod)
            .clip(shape)
            .background(fillBrush)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .then(clickMod),
        content = content
    )
}

data class NavTabItem(
    val key: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun LiquidGlassFloatingNavBar(
    tabs: List<NavTabItem>,
    selectedKey: String,
    onSelect: (NavTabItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val style = LocalLiquidGlassStyle.current
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            elevation = 14.dp,
            tintColor = if (style.isDark) Color(0xFF152550) else Color(0xFFF6F9FF)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = tab.key == selectedKey
                    val pillColor by animateColorAsState(
                        targetValue = if (selected) {
                            style.accentPrimary.copy(alpha = if (style.isDark) 0.34f else 0.16f)
                        } else {
                            Color.Transparent
                        },
                        animationSpec = tween(if (style.reduceMotion) 0 else 220),
                        label = "navPill"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) {
                            if (style.isDark) Color.White else style.accentPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = tween(if (style.reduceMotion) 0 else 220),
                        label = "navTint"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(pillColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (style.hapticsEnabled) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onSelect(tab)
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag(tab.testTag),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidGlassNavRail(
    tabs: List<NavTabItem>,
    selectedKey: String,
    onSelect: (NavTabItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val style = LocalLiquidGlassStyle.current
    Surface(
        modifier = modifier,
        color = if (style.isDark) Color(0xCC0D1633) else Color(0xCCF3F6FC),
        tonalElevation = 4.dp
    ) {
        NavigationRail(
            containerColor = Color.Transparent,
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            tabs.forEach { tab ->
                val selected = tab.key == selectedKey
                NavigationRailItem(
                    selected = selected,
                    onClick = { onSelect(tab) },
                    icon = {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label
                        )
                    },
                    label = {
                        Text(
                            text = tab.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag(tab.testTag)
                )
            }
        }
    }
}

@Composable
fun InteractiveWaveformScrubber(
    progress: Float,
    isPlaying: Boolean,
    seedKey: Long,
    showWaveform: Boolean,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val style = LocalLiquidGlassStyle.current
    val clampedProgress = progress.coerceIn(0f, 1f)
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val activeFraction = if (dragFraction >= 0f) dragFraction else clampedProgress

    val barHeights = remember(seedKey) {
        val count = 44
        List(count) { i ->
            val phase = (seedKey % 97) * 0.13f + i * 0.48f
            val wave = (sin(phase) * 0.5f + 0.5f) * 0.65f +
                (abs(sin(phase * 1.9f)) * 0.35f)
            wave.coerceIn(0.18f, 1.0f)
        }
    }

    val activeColor = style.accentPrimary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(seedKey) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeekFraction(frac)
                }
            }
            .pointerInput(seedKey) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        if (dragFraction >= 0f) {
                            onSeekFraction(dragFraction)
                        }
                        dragFraction = -1f
                    },
                    onDragCancel = { dragFraction = -1f }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (!showWaveform) {
                val trackY = size.height / 2f
                val trackH = 5.dp.toPx()
                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset(0f, trackY - trackH / 2f),
                    size = Size(size.width, trackH),
                    cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
                )
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, trackY - trackH / 2f),
                    size = Size(size.width * activeFraction, trackH),
                    cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 7.dp.toPx(),
                    center = Offset(size.width * activeFraction, trackY)
                )
            } else {
                val count = barHeights.size
                val slotWidth = size.width / count
                val barWidth = (slotWidth * 0.56f).coerceAtLeast(2.dp.toPx())
                val maxH = size.height * 0.85f
                val centerY = size.height / 2f

                barHeights.forEachIndexed { idx, amp ->
                    val frac = idx.toFloat() / count.toFloat()
                    val played = frac <= activeFraction
                    val pulse = if (isPlaying && !style.reduceMotion && abs(frac - activeFraction) < 0.08f) {
                        1.14f
                    } else {
                        1.0f
                    }
                    val h = (amp * maxH * pulse).coerceAtMost(size.height)
                    val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                    drawRoundRect(
                        color = if (played) activeColor else inactiveColor,
                        topLeft = Offset(x, centerY - h / 2f),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }
    }
}
