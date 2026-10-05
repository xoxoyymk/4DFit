package com.fourdfit.app.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourdfit.app.di.LocalAppContainer
import com.fourdfit.app.di.appViewModel
import com.fourdfit.app.domain.model.StepStatus
import com.fourdfit.app.ui.components.AdaptiveContainer
import com.fourdfit.app.ui.components.Avatar
import com.fourdfit.app.ui.components.DifficultyBadge
import com.fourdfit.app.ui.components.ExerciseAnimation
import com.fourdfit.app.ui.components.GhostButton
import com.fourdfit.app.ui.components.GlassCard
import com.fourdfit.app.ui.components.IconBadge
import com.fourdfit.app.ui.components.IconCircleButton
import com.fourdfit.app.ui.components.LinearMeter
import com.fourdfit.app.ui.components.LoadingState
import com.fourdfit.app.ui.components.NeonButton
import com.fourdfit.app.ui.components.ProgressRing
import com.fourdfit.app.ui.components.Tag
import com.fourdfit.app.ui.components.WeeklyBarChart
import com.fourdfit.app.ui.components.entrance
import com.fourdfit.app.ui.components.screenPadding
import com.fourdfit.app.ui.theme.FourD
import com.fourdfit.app.utils.DateUtils
import java.time.LocalDate

@Composable
fun HomeScreen(
    onOpenProgram: (String) -> Unit,
    onStartProgram: (String) -> Unit,
    onOpenProgress: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNutrition: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm =
        appViewModel {
            HomeViewModel(it.profileRepository, it.workoutRepository, it.nutritionRepository, it.activityRepository, it.stepCounter)
        }
    val state by vm.state.collectAsStateWithLifecycle()
    val stepPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            container.stepCounter.refresh()
        }
    val requestSteps: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stepPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            container.stepCounter.refresh()
        }
    }

    AdaptiveContainer {
        if (state.loading) {
            LoadingState(Modifier.padding(top = 120.dp))
            return@AdaptiveContainer
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = screenPadding(withBottomBar = true),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") {
                HomeHeader(state, onOpenProfile, onOpenSettings, Modifier.entrance(0))
            }
            item(key = "workout") {
                TodaysWorkoutCard(state, onOpenProgram, onStartProgram, Modifier.entrance(1))
            }
            item(key = "activity") {
                ActivityCard(state, requestSteps, Modifier.entrance(2))
            }
            item(key = "hydration") {
                HydrationCard(state, vm::addWater, vm::removeWater, Modifier.entrance(3))
            }
            item(key = "wellness") { WellnessGoalCard(state) }
            item(key = "nutrition") { NutritionTipCard(state, onOpenNutrition) }
            item(key = "streak") { StreakCard(state) }
            item(key = "weekly") { WeeklyProgressCard(state, onOpenProgress) }
        }
    }
}

@Composable
private fun HomeHeader(
    state: HomeUiState,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = FourD.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(state.dateLabel, style = MaterialTheme.typography.labelLarge, color = c.textSecondary)
            Text(
                "${state.greeting},",
                style = MaterialTheme.typography.headlineMedium,
                color = c.textPrimary,
            )
            Text(
                state.firstName,
                style = MaterialTheme.typography.displaySmall,
                color = c.cyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            state.zodiac?.let { sign ->
                Spacer(Modifier.height(6.dp))
                Tag("${sign.symbol} ${sign.displayName}")
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            IconCircleButton(Icons.Rounded.Settings, "Settings", onOpenSettings)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .clip(CircleShape)
                    .clickable(onClickLabel = "Open profile", role = Role.Button, onClick = onOpenProfile),
            ) {
                Avatar(state.photoPath, state.firstName, size = 56.dp)
            }
        }
    }
}

@Composable
private fun TodaysWorkoutCard(
    state: HomeUiState,
    onOpenProgram: (String) -> Unit,
    onStartProgram: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = FourD.colors
    val program = state.todaysWorkout ?: return
    GlassCard(modifier = modifier.fillMaxWidth(), glow = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Today's workout", style = MaterialTheme.typography.labelLarge, color = c.cyan)
                Spacer(Modifier.height(4.dp))
                Text(program.title, style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
                Text(program.subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag("${program.estimatedMinutes} min")
                    Tag("${state.todaysWorkoutExerciseCount} moves")
                }
                Spacer(Modifier.height(8.dp))
                DifficultyBadge(program.difficulty)
            }
            state.todaysWorkoutPreview?.let { preview ->
                ExerciseAnimation(
                    motion = preview.motion,
                    tempo = preview.tempo.multiplier,
                    modifier = Modifier.size(128.dp),
                    contentDescription = "Animated preview: ${preview.name}",
                )
            }
        }
        if (state.workoutDoneToday) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.mint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("You've already moved today. Nice work.", style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonButton(
                text = if (state.workoutDoneToday) "Go again" else "Start workout",
                onClick = { onStartProgram(program.id) },
                icon = Icons.Rounded.PlayArrow,
                modifier = Modifier.weight(1f),
            )
            GhostButton("Details", onClick = { onOpenProgram(program.id) })
        }
    }
}

