package com.fourdfit.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourdfit.app.di.appViewModel
import com.fourdfit.app.domain.model.Exercise
import com.fourdfit.app.ui.components.AdaptiveContainer
import com.fourdfit.app.ui.components.CenteredMessage
import com.fourdfit.app.ui.components.DifficultyBadge
import com.fourdfit.app.ui.components.DisclaimerNote
import com.fourdfit.app.ui.components.ExerciseAnimation
import com.fourdfit.app.ui.components.GhostButton
import com.fourdfit.app.ui.components.GlassCard
import com.fourdfit.app.ui.components.IconCircleButton
import com.fourdfit.app.ui.components.LoadingState
import com.fourdfit.app.ui.components.NeonButton
import com.fourdfit.app.ui.components.ProgressRing
import com.fourdfit.app.ui.components.ScreenHeader
import com.fourdfit.app.ui.components.SectionTitle
import com.fourdfit.app.ui.components.StatTile
import com.fourdfit.app.ui.components.Tag
import com.fourdfit.app.ui.components.screenPadding
import com.fourdfit.app.ui.theme.FourD
import com.fourdfit.app.utils.DateUtils
import kotlinx.coroutines.delay

private val StartButtonClearance = 100.dp

@Composable
private fun detailPadding(): PaddingValues {
    val base = screenPadding(withBottomBar = false)
    return PaddingValues(
        start = 20.dp,
        end = 20.dp,
        top = base.calculateTopPadding(),
        bottom = base.calculateBottomPadding() + StartButtonClearance,
    )
}

