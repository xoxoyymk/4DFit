package com.fourdfit.app.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.fourdfit.app.di.appViewModel
import com.fourdfit.app.domain.model.Achievement
import com.fourdfit.app.domain.model.ProgressSummary
import com.fourdfit.app.domain.model.WorkoutSession
import com.fourdfit.app.domain.repository.AchievementRepository
import com.fourdfit.app.domain.repository.WorkoutRepository
import com.fourdfit.app.domain.usecase.ProgressCalculator
import com.fourdfit.app.ui.components.AchievementBadge
import com.fourdfit.app.ui.components.AdaptiveContainer
import com.fourdfit.app.ui.components.CenteredMessage
import com.fourdfit.app.ui.components.GlassCard
import com.fourdfit.app.ui.components.MonthlyHeatmap
import com.fourdfit.app.ui.components.ScreenHeader
import com.fourdfit.app.ui.components.SectionTitle
import com.fourdfit.app.ui.components.StatTile
import com.fourdfit.app.ui.components.WeeklyBarChart
import com.fourdfit.app.ui.components.entrance
import com.fourdfit.app.ui.components.screenPadding
import com.fourdfit.app.ui.theme.FourD
import com.fourdfit.app.utils.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ProgressUiState(
    val loading: Boolean = true,
    val summary: ProgressSummary = ProgressSummary.EMPTY,
    val history: List<WorkoutSession> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
)

class ProgressViewModel(
    private val workoutRepository: WorkoutRepository,
    private val achievementRepository: AchievementRepository,
) : ViewModel() {
    val state: StateFlow<ProgressUiState> =
        combine(
            workoutRepository.observeHistory(),
            achievementRepository.observeAchievements(),
        ) { history, achievements ->
            ProgressUiState(false, ProgressCalculator.summarize(history, LocalDate.now()), history, achievements)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    init {
        viewModelScope.launch {
            workoutRepository.refreshHistory()
            achievementRepository.evaluate()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressScreen(onBack: () -> Unit) {
    val vm = appViewModel { ProgressViewModel(it.workoutRepository, it.achievementRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = FourD.colors
    val s = state.summary
    val now = Instant.now()

    AdaptiveContainer {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = screenPadding(withBottomBar = false),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    "Progress",
                    subtitle = "Your journey, compared only with your past self.",
                    onBack = onBack,
                    modifier = Modifier.entrance(0),
                )
            }
            item(key = "tiles") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Completed workouts", "${s.totalWorkouts}", Modifier.weight(1f), Icons.Rounded.FitnessCenter, c.violet)
                        StatTile("Exercise time", DateUtils.formatDuration(s.totalActiveSeconds), Modifier.weight(1f), Icons.Rounded.Timer)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(
                            "Current streak",
                            "${s.currentStreak} days",
                            Modifier.weight(1f),
                            Icons.Rounded.LocalFireDepartment,
                            c.amber,
                        )
                        StatTile("Longest streak", "${s.longestStreak} days", Modifier.weight(1f), Icons.Rounded.WorkspacePremium, c.pink)
                    }
                }
            }
            item(key = "weekly") {
                GlassCard(Modifier.fillMaxWidth()) {
                    SectionTitle("Weekly activity")
                    Text(
                        "${s.workoutsThisWeek} workouts this week. Minutes per day below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                    Spacer(Modifier.height(14.dp))
                    WeeklyBarChart(s.last7Days)
                }
            }
            item(key = "monthly") {
                GlassCard(Modifier.fillMaxWidth()) {
                    SectionTitle("Monthly activity")
                    Text(
                        "Active on ${s.activeDaysThisMonth} days this month, ${s.minutesThisMonth} minutes in total.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                    Spacer(Modifier.height(14.dp))
                    MonthlyHeatmap(s.last28Days)
                }
            }
            item(key = "achievementsTitle") {
                SectionTitle("Achievements")
            }
            item(key = "achievements") {
                GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                    val unlockedCount = state.achievements.count { it.unlocked }
                    Text(
                        "$unlockedCount of ${state.achievements.size} unlocked",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        state.achievements.forEach { achievement ->
                            val recent = achievement.unlockedAt?.let { Duration.between(it, now).toHours() < 24 } == true
                            AchievementBadge(achievement.type, achievement.unlocked, animateUnlock = recent)
                        }
                    }
                }
            }
            item(key = "historyTitle") { SectionTitle("Workout history") }
            if (!state.loading && state.history.isEmpty()) {
                item(key = "empty") {
                    CenteredMessage("No workouts yet", "Finish any workout and it will appear here.")
                }
            }
            items(state.history.take(50), key = { it.clientId }) { session -> HistoryRow(session) }
        }
    }
}

@Composable
private fun HistoryRow(session: WorkoutSession) {
    val c = FourD.colors
    val time = session.completedAt.atZone(ZoneId.systemDefault())
    GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (session.completedFully) Icons.Rounded.CheckCircle else Icons.Rounded.Timelapse,
                contentDescription = null,
                tint = if (session.completedFully) c.mint else c.amber,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(session.title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(
                    "${time.toLocalDate().format(DateUtils.mediumDate)}, ${if (session.completedFully) "completed" else "partial"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(DateUtils.formatDuration(session.durationSeconds), style = MaterialTheme.typography.titleMedium, color = c.cyan)
                Text("${session.exercisesCompleted} exercises", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        }
    }
}
