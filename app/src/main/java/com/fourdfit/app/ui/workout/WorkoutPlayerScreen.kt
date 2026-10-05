package com.fourdfit.app.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourdfit.app.di.appViewModel
import com.fourdfit.app.domain.model.MotionType
import com.fourdfit.app.ui.components.AchievementBadge
import com.fourdfit.app.ui.components.AdaptiveContainer
import com.fourdfit.app.ui.components.CenteredMessage
import com.fourdfit.app.ui.components.ConfettiBurst
import com.fourdfit.app.ui.components.ConfirmDialog
import com.fourdfit.app.ui.components.DisclaimerNote
import com.fourdfit.app.ui.components.ExerciseAnimation
import com.fourdfit.app.ui.components.GlassCard
import com.fourdfit.app.ui.components.IconBadge
import com.fourdfit.app.ui.components.IconCircleButton
import com.fourdfit.app.ui.components.LinearMeter
import com.fourdfit.app.ui.components.LoadingState
import com.fourdfit.app.ui.components.NeonButton
import com.fourdfit.app.ui.components.ProgressRing
import com.fourdfit.app.ui.components.ScreenHeader
import com.fourdfit.app.ui.components.StatTile
import com.fourdfit.app.ui.components.screenPadding
import com.fourdfit.app.ui.theme.FourD
import com.fourdfit.app.utils.DateUtils

