package com.fourdfit.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fourdfit.app.domain.model.DayActivity
import com.fourdfit.app.ui.theme.FourD
import com.fourdfit.app.utils.DateUtils
import java.time.LocalDate

/** Circular gradient progress indicator with glow. [progress] is 0..1. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 120.dp,
    stroke: Dp = 12.dp,
    colors: List<Color> = FourD.colors.let { listOf(it.cyan, it.violet) },
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val palette = FourD.colors
    val reduce = FourD.reduceMotion
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = if (reduce) snap() else tween(1200, easing = FastOutSlowInEasing),
        label = "ring",
    )
    Box(
        modifier =
            modifier
                .size(diameter)
                .semantics(mergeDescendants = true) {
                    if (contentDescription != null) this.contentDescription = contentDescription
                    progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f)
                },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2f + sw * 0.35f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(palette.ringTrack, 0f, 360f, false, topLeft, arcSize, style = Stroke(sw))
            val sweep = 360f * animated
            if (sweep > 0.5f) {
                val brush = Brush.sweepGradient(colors + colors.first())
                rotate(-90f) {
                    drawArc(brush, 0f, sweep, false, topLeft, arcSize, alpha = 0.28f, style = Stroke(sw * 1.9f, cap = StrokeCap.Round))
                    drawArc(brush, 0f, sweep, false, topLeft, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
                }
            }
        }
        content()
    }
}

/** Thin horizontal meter (used for wellness goals and plan progress). */
@Composable
fun LinearMeter(
    progress: Float,
    modifier: Modifier = Modifier,
    colors: List<Color> = FourD.colors.let { listOf(it.cyan, it.violet) },
    height: Dp = 8.dp,
) {
    val reduce = FourD.reduceMotion
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (reduce) snap() else tween(900, easing = FastOutSlowInEasing),
        label = "meter",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(FourD.colors.ringTrack),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Brush.horizontalGradient(colors)),
        )
    }
}

/**
 * Animated bar chart of active minutes for the last 7 days. Values are printed above each bar and
 * the whole chart has a spoken summary, so it does not rely on colour or shape alone.
 */
@Composable
fun WeeklyBarChart(
    days: List<DayActivity>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 140.dp,
) {
    val colors = FourD.colors
    val today = LocalDate.now()
    val max = (days.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(10)
    val summary = days.joinToString { "${it.date.format(DateUtils.shortDay)} ${it.minutes} minutes" }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = "Active minutes, last 7 days: $summary" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            val target = day.minutes.toFloat() / max
            val reduce = FourD.reduceMotion
            val fraction by animateFloatAsState(
                targetValue = target,
                animationSpec = if (reduce) snap() else tween(900, easing = FastOutSlowInEasing),
                label = "bar",
            )
            val isToday = day.date == today
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (day.minutes > 0) "${day.minutes}" else "–",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) colors.textPrimary else colors.textMuted,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.62f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.ringTrack),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.62f)
                            .fillMaxHeight(fraction.coerceAtLeast(if (day.minutes > 0) 0.06f else 0f))
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.verticalGradient(
                                    if (isToday) {
                                        listOf(
                                            colors.cyan,
                                            colors.violet,
                                        )
                                    } else {
                                        listOf(colors.violet, colors.blue.copy(alpha = 0.7f))
                                    },
                                ),
                            ),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = day.date.format(DateUtils.shortDay).take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) colors.cyan else colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Four-week activity grid. Intensity shows minutes; active days also carry a check mark so the
 * information is readable without colour.
 */
@Composable
fun MonthlyHeatmap(
    days: List<DayActivity>,
    modifier: Modifier = Modifier,
) {
    val colors = FourD.colors
    val today = LocalDate.now()
    val active = days.count { it.workouts > 0 }
    Column(
        modifier =
            modifier.clearAndSetSemantics {
                contentDescription = "Active on $active of the last ${days.size} days"
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        days.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val intensity = (day.minutes / 30f).coerceIn(0f, 1f)
                    val fill =
                        if (day.workouts > 0) {
                            Brush.linearGradient(
                                listOf(
                                    colors.violet.copy(alpha = 0.35f + 0.65f * intensity),
                                    colors.cyan.copy(alpha = 0.25f + 0.6f * intensity),
                                ),
                            )
                        } else {
                            Brush.linearGradient(listOf(colors.ringTrack, colors.ringTrack))
                        }
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(fill)
                                .then(
                                    if (day.date ==
                                        today
                                    ) {
                                        Modifier.border(1.5.dp, colors.textPrimary, RoundedCornerShape(8.dp))
                                    } else {
                                        Modifier
                                    },
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day.workouts > 0) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        } else {
                            Text(
                                text = day.date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}
