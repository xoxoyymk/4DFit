package com.fourdfit.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fourdfit.app.domain.model.AchievementType
import com.fourdfit.app.domain.model.ZodiacSign
import com.fourdfit.app.ui.theme.FourD
import kotlinx.coroutines.launch
import kotlin.random.Random

/** One-shot confetti burst for the workout-complete moment. Skipped when Reduce motion is on. */
@Composable
fun ConfettiBurst(
    modifier: Modifier = Modifier,
    play: Boolean = true,
) {
    val c = FourD.colors
    if (FourD.reduceMotion || !play) return
    val particles =
        remember {
            val rng = Random(42)
            List(80) {
                Particle(
                    vx = rng.nextFloat() * 1.3f - 0.65f,
                    vy = -(0.35f + rng.nextFloat() * 0.75f),
                    spin = rng.nextFloat() * 2f - 1f,
                    size = 7f + rng.nextFloat() * 9f,
                    colorIndex = rng.nextInt(5),
                )
            }
        }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(2800, easing = LinearOutSlowInEasing)) }
    val palette = listOf(c.cyan, c.violet, c.pink, c.mint, c.amber)
    Canvas(modifier.clearAndSetSemantics { }) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas
        val origin = Offset(size.width / 2f, size.height * 0.32f)
        particles.forEach { p ->
            val x = origin.x + p.vx * size.width * t
            val y = origin.y + p.vy * size.height * 0.8f * t + 1.1f * size.height * t * t
            val w = p.size * density
            rotate(p.spin * 900f * t, Offset(x, y)) {
                drawRect(
                    color = palette[p.colorIndex].copy(alpha = (1f - t).coerceIn(0f, 1f)),
                    topLeft = Offset(x - w / 2f, y - w / 4f),
                    size = Size(w, w / 2f),
                )
            }
        }
    }
}

private data class Particle(
    val vx: Float,
    val vy: Float,
    val spin: Float,
    val size: Float,
    val colorIndex: Int,
)

fun achievementIcon(type: AchievementType): ImageVector =
    when (type) {
        AchievementType.FIRST_WORKOUT -> Icons.Rounded.Flag
        AchievementType.STREAK_7 -> Icons.Rounded.LocalFireDepartment
        AchievementType.TEN_WORKOUTS -> Icons.Rounded.FitnessCenter
        AchievementType.CONSISTENCY_30 -> Icons.Rounded.CalendarMonth
        AchievementType.MOBILITY_MASTER -> Icons.Rounded.SelfImprovement
        AchievementType.CARDIO_STARTER -> Icons.Rounded.Favorite
        AchievementType.WORKOUT_CHAMPION -> Icons.Rounded.EmojiEvents
    }

/**
 * Achievement medallion. With [animateUnlock] it spins in on a spring with a fading halo.
 * State is always stated in text ("Unlocked"/"Locked"), never by colour alone.
 */
@Composable
fun AchievementBadge(
    type: AchievementType,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
    animateUnlock: Boolean = false,
    diameter: Dp = 76.dp,
) {
    val c = FourD.colors
    val animate = animateUnlock && unlocked && !FourD.reduceMotion
    val scale = remember { Animatable(if (animate) 0.2f else 1f) }
    val turn = remember { Animatable(if (animate) -180f else 0f) }
    val halo = remember { Animatable(0f) }
    LaunchedEffect(animate) {
        if (animate) {
            launch { scale.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 170f)) }
            launch { turn.animateTo(0f, tween(950, easing = FastOutSlowInEasing)) }
            halo.snapTo(1f)
            halo.animateTo(0f, tween(1600, delayMillis = 250))
        }
    }
    val ring = if (unlocked) listOf(c.cyan, c.violet, c.pink, c.amber, c.cyan) else listOf(c.ringTrack, c.ringTrack)
    Column(
        modifier =
            modifier.clearAndSetSemantics {
                contentDescription = "${type.title}, ${if (unlocked) "unlocked" else "locked"}. ${type.description}"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(diameter)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    rotationY = turn.value
                    cameraDistance = 12f * density
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val h = halo.value
                if (h > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(c.amber.copy(alpha = 0.55f * h), Color.Transparent)),
                        radius = size.minDimension * (0.5f + 0.45f * (1f - h)),
                    )
                }
            }
            Box(
                Modifier
                    .size(diameter * 0.84f)
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(ring))
                    .alpha(if (unlocked) 1f else 0.7f),
            )
            Box(
                Modifier
                    .size(diameter * 0.70f)
                    .clip(CircleShape)
                    .background(if (unlocked) c.surfaceHigh else c.surface)
                    .border(1.dp, c.glassBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (unlocked) achievementIcon(type) else Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = if (unlocked) c.amber else c.textMuted,
                    modifier = Modifier.size(diameter * 0.32f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            type.title,
            style = MaterialTheme.typography.labelMedium,
            color = if (unlocked) c.textPrimary else c.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(diameter + 20.dp),
        )
        Text(
            if (unlocked) "Unlocked" else "Locked",
            style = MaterialTheme.typography.labelSmall,
            color = if (unlocked) c.mint else c.textMuted,
        )
    }
}

/** Zodiac glyph inside a slowly turning dashed orbit. */
@Composable
fun ZodiacEmblem(
    sign: ZodiacSign,
    modifier: Modifier = Modifier,
    diameter: Dp = 72.dp,
) {
    val c = FourD.colors
    val angle: State<Float> =
        if (FourD.reduceMotion) {
            remember { mutableFloatStateOf(0f) }
        } else {
            rememberInfiniteTransition(label = "emblem").animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
                label = "emblemAngle",
            )
        }
    Box(
        modifier
            .size(diameter)
            .clearAndSetSemantics { contentDescription = "${sign.displayName} symbol" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(listOf(c.violet.copy(alpha = 0.35f), Color.Transparent)),
                radius = r,
            )
            rotate(angle.value) {
                drawCircle(
                    brush = Brush.sweepGradient(listOf(c.cyan, c.violet, c.pink, c.cyan)),
                    radius = r * 0.88f,
                    style =
                        Stroke(
                            width = r * 0.05f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.12f, r * 0.08f)),
                        ),
                )
            }
            drawCircle(c.glassFill, radius = r * 0.70f)
        }
        Text(
            text = sign.symbol + "\uFE0E",
            color = c.textPrimary,
            fontSize = (diameter.value * 0.40f).sp,
        )
    }
}