@Composable
fun WorkoutPlayerScreen(
    kind: String,
    id: String,
    onClose: () -> Unit,
) {
    val vm =
        appViewModel(key = "player_${kind}_$id") {
            WorkoutPlayerViewModel(it.workoutRepository, it.achievementRepository, kind, id)
        }
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmExit by remember { mutableStateOf(false) }

    KeepScreenOn()
    BackHandler(enabled = state is PlayerUiState.Active) {
        vm.pause()
        confirmExit = true
    }

    AdaptiveContainer {
        when (val s = state) {
            PlayerUiState.Loading -> LoadingState(Modifier.padding(top = 140.dp), label = "Preparing your workout")
            is PlayerUiState.NotFound ->
                Column(Modifier.padding(screenPadding(withBottomBar = false))) {
                    ScreenHeader("Workout", onBack = onClose)
                    CenteredMessage("Can't start this workout", s.message)
                }
            is PlayerUiState.Active ->
                ActivePlayer(
                    s = s,
                    onTogglePause = vm::togglePause,
                    onSkip = vm::skip,
                    onRequestExit = {
                        vm.pause()
                        confirmExit = true
                    },
                )
            is PlayerUiState.Completed -> CompletionContent(s, onDone = onClose)
        }
    }

    if (confirmExit) {
        ConfirmDialog(
            title = "Finish workout?",
            message = "Your progress so far will be saved. You can also keep going.",
            confirmLabel = "Finish workout",
            onConfirm = {
                confirmExit = false
                vm.finishEarly()
            },
            onDismiss = {
                confirmExit = false
                vm.resume()
            },
            dismissLabel = "Keep going",
        )
    }
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun ActivePlayer(
    s: PlayerUiState.Active,
    onTogglePause: () -> Unit,
    onSkip: () -> Unit,
    onRequestExit: () -> Unit,
) {
    val c = FourD.colors
    val step = s.step
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(s.index) {
        if (s.index > 0) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    val phaseColors =
        when (step.kind) {
            StepKind.PREPARE -> listOf(c.amber, c.pink)
            StepKind.WORK -> listOf(c.cyan, c.violet)
            StepKind.REST -> listOf(c.mint, c.cyan)
        }
    val phaseLabel =
        when (step.kind) {
            StepKind.PREPARE -> "Get ready"
            StepKind.WORK -> "Work"
            StepKind.REST -> "Rest"
        }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(screenPadding(withBottomBar = false)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconCircleButton(Icons.Rounded.Close, "End workout", onRequestExit)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(
                    "Elapsed ${DateUtils.clock(s.elapsedSeconds)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        LinearMeter(s.progress, colors = phaseColors)
        Spacer(Modifier.height(16.dp))

        AnimatedContent(
            targetState = phaseLabel,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "phase",
        ) { label ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Icon(
                    imageVector =
                        when (step.kind) {
                            StepKind.PREPARE -> Icons.Rounded.Timer
                            StepKind.WORK -> Icons.Rounded.Bolt
                            StepKind.REST -> Icons.Rounded.Hotel
                        },
                    contentDescription = null,
                    tint = phaseColors.first(),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (s.paused) "$label (paused)" else label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = phaseColors.first(),
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentAlignment = Alignment.Center,
        ) {
            ExerciseAnimation(
                motion = if (step.kind == StepKind.REST) MotionType.BREATH else step.exercise.motion,
                playing = !s.paused,
                tempo = step.exercise.tempo.multiplier,
                primary = phaseColors.first(),
                secondary = phaseColors.last(),
                modifier = Modifier.fillMaxSize(),
                contentDescription = if (step.kind == StepKind.REST) "Breathe and recover" else "Demonstration of ${step.exercise.name}",
            )
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = s.stepProgress,
                diameter = 128.dp,
                stroke = 11.dp,
                colors = phaseColors,
                contentDescription = "${s.remaining} seconds remaining",
            ) {
                Text(DateUtils.clock(s.remaining), style = MaterialTheme.typography.headlineMedium, color = c.textPrimary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (step.kind == StepKind.REST) "Next: ${step.exercise.name}" else step.exercise.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text("Set ${step.setNumber} of ${step.totalSets}", style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
                if (step.kind == StepKind.WORK) {
                    Text(
                        step.exercise.reps?.let { "Target: $it reps" } ?: "Hold or move for ${step.seconds}s",
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.cyan,
                    )
                }
            }
        }

        s.upNext?.let { next ->
            Spacer(Modifier.height(16.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Up next", style = MaterialTheme.typography.labelLarge, color = c.textSecondary)
                Text(next, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlButton(Icons.Rounded.SkipNext, "Skip", onSkip)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconCircleButton(
                    icon = if (s.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = if (s.paused) "Resume" else "Pause",
                    onClick = onTogglePause,
                    size = 80.dp,
                    filled = true,
                )
                Spacer(Modifier.height(6.dp))
                Text(if (s.paused) "Resume" else "Pause", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
            }
            ControlButton(Icons.Rounded.Flag, "Finish", onRequestExit)
        }
        Spacer(Modifier.height(16.dp))
        DisclaimerNote(SAFETY_NOTE)
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconCircleButton(icon, label, onClick, size = 56.dp)
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = FourD.colors.textSecondary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompletionContent(
    s: PlayerUiState.Completed,
    onDone: () -> Unit,
) {
    val c = FourD.colors
    val trophyScale = remember { Animatable(if (FourD.reduceMotion) 1f else 0.3f) }
    LaunchedEffect(Unit) { trophyScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 160f)) }
    Box(Modifier.fillMaxSize()) {
        ConfettiBurst(Modifier.fillMaxSize(), play = s.saved)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(screenPadding(withBottomBar = false)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            IconBadge(
                Icons.Rounded.EmojiEvents,
                colors = listOf(c.amber, c.pink),
                size = 104.dp,
                modifier =
                    Modifier.graphicsLayer {
                        scaleX = trophyScale.value
                        scaleY = trophyScale.value
                    },
            )
            Spacer(Modifier.height(18.dp))
            Text(
                when {
                    !s.saved -> "Workout ended"
                    s.completedFully -> "Workout complete!"
                    else -> "Nice effort!"
                },
                style = MaterialTheme.typography.displaySmall,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text(s.title, style = MaterialTheme.typography.titleMedium, color = c.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Duration", DateUtils.formatDuration(s.durationSeconds), Modifier.weight(1f), Icons.Rounded.Timer)
                StatTile("Exercises", "${s.exercisesCompleted}", Modifier.weight(1f), Icons.Rounded.Bolt, c.violet)
                StatTile("Sets", "${s.setsCompleted}", Modifier.weight(1f), Icons.Rounded.Flag, c.pink)
            }
            if (s.newAchievements.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                GlassCard(Modifier.fillMaxWidth(), glow = true) {
                    Text(
                        if (s.newAchievements.size == 1) "New badge unlocked" else "${s.newAchievements.size} new badges unlocked",
                        style = MaterialTheme.typography.titleLarge,
                        color = c.textPrimary,
                    )
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        s.newAchievements.forEach { AchievementBadge(it, unlocked = true, animateUnlock = true) }
                    }
                }
            }
            if (!s.saved) {
                Spacer(Modifier.height(18.dp))
                DisclaimerNote("Sessions shorter than 10 seconds of exercise aren't saved. Start again whenever you're ready.")
            } else {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Saved to your progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.mint,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = "Workout saved to your progress" },
                )
            }
            Spacer(Modifier.height(24.dp))
            NeonButton("Done", onDone, Modifier.fillMaxWidth())
        }
    }
}