@Composable
fun ProgramDetailScreen(
    programId: String,
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onStart: () -> Unit,
) {
    val vm = appViewModel(key = "program_$programId") { ProgramDetailViewModel(it.workoutRepository, programId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = FourD.colors
    AdaptiveContainer {
        val program = state.program
        if (program == null) {
            Column(Modifier.padding(screenPadding(withBottomBar = false))) {
                ScreenHeader("Workout", onBack = onBack)
                if (state.loading) LoadingState() else CenteredMessage("Workout not found", "Go back and choose another workout.")
            }
            return@AdaptiveContainer
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = detailPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { ScreenHeader(program.title, subtitle = program.subtitle, onBack = onBack) }
            item {
                GlassCard(Modifier.fillMaxWidth(), glow = true) {
                    state.exercises.firstOrNull()?.let { first ->
                        ExerciseAnimation(
                            motion = first.motion,
                            tempo = first.tempo.multiplier,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(210.dp),
                            contentDescription = "Animated preview: ${first.name}",
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(program.description, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        DifficultyBadge(program.difficulty)
                        program.categories.forEach { Tag(it.label) }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Minutes", "${program.estimatedMinutes}", Modifier.weight(1f), Icons.Rounded.Timer)
                    StatTile("Exercises", "${state.exercises.size}", Modifier.weight(1f), Icons.Rounded.FitnessCenter, c.violet)
                    StatTile("Sets", "${state.exercises.sumOf { it.sets }}", Modifier.weight(1f), Icons.Rounded.Repeat, c.pink)
                }
            }
            item { SectionTitle("Exercises in order") }
            itemsIndexed(state.exercises, key = { i, e -> "$i-${e.id}" }) { i, exercise ->
                ExerciseRow(exercise, onClick = { onOpenExercise(exercise.id) }, position = i + 1)
            }
            item { DisclaimerNote(SAFETY_NOTE) }
        }
        NeonButton(
            text = "Start workout",
            onClick = onStart,
            icon = Icons.Rounded.PlayArrow,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .fillMaxWidth(),
        )
    }
}

@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    val vm = appViewModel(key = "exercise_$exerciseId") { ExerciseDetailViewModel(it.workoutRepository, exerciseId) }
    val state by vm.state.collectAsStateWithLifecycle()
    AdaptiveContainer {
        val exercise = state.exercise
        if (exercise == null) {
            Column(Modifier.padding(screenPadding(withBottomBar = false))) {
                ScreenHeader("Exercise", onBack = onBack)
                if (state.loading) LoadingState() else CenteredMessage("Exercise not found", "Go back and choose another exercise.")
            }
            return@AdaptiveContainer
        }
        ExerciseDetailContent(exercise, onBack)
        NeonButton(
            text = "Start Workout",
            onClick = onStart,
            icon = Icons.Rounded.PlayArrow,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseDetailContent(
    exercise: Exercise,
    onBack: () -> Unit,
) {
    val c = FourD.colors
    var playing by remember { mutableStateOf(true) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = detailPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader(exercise.name, onBack = onBack) }
        item {
            GlassCard(Modifier.fillMaxWidth(), glow = true) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                ) {
                    ExerciseAnimation(
                        motion = exercise.motion,
                        playing = playing,
                        tempo = exercise.tempo.multiplier,
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = "Animated demonstration of ${exercise.name}",
                    )
                    IconCircleButton(
                        icon = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playing) "Pause demonstration" else "Play demonstration",
                        onClick = { playing = !playing },
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    DifficultyBadge(exercise.difficulty)
                    exercise.categories.take(2).forEach { Tag(it.label) }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Per set", DateUtils.formatDuration(exercise.durationSeconds), Modifier.weight(1f), Icons.Rounded.Timer)
                    StatTile("Sets", "${exercise.sets}", Modifier.weight(1f), Icons.Rounded.Repeat, c.violet)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Reps", exercise.reps?.toString() ?: "Timed", Modifier.weight(1f), Icons.Rounded.Speed, c.pink)
                    StatTile("Rest", DateUtils.formatDuration(exercise.restSeconds), Modifier.weight(1f), Icons.Rounded.Hotel, c.mint)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    "About this move",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(6.dp))
                Text(exercise.description, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
                Spacer(Modifier.height(12.dp))
                Text("Target muscles", style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    exercise.targetMuscles.forEach { Tag(it, color = c.textPrimary) }
                }
            }
        }
        item { RestTimerCard(exercise.restSeconds.coerceAtLeast(10)) }
        item { TipsCard("Safety instructions", exercise.safety, Icons.Rounded.Shield, c.mint) }
        item { TipsCard("Common mistakes", exercise.mistakes, Icons.Rounded.WarningAmber, c.amber) }
        item { DisclaimerNote(SAFETY_NOTE) }
    }
}

@Composable
private fun TipsCard(
    title: String,
    tips: List<String>,
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
) {
    if (tips.isEmpty()) return
    val c = FourD.colors
    GlassCard(Modifier.fillMaxWidth()) {
        SectionTitle(title)
        Spacer(Modifier.height(4.dp))
        tips.forEach { tip ->
            Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(tip, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
            }
        }
    }
}

/** Stand-alone rest timer on the exercise page. */
@Composable
fun RestTimerCard(seconds: Int) {
    val c = FourD.colors
    var remaining by remember(seconds) { mutableIntStateOf(seconds) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(running) {
        while (running && remaining > 0) {
            delay(1_000)
            remaining--
        }
        if (remaining <= 0) running = false
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = remaining / seconds.toFloat(),
                diameter = 96.dp,
                stroke = 9.dp,
                colors = listOf(c.mint, c.cyan),
                contentDescription = "Rest timer, $remaining seconds remaining",
            ) {
                Text(DateUtils.clock(remaining), style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Rest timer", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(
                    if (remaining == 0) "Rest complete — ready for the next set." else "Recommended rest between sets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton(
                        text = if (running) "Pause" else "Start",
                        icon = if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        onClick = {
                            if (remaining == 0) remaining = seconds
                            running = !running
                        },
                    )
                    GhostButton(
                        text = "Reset",
                        icon = Icons.Rounded.Refresh,
                        onClick = {
                            running = false
                            remaining = seconds
                        },
                    )
                }
            }
        }
    }
}
