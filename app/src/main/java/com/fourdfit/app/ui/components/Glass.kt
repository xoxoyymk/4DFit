package com.fourdfit.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fourdfit.app.ui.theme.FourD
import com.fourdfit.app.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

val CardShape = RoundedCornerShape(26.dp)

/**
 * Animated neon border: a gradient that slowly orbits the shape's outline.
 * The angle is read only in the draw phase, so it never triggers recomposition.
 */
fun Modifier.glowBorder(
    shape: Shape,
    colors: List<Color>,
    width: Dp = 1.5.dp,
    animated: Boolean = true,
    periodMillis: Int = 7000,
): Modifier =
    composed {
        val reduce = LocalReduceMotion.current
        val angle: State<Float> =
            if (animated && !reduce) {
                rememberInfiniteTransition(label = "glowBorder").animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing), RepeatMode.Restart),
                    label = "glowAngle",
                )
            } else {
                remember { mutableFloatStateOf(35f) }
            }
        drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val stroke = Stroke(width.toPx() * 2f) // half is clipped by the shape
            onDrawWithContent {
                drawContent()
                val radians = Math.toRadians(angle.value.toDouble())
                val r = max(size.width, size.height) / 2f
                val c = Offset(size.width / 2f, size.height / 2f)
                val start = Offset(c.x + (cos(radians) * r).toFloat(), c.y + (sin(radians) * r).toFloat())
                val end = Offset(c.x - (cos(radians) * r).toFloat(), c.y - (sin(radians) * r).toFloat())
                drawOutline(outline, Brush.linearGradient(colors, start, end), style = stroke)
            }
        }
    }

/** Subtle squash on press so taps feel physical. */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f,
): Modifier =
    composed {
        val pressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) pressedScale else 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
            label = "pressScale",
        )
        graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    }

/**
 * One-time "depth" entrance: content tilts up from below on first composition.
 * Use on the first few items of a screen only — it is the screen's single orchestrated moment.
 */
fun Modifier.entrance(index: Int = 0): Modifier =
    composed {
        val reduce = LocalReduceMotion.current
        val progress = remember { Animatable(if (reduce) 1f else 0f) }
        val density = LocalDensity.current
        LaunchedEffect(Unit) {
            if (!reduce && progress.value < 1f) {
                delay(60L * index.coerceAtMost(6))
                progress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 220f))
            }
        }
        graphicsLayer {
            val p = progress.value
            alpha = p.coerceIn(0f, 1f)
            translationY = (1f - p) * with(density) { 36.dp.toPx() }
            rotationX = (1f - p) * 14f
            cameraDistance = 14f * density.density
        }
    }

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    glow: Boolean = false,
    glowColors: List<Color>? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    strong: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = FourD.colors
    val interaction = remember { MutableInteractionSource() }
    val fill =
        if (strong) {
            Brush.linearGradient(listOf(colors.glassFillStrong, colors.glassFillStrong))
        } else {
            Brush.linearGradient(listOf(colors.glassFill, colors.glassFillLow))
        }
    val edge =
        if (glow) {
            Modifier.glowBorder(shape, glowColors ?: colors.glowColors)
        } else {
            Modifier.border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        colors.glassBorder,
                        colors.glassBorder.copy(
                            alpha =
                                colors.glassBorder.alpha * 0.4f,
                        ),
                    ),
                ),
                shape,
            )
        }
    Column(
        modifier =
            modifier
                .then(if (onClick != null) Modifier.pressScale(interaction) else Modifier)
                .clip(shape)
                .background(fill)
                .then(edge)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interaction,
                            indication = LocalIndication.current,
                            onClickLabel = onClickLabel,
                            role = Role.Button,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                ).padding(contentPadding),
        content = content,
    )
}
