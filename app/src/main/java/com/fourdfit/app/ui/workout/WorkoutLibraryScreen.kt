package com.fourdfit.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourdfit.app.di.appViewModel
import com.fourdfit.app.domain.model.Exercise
import com.fourdfit.app.ui.components.AdaptiveContainer
import com.fourdfit.app.ui.components.CenteredMessage
import com.fourdfit.app.ui.components.DifficultyBadge
import com.fourdfit.app.ui.components.ExerciseAnimation
import com.fourdfit.app.ui.components.FourDTextField
import com.fourdfit.app.ui.components.GlassCard
import com.fourdfit.app.ui.components.ScreenHeader
import com.fourdfit.app.ui.components.SectionTitle
import com.fourdfit.app.ui.components.SelectableChip
import com.fourdfit.app.ui.components.Tag
import com.fourdfit.app.ui.components.entrance
import com.fourdfit.app.ui.components.screenPadding
import com.fourdfit.app.ui.theme.FourD

@Composable
fun programAccent(index: Int): List<Color> {
    val c = FourD.colors
    val sets =
        listOf(
            listOf(c.cyan, c.violet),
            listOf(c.pink, c.violet),
            listOf(c.mint, c.cyan),
            listOf(c.amber, c.pink),
            listOf(c.blue, c.mint),
        )
    return sets[index % sets.size]
}

@Composable
fun WorkoutLibraryScreen(
    onOpenExercise: (String) -> Unit,
    onOpenProgram: (String) -> Unit,
) {
    val vm = appViewModel { WorkoutLibraryViewModel(it.workoutRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val c = FourD.colors

    AdaptiveContainer {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = screenPadding(withBottomBar = true),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = "Workouts",
                    subtitle = "Guided programs and ${state.totalExercises} exercises for every level",
                    modifier = Modifier.entrance(0),
                )
            }
            item(key = "search") {
                FourDTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    label = "Search exercises or muscles",
                    leadingIcon = Icons.Rounded.Search,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    trailing =
                        if (query.isNotEmpty()) {
                            { IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear search") } }
                        } else {
                            null
                        },
                )
            }
            item(key = "filters") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LibraryFilters, key = { it.label }) { filter ->
                        SelectableChip(
                            label = filter.label,
                            selected = filter == state.filter,
                            onClick = { vm.select(filter) },
                            role = Role.Tab,
                        )
                    }
                }
            }
            if (state.programs.isNotEmpty()) {
                item(key = "programsTitle") { SectionTitle("Programs") }
                item(key = "programs") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(state.programs, key = { _, m -> m.program.id }) { index, model ->
                            ProgramCard(model, programAccent(index)) { onOpenProgram(model.program.id) }
                        }
                    }
                }
            }
            item(key = "exercisesTitle") {
                Column {
                    SectionTitle(if (query.isBlank()) "Exercises" else "Results")
                    Text(
                        if (state.exercises.size == 1) "1 exercise" else "${state.exercises.size} exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
            }
            if (!state.loading && state.exercises.isEmpty()) {
                item(key = "empty") { CenteredMessage("No matches", "Try another filter or search term.") }
            }
            items(state.exercises, key = { it.id }) { exercise ->
                ExerciseRow(exercise, onClick = { onOpenExercise(exercise.id) })
            }
        }
    }
}

@Composable
private fun ProgramCard(
    model: ProgramCardModel,
    accent: List<Color>,
    onClick: () -> Unit,
) {
    val c = FourD.colors
    GlassCard(
        modifier = Modifier.width(252.dp),
        onClick = onClick,
        onClickLabel = "Open ${model.program.title}",
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(132.dp)
                .background(Brush.linearGradient(accent.map { it.copy(alpha = 0.38f) })),
        ) {
            model.preview?.let {
                ExerciseAnimation(
                    motion = it.motion,
                    tempo = it.tempo.multiplier,
                    primary = c.textPrimary,
                    secondary = accent.first(),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                    contentDescription = null,
                )
            }
            DifficultyBadge(
                model.program.difficulty,
                Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
            )
        }
        Column(Modifier.padding(14.dp)) {
            Text(
                model.program.title,
                style = MaterialTheme.typography.titleMedium,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                model.program.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${model.program.estimatedMinutes} min, ${model.exerciseCount} exercises",
                style = MaterialTheme.typography.labelMedium,
                color = c.cyan,
            )
        }
    }
}

/** Exercise list row with a static kinetic-figure thumbnail. */
@Composable
fun ExerciseRow(
    exercise: Exercise,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    position: Int? = null,
) {
    val c = FourD.colors
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        onClickLabel = "Open ${exercise.name}",
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (position != null) {
                Text("$position", style = MaterialTheme.typography.titleMedium, color = c.textMuted, modifier = Modifier.width(26.dp))
            }
            Box(
                Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.glassFillLow),
            ) {
                ExerciseAnimation(
                    motion = exercise.motion,
                    playing = false,
                    showFloor = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    exercise.targetMuscles.take(3).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    DifficultyBadge(exercise.difficulty)
                    Tag(exercise.volumeLabel)
                }
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textMuted)
        }
    }
}