@Composable
private fun ActivityCard(
    state: HomeUiState,
    onEnableSteps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = FourD.colors
    GlassCard(modifier.fillMaxWidth()) {
        Text(
            "Today's activity",
            style = MaterialTheme.typography.titleLarge,
            color = c.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            RingStat(
                progress = state.activeMinutes / state.activeMinutesGoal.toFloat(),
                value = "${(100 * state.activeMinutes / state.activeMinutesGoal.coerceAtLeast(1)).coerceAtMost(100)}%",
                label = "Workout",
                detail = "${state.activeMinutes} of ${state.activeMinutesGoal} min",
                colors = listOf(c.violet, c.pink),
                description = "Workout goal ${state.activeMinutes} of ${state.activeMinutesGoal} active minutes",
                modifier = Modifier.weight(1f),
            )
            when (state.stepStatus) {
                StepStatus.ACTIVE ->
                    RingStat(
                        progress = state.steps / state.stepGoal.toFloat(),
                        value = "%,d".format(state.steps),
                        label = "Steps",
                        detail = "of %,d".format(state.stepGoal),
                        colors = listOf(c.cyan, c.blue),
                        description = "Steps ${state.steps} of ${state.stepGoal}",
                        modifier = Modifier.weight(1f),
                    )
                StepStatus.NEEDS_PERMISSION ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        ProgressRing(0f, diameter = 92.dp, stroke = 9.dp) {
                            Text("Steps", style = MaterialTheme.typography.labelLarge, color = c.textSecondary)
                        }
                        TextButton(onClick = onEnableSteps) {
                            Text("Turn on", color = c.cyan, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                StepStatus.UNAVAILABLE ->
                    RingStat(
                        progress = 0f,
                        value = "–",
                        label = "Steps",
                        detail = "No step sensor",
                        colors = listOf(c.cyan, c.blue),
                        description = "Steps unavailable on this device",
                        modifier = Modifier.weight(1f),
                    )
            }
            RingStat(
                progress = state.water / state.waterGoal.toFloat(),
                value = "${state.water}/${state.waterGoal}",
                label = "Water",
                detail = "glasses",
                colors = listOf(c.mint, c.cyan),
                description = "Water ${state.water} of ${state.waterGoal} glasses",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RingStat(
    progress: Float,
    value: String,
    label: String,
    detail: String,
    colors: List<androidx.compose.ui.graphics.Color>,
    description: String,
    modifier: Modifier = Modifier,
) {
    val c = FourD.colors
    Column(modifier.clearAndSetSemantics { contentDescription = description }, horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressRing(progress = progress, diameter = 92.dp, stroke = 9.dp, colors = colors) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = c.textPrimary, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = c.textPrimary)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HydrationCard(
    state: HomeUiState,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = FourD.colors
    GlassCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.WaterDrop, colors = listOf(c.cyan, c.blue))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Hydration", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(
                    if (state.water >=
                        state.waterGoal
                    ) {
                        "Goal reached — keep sipping when thirsty."
                    } else {
                        "${state.waterGoal - state.water} glasses to go today"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
            IconCircleButton(Icons.Rounded.Remove, "Remove a glass of water", onRemove, enabled = state.water > 0)
            Spacer(Modifier.width(8.dp))
            IconCircleButton(Icons.Rounded.Add, "Add a glass of water", onAdd, filled = true)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = "${state.water} of ${state.waterGoal} glasses of water" },
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            repeat(state.waterGoal) { index ->
                val filled = index < state.water
                Icon(
                    imageVector = if (filled) Icons.Rounded.WaterDrop else Icons.Outlined.WaterDrop,
                    contentDescription = null,
                    tint = if (filled) c.cyan else c.textMuted,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        if (state.hydrationTip.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(state.hydrationTip, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
    }
}

@Composable
private fun WellnessGoalCard(state: HomeUiState) {
    val c = FourD.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.SelfImprovement, colors = listOf(c.violet, c.pink))
            Spacer(Modifier.width(12.dp))
            Text("Daily wellness goal", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        }
        Spacer(Modifier.height(10.dp))
        Text(state.wellnessGoal, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "${state.activeMinutes} of ${state.activeMinutesGoal} active minutes",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearMeter(state.activeMinutes / state.activeMinutesGoal.toFloat(), colors = listOf(c.violet, c.pink))
    }
}

@Composable
private fun NutritionTipCard(
    state: HomeUiState,
    onOpenNutrition: () -> Unit,
) {
    val c = FourD.colors
    GlassCard(Modifier.fillMaxWidth(), onClick = onOpenNutrition, onClickLabel = "Open nutrition plan") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Restaurant, colors = listOf(c.mint, c.cyan))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Today's nutrition tip", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(
                    "Day ${state.planDay} of 30, ${state.mealsDoneToday} of 4 meals checked off",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(state.nutritionTip, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
    }
}

@Composable
private fun StreakCard(state: HomeUiState) {
    val c = FourD.colors
    val summary = state.summary
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.LocalFireDepartment, colors = listOf(c.amber, c.pink))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (summary.currentStreak == 1) "1-day streak" else "${summary.currentStreak}-day streak",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.textPrimary,
                )
                Text(
                    if (summary.currentStreak ==
                        0
                    ) {
                        "Any workout today starts a new streak."
                    } else {
                        "Longest so far: ${summary.longestStreak} days"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            summary.last7Days.forEach { day ->
                val done = day.workouts > 0
                val isToday = day.date == LocalDate.now()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier.clearAndSetSemantics {
                            contentDescription = "${day.date.format(DateUtils.longDate)}: ${if (done) "worked out" else "no workout"}"
                        },
                ) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (done) c.amber.copy(alpha = 0.9f) else c.ringTrack)
                            .then(if (isToday) Modifier.border(1.5.dp, c.textPrimary, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Icons.Rounded.Check, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(day.date.format(DateUtils.shortDay).take(1), style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun WeeklyProgressCard(
    state: HomeUiState,
    onOpenProgress: () -> Unit,
) {
    val c = FourD.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Weekly progress",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "${state.summary.workoutsThisWeek} workouts this week",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
            IconCircleButton(Icons.Rounded.Insights, "See all progress", onOpenProgress)
        }
        Spacer(Modifier.height(16.dp))
        WeeklyBarChart(days = state.summary.last7Days, chartHeight = 110.dp)
    }
}
