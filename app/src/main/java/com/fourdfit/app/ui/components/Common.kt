package com.fourdfit.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fourdfit.app.domain.model.Difficulty
import com.fourdfit.app.ui.theme.FourD
import java.io.File

/** Maximum content width so tablets get a comfortable reading column. */
val MaxContentWidth = 760.dp

/** Space reserved at the bottom of top-level screens for the floating navigation bar. */
val BottomBarClearance = 108.dp

@Composable
fun screenPadding(withBottomBar: Boolean): PaddingValues {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(
        start = 20.dp,
        end = 20.dp,
        top = top + 12.dp,
        bottom = bottom + if (withBottomBar) BottomBarClearance else 28.dp,
    )
}

/** Centers a column of content and caps its width on tablets. */
@Composable
fun AdaptiveContainer(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = MaxContentWidth).fillMaxSize(), content = content)
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = if (onBack != null) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                color = FourD.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = FourD.colors.textSecondary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = FourD.colors.textPrimary,
            modifier =
                Modifier
                    .weight(1f)
                    .semantics { heading() },
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action, color = FourD.colors.cyan, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Gradient icon disc used to label cards. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(FourD.colors.violet, FourD.colors.cyan),
    size: Dp = 40.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(Brush.linearGradient(colors.map { it.copy(alpha = 0.9f) })),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = FourD.colors.cyan,
) {
    GlassCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
        }
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = FourD.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = FourD.colors.textSecondary, maxLines = 2)
    }
}

/** Non-interactive tag (category, muscle group). */
@Composable
fun Tag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = FourD.colors.textSecondary,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .background(FourD.colors.glassFill)
                .border(1.dp, FourD.colors.glassBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** Difficulty shown as text plus 1–3 filled bars (never colour alone). */
@Composable
fun DifficultyBadge(
    difficulty: Difficulty,
    modifier: Modifier = Modifier,
) {
    val c = FourD.colors
    val accent =
        when (difficulty) {
            Difficulty.BEGINNER -> c.mint
            Difficulty.INTERMEDIATE -> c.amber
            Difficulty.ADVANCED -> c.pink
        }
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.glassFill)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
            (1..3).forEach { level ->
                Box(
                    Modifier
                        .width(4.dp)
                        .height((5 + level * 3).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (level <= difficulty.level) accent else c.ringTrack),
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(difficulty.label, style = MaterialTheme.typography.labelMedium, color = c.textPrimary)
    }
}

@Composable
fun DisclaimerNote(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FourD.colors.glassFillLow)
            .border(1.dp, FourD.colors.glassBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = FourD.colors.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = FourD.colors.textSecondary)
    }
}

/** Profile photo with a gradient ring; falls back to an icon. */
@Composable
fun Avatar(
    photoPath: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val c = FourD.colors
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.sweepGradient(listOf(c.cyan, c.violet, c.pink, c.cyan)))
            .padding(2.5.dp)
            .clip(CircleShape)
            .background(c.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (photoPath != null && File(photoPath).exists()) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = "Profile photo of $name",
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
            )
        } else {
            val initial = name.trim().firstOrNull()?.uppercaseChar()
            if (initial != null) {
                Text(
                    initial.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = c.textPrimary,
                )
            } else {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = c.textSecondary)
            }
        }
    }
}

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    label: String = "Loading",
) {
    val reduce = FourD.reduceMotion
    val c = FourD.colors
    val angle =
        if (reduce) {
            0f
        } else {
            rememberInfiniteTransition(label = "loading")
                .animateFloat(
                    0f,
                    360f,
                    infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
                    label = "loadingAngle",
                ).value
        }
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(44.dp)
                .rotate(angle)
                .border(3.dp, Brush.sweepGradient(listOf(Color.Transparent, c.cyan, c.violet)), CircleShape),
        )
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier.fillMaxWidth()) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = FourD.colors.textPrimary)
        if (onRetry != null) {
            Spacer(Modifier.height(12.dp))
            GhostButton("Try again", onRetry, icon = Icons.Rounded.Refresh)
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismissLabel: String = "Cancel",
) {
    val c = FourD.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceHigh,
        titleContentColor = c.textPrimary,
        textContentColor = c.textSecondary,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) c.danger else c.cyan, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, color = c.textSecondary, style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

@Composable
fun CenteredMessage(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = FourD.colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = FourD.colors.textSecondary, textAlign = TextAlign.Center)
    }
}
