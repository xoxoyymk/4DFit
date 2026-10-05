package com.fourdfit.app.ui.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DinnerDining
import androidx.compose.material.icons.rounded.FreeBreakfast
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourdfit.app.di.appViewModel
import com.fourdfit.app.domain.model.Meal
import com.fourdfit.app.domain.model.MealType
import com.fourdfit.app.ui.components.AdaptiveContainer
import com.fourdfit.app.ui.components.DisclaimerNote
import com.fourdfit.app.ui.components.GlassCard
import com.fourdfit.app.ui.components.IconBadge
import com.fourdfit.app.ui.components.LinearMeter
import com.fourdfit.app.ui.components.LoadingState
import com.fourdfit.app.ui.components.ProgressRing
import com.fourdfit.app.ui.components.ScreenHeader
import com.fourdfit.app.ui.components.Tag
import com.fourdfit.app.ui.components.entrance
import com.fourdfit.app.ui.components.screenPadding
import com.fourdfit.app.ui.theme.FourD

private fun mealIcon(type: MealType): ImageVector =
    when (type) {
        MealType.BREAKFAST -> Icons.Rounded.FreeBreakfast
        MealType.LUNCH -> Icons.Rounded.LunchDining
        MealType.DINNER -> Icons.Rounded.DinnerDining
        MealType.SNACK -> Icons.Rounded.BakeryDining
    }

@Composable
fun NutritionScreen() {
    val vm = appViewModel { NutritionViewModel(it.nutritionRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = FourD.colors

    AdaptiveContainer {
        if (state.loading) {
            LoadingState(Modifier.padding(top = 120.dp))
            return@AdaptiveContainer
        }
        val day = state.selected
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = screenPadding(withBottomBar = true),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                ScreenHeader("Nutrition", subtitle = "Your 30-day balanced wellness plan", modifier = Modifier.entrance(0))
            }
            item(key = "disclaimer") {
                DisclaimerNote(
                    "General wellness guidance, not medical or dietary advice. Adapt meals to allergies, preferences and needs — a registered dietitian can personalise them.",
                )
            }
            item(key = "progress") {
                GlassCard(Modifier.fillMaxWidth().entrance(1)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Day ${state.currentDay} of 30", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                            Text(
                                "${state.completions.size} of ${state.totalMeals} meals checked off",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                            )
                        }
                        Tag("Balanced, no calorie counting")
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearMeter(state.completions.size / state.totalMeals.coerceAtLeast(1).toFloat(), colors = listOf(c.mint, c.cyan))
                }
            }
            item(key = "days") { DaySelector(state, vm::selectDay) }
            if (day != null) {
                item(key = "dayHeader") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Day ${day.day}", style = MaterialTheme.typography.headlineMedium, color = c.textPrimary)
                            Text(
                                when {
                                    day.day == state.currentDay -> "Today"
                                    day.day < state.currentDay -> "Earlier in your plan"
                                    else -> "Coming up"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textSecondary,
                            )
                        }
                        val done = state.doneOn(day.day)
                        ProgressRing(
                            progress = done / 4f,
                            diameter = 64.dp,
                            stroke = 7.dp,
                            colors = listOf(c.mint, c.cyan),
                            contentDescription = "$done of 4 meals done",
                        ) {
                            Text("$done/4", style = MaterialTheme.typography.labelLarge, color = c.textPrimary)
                        }
                    }
                }
                items(day.meals, key = { "${day.day}-${it.type}" }) { meal ->
                    MealCard(
                        meal = meal,
                        done = state.isDone(day.day, meal.type),
                        onToggle = { vm.setMeal(day.day, meal.type, it) },
                    )
                }
                item(key = "hydration") { TipCard(Icons.Rounded.WaterDrop, "Hydration reminder", day.hydration, listOf(c.cyan, c.blue)) }
                item(key = "nutritionTip") { TipCard(Icons.Rounded.Lightbulb, "Nutrition tip", day.nutritionTip, listOf(c.amber, c.pink)) }
                item(key = "wellness") { TipCard(Icons.Rounded.Spa, "Wellness tip", day.wellnessTip, listOf(c.violet, c.pink)) }
            }
        }
    }
}

@Composable
private fun DaySelector(
    state: NutritionUiState,
    onSelect: (Int) -> Unit,
) {
    val c = FourD.colors
    val listState = rememberLazyListState()
    LaunchedEffect(state.currentDay) {
        listState.scrollToItem((state.currentDay - 3).coerceAtLeast(0))
    }
    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.days.map { it.day }, key = { it }) { dayNumber ->
            val selected = dayNumber == state.selectedDay
            val done = state.doneOn(dayNumber)
            val shape = RoundedCornerShape(18.dp)
            Column(
                Modifier
                    .width(58.dp)
                    .clip(shape)
                    .background(
                        if (selected) {
                            Brush.verticalGradient(listOf(c.violet, c.blue))
                        } else {
                            Brush.verticalGradient(listOf(c.glassFill, c.glassFillLow))
                        },
                    ).border(
                        width = if (dayNumber == state.currentDay) 1.5.dp else 1.dp,
                        color = if (dayNumber == state.currentDay) c.cyan else c.glassBorder,
                        shape = shape,
                    ).selectable(selected = selected, role = Role.Tab, onClick = { onSelect(dayNumber) })
                    .clearAndSetSemantics {
                        contentDescription =
                            buildString {
                                append("Day $dayNumber, $done of 4 meals done")
                                if (dayNumber == state.currentDay) append(", today")
                            }
                        this.selected = selected
                    }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Day", style = MaterialTheme.typography.labelSmall, color = if (selected) Color.White else c.textSecondary)
                Text("$dayNumber", style = MaterialTheme.typography.titleLarge, color = if (selected) Color.White else c.textPrimary)
                Spacer(Modifier.height(4.dp))
                if (done == 4) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (selected) Color.White else c.mint,
                        modifier = Modifier.size(14.dp),
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(4) { i ->
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (i < done) (if (selected) Color.White else c.mint) else c.ringTrack),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealCard(
    meal: Meal,
    done: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val c = FourD.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(mealIcon(meal.type), colors = if (done) listOf(c.mint, c.cyan) else listOf(c.violet, c.blue))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(meal.type.label, style = MaterialTheme.typography.labelLarge, color = c.cyan)
                Text(meal.title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(meal.description, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (done) c.mint.copy(alpha = 0.14f) else c.glassFillLow)
                .toggleable(value = done, role = Role.Checkbox, onValueChange = onToggle)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (done) c.mint else c.textSecondary,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (done) "${meal.type.label} completed" else "Mark ${meal.type.label.lowercase()} as completed",
                style = MaterialTheme.typography.labelLarge,
                color = c.textPrimary,
            )
        }
    }
}

@Composable
private fun TipCard(
    icon: ImageVector,
    title: String,
    body: String,
    colors: List<Color>,
) {
    if (body.isBlank()) return
    val c = FourD.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, colors = colors, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        }
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
    }
}
