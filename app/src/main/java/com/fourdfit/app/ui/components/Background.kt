package com.fourdfit.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.fourdfit.app.ui.theme.FourD
import com.fourdfit.app.ui.theme.FourDColors
import kotlin.math.PI
import kotlin.math.sin

/**
 * The app's ambient backdrop: deep gradient, three slowly drifting light orbs and a faint
 * perspective floor grid. Animation is read in the draw phase only (no recomposition) and
 * stops entirely when Reduce motion is on.
 */
@Composable
fun FourDBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = FourD.colors
    val reduce = FourD.reduceMotion
    val phase: State<Float> =
        if (reduce) {
            remember { mutableFloatStateOf(0.35f) }
        } else {
            rememberInfiniteTransition(label = "background").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(22_000, easing = LinearEasing), RepeatMode.Reverse),
                label = "backgroundPhase",
            )
        }
    Box(
        modifier =
            modifier
                .background(Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom)))
                .drawBehind { drawAmbient(colors, phase.value) },
        content = content,
    )
}

private fun DrawScope.drawAmbient(
    colors: FourDColors,
    p: Float,
) {
    val w = size.width
    val h = size.height
    val wave = sin(p * 2 * PI).toFloat()
    val orbAlpha = if (colors.isDark) 0.30f else 0.45f

    fun orb(
        color: Color,
        center: Offset,
        radius: Float,
    ) {
        if (color.alpha == 0f) return
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = orbAlpha), Color.Transparent), center, radius),
            radius = radius,
            center = center,
        )
    }
    orb(colors.orbA, Offset(w * (0.10f + 0.25f * p), h * (0.10f + 0.04f * wave)), w * 0.85f)
    orb(colors.orbB, Offset(w * (0.95f - 0.20f * p), h * (0.38f + 0.06f * wave)), w * 0.9f)
    orb(colors.orbC, Offset(w * (0.30f + 0.30f * p), h * (0.92f - 0.05f * wave)), w * 0.75f)

    // Perspective floor grid — the "fourth dimension" of the brand, kept very faint.
    if (colors.gridLine.alpha > 0f) {
        val horizon = h * 0.62f
        val vanishing = Offset(w / 2f, horizon)
        val lines = 14
        for (i in 0..lines) {
            val x = -w * 0.6f + (w * 2.2f) * i / lines
            drawLine(colors.gridLine, vanishing, Offset(x, h), strokeWidth = 1f)
        }
        val drift = (p * 6f) % 1f
        for (j in 0 until 7) {
            val t = ((j + drift) / 7f)
            val y = horizon + (h - horizon) * t * t
            drawLine(colors.gridLine.copy(alpha = colors.gridLine.alpha * t), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
    }
}
