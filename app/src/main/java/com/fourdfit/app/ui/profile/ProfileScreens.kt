package com.fourdfit.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourdfit.app.di.appViewModel
import com.fourdfit.app.ui.components.AdaptiveContainer
import com.fourdfit.app.ui.components.Avatar
import com.fourdfit.app.ui.components.FourDTextField
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
import com.fourdfit.app.ui.components.WeeklyBarChart
import com.fourdfit.app.ui.components.entrance
import com.fourdfit.app.ui.components.screenPadding
import com.fourdfit.app.ui.onboarding.FormMessage
import com.fourdfit.app.ui.onboarding.ProfileFormFields
import com.fourdfit.app.ui.theme.FourD
import com.fourdfit.app.utils.DateUtils
import com.fourdfit.app.utils.Units

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm =
        appViewModel {
            ProfileViewModel(it.profileRepository, it.workoutRepository, it.nutritionRepository, it.settingsRepository)
        }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = FourD.colors

    AdaptiveContainer {
        val profile = state.profile
        if (state.loading || profile == null) {
            LoadingState(Modifier.padding(top = 120.dp))
            return@AdaptiveContainer
        }
        val summary = state.summary
        val sign = profile.zodiacSign
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = screenPadding(withBottomBar = true),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                ScreenHeader("Profile", modifier = Modifier.entrance(0)) {
                    IconCircleButton(Icons.Rounded.Settings, "Settings", onOpenSettings)
                }
            }
            item(key = "hero") {
                GlassCard(Modifier.fillMaxWidth().entrance(1), glow = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(profile.photoPath, profile.fullName, size = 92.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                profile.fullName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = c.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                profile.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Tag("Age ${profile.ageOn()}")
                                Tag("${sign.symbol} ${sign.displayName}", color = c.cyan)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    GhostButton("Edit profile", onEditProfile, Modifier.fillMaxWidth(), icon = Icons.Rounded.Edit)
                }
            }
            item(key = "details") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Height", Units.formatHeight(profile.heightCm, state.units), Modifier.weight(1f), Icons.Rounded.Straighten)
                        StatTile("Born", profile.dateOfBirth.format(DateUtils.mediumDate), Modifier.weight(1f), Icons.Rounded.Cake, c.pink)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Activity level", profile.activityLevel.label, Modifier.weight(1f), Icons.Rounded.Speed, c.violet)
                        StatTile("Fitness goal", profile.fitnessGoal.label, Modifier.weight(1f), Icons.Rounded.Flag, c.amber)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(
                            "Zodiac sign",
                            "${sign.displayName} ${sign.symbol}",
                            Modifier.weight(1f),
                            Icons.Rounded.AutoAwesome,
                            c.cyan,
                        )
                        StatTile("Country", profile.country, Modifier.weight(1f), Icons.Rounded.Public, c.mint)
                    }
                }
            }
            item(key = "statsTitle") { SectionTitle("Your activity") }
            item(key = "stats") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(
                            "Workout streak",
                            "${summary.currentStreak} days",
                            Modifier.weight(1f),
                            Icons.Rounded.LocalFireDepartment,
                            c.amber,
                        )
                        StatTile(
                            "Completed workouts",
                            "${summary.totalWorkouts}",
                            Modifier.weight(1f),
                            Icons.Rounded.FitnessCenter,
                            c.violet,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(
                            "This week",
                            "${summary.workoutsThisWeek} workouts",
                            Modifier.weight(1f),
                            Icons.Rounded.CalendarMonth,
                            c.cyan,
                        )
                        StatTile(
                            "Exercise time",
                            DateUtils.formatDuration(summary.totalActiveSeconds),
                            Modifier.weight(1f),
                            Icons.Rounded.Insights,
                            c.pink,
                        )
                    }
                }
            }
            item(key = "nutrition") {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(
                            progress = state.mealsDone / 120f,
                            diameter = 84.dp,
                            stroke = 8.dp,
                            colors = listOf(c.mint, c.cyan),
                            contentDescription = "Nutrition plan ${state.mealsDone} of 120 meals",
                        ) {
                            Text("${(state.mealsDone * 100) / 120}%", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Nutrition progress", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                            Text(
                                "Day ${state.planDay} of 30. ${state.mealsDone} of 120 meals checked off.",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                            )
                        }
                    }
                }
            }
            item(key = "weekly") {
                GlassCard(Modifier.fillMaxWidth()) {
                    SectionTitle("Weekly activity", action = "All progress", onAction = onOpenProgress)
                    Spacer(Modifier.height(10.dp))
                    WeeklyBarChart(summary.last7Days, chartHeight = 100.dp)
                }
            }
            item(key = "achievements") {
                NeonButton("Progress & achievements", onOpenProgress, Modifier.fillMaxWidth(), icon = Icons.Rounded.EmojiEvents)
            }
        }
    }
}

@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    val vm = appViewModel { EditProfileViewModel(it.profileRepository, it.settingsRepository, it.imageStorage) }
    val state by vm.state.collectAsStateWithLifecycle()
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) vm.onPhotoPicked(uri)
        }
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    AdaptiveContainer {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(screenPadding(withBottomBar = false)),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenHeader("Edit profile", onBack = onBack)
            if (!state.loaded) {
                LoadingState()
            } else {
                EditProfileForm(state, vm, onPickPhoto = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                })
            }
        }
    }
}

@Composable
private fun EditProfileForm(
    state: EditProfileUiState,
    vm: EditProfileViewModel,
    onPickPhoto: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard(Modifier.fillMaxWidth()) {
            ProfileFormFields(
                form = state.form,
                errors = state.errors,
                onChange = vm::onFormChange,
                onPickPhoto = onPickPhoto,
                photoBusy = state.savingPhoto,
                afterName = {
                    FourDTextField(
                        value = state.email,
                        onValueChange = {},
                        label = "Email",
                        enabled = false,
                        supportingText = "Email is used to sign in and can't be changed here.",
                    )
                },
            )
        }
        FormMessage(state.message)
        NeonButton("Save changes", vm::save, Modifier.fillMaxWidth(), loading = state.saving, icon = Icons.Rounded.Save)
    }
}
