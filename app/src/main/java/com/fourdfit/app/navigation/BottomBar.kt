package com.fourdfit.app.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fourdfit.app.ui.components.glowBorder
import com.fourdfit.app.ui.theme.FourD

/** Floating glass navigation bar with bouncing, glowing icons. */
@Composable
fun FourDBottomBar(
    current: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = FourD.colors
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(shape)
                    .background(c.glassFillStrong)
                    .glowBorder(
                        shape,
                        listOf(c.cyan.copy(alpha = 0.7f), c.violet.copy(alpha = 0.25f), c.pink.copy(alpha = 0.7f)),
                        animated = false,
                    ).padding(horizontal = 6.dp)
                    .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopLevelDestination.entries.forEach { destination ->
                BottomBarItem(destination, destination == current) { onSelect(destination) }
            }
        }
    }
}

@Composable
private fun RowScope.BottomBarItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = FourD.colors
    val bouncy = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    val scale by animateFloatAsState(if (selected) 1.14f else 1f, bouncy, label = "navScale")
    val pill by animateFloatAsState(if (selected) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "navPill")
    val lift by animateDpAsState(if (selected) (-2).dp else 0.dp, label = "navLift")
    val labelColor by animateColorAsState(if (selected) c.textPrimary else c.textMuted, label = "navLabel")
    Column(
        modifier =
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(22.dp))
                .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .offset(y = lift)
                .size(width = 52.dp, height = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .matchParentSizeCompat()
                    .graphicsLayer {
                        alpha = pill
                        scaleX = 0.6f + 0.4f * pill
                    }.clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(c.violet.copy(alpha = 0.55f), c.cyan.copy(alpha = 0.35f)))),
            )
            AnimatedContent(
                targetState = selected,
                transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.7f)) togetherWith fadeOut() },
                label = "navIcon",
            ) { isSelected ->
                Icon(
                    imageVector = if (isSelected) destination.selectedIcon else destination.icon,
                    contentDescription = null,
                    tint = if (isSelected) c.textPrimary else c.textMuted,
                    modifier =
                        Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Modifier.matchParentSizeCompat(): Modifier = this.fillMaxWidth().fillMaxHeight()
