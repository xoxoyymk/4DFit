package com.fourdfit.app.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fourdfit.app.ui.theme.FourD

private val ButtonShape = RoundedCornerShape(20.dp)

/** Primary call to action: gradient fill, soft coloured glow and press squash. */
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    gradient: List<Color>? = null,
) {
    val colors = FourD.colors
    val brushColors = gradient ?: colors.primaryGradient
    val interaction = remember { MutableInteractionSource() }
    val active = enabled && !loading
    Box(
        modifier =
            modifier
                .defaultMinSize(minHeight = 56.dp)
                .pressScale(interaction, 0.96f)
                .alpha(if (enabled) 1f else 0.45f)
                .shadow(
                    elevation = if (enabled) 16.dp else 0.dp,
                    shape = ButtonShape,
                    ambientColor = brushColors.first(),
                    spotColor = brushColors.last(),
                ).clip(ButtonShape)
                .background(Brush.horizontalGradient(brushColors))
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    enabled = active,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Secondary action: glass fill with outline. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color? = null,
) {
    val colors = FourD.colors
    val tint = contentColor ?: colors.textPrimary
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .defaultMinSize(minHeight = 52.dp)
                .pressScale(interaction, 0.96f)
                .alpha(if (enabled) 1f else 0.45f)
                .clip(ButtonShape)
                .background(colors.glassFill)
                .border(1.dp, colors.glassBorder, ButtonShape)
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = tint)
        }
    }
}

/** Round icon button with a 48dp minimum touch target and a required content description. */
@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    filled: Boolean = false,
    tint: Color? = null,
    enabled: Boolean = true,
) {
    val colors = FourD.colors
    val interaction = remember { MutableInteractionSource() }
    val background =
        if (filled) {
            Brush.linearGradient(
                colors.primaryGradient,
            )
        } else {
            Brush.linearGradient(listOf(colors.glassFill, colors.glassFill))
        }
    Box(
        modifier =
            modifier
                .size(size.coerceAtLeast(48.dp))
                .pressScale(interaction, 0.92f)
                .alpha(if (enabled) 1f else 0.4f)
                .clip(CircleShape)
                .background(background)
                .border(1.dp, if (filled) Color.Transparent else colors.glassBorder, CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    enabled = enabled,
                    role = Role.Button,
                    onClickLabel = contentDescription,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: if (filled) Color.White else colors.textPrimary,
            modifier = Modifier.size(size.coerceAtLeast(48.dp) * 0.46f),
        )
    }
}
