package com.fourdfit.app.ui.horoscope

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourdfit.app.data.api.HoroscopeEngine
import com.fourdfit.app.di.appViewModel
import com.fourdfit.app.domain.model.Horoscope
import com.fourdfit.app.ui.components.AdaptiveContainer
import com.fourdfit.app.ui.components.DisclaimerNote
import com.fourdfit.app.ui.components.ErrorState
import com.fourdfit.app.ui.components.GlassCard
import com.fourdfit.app.ui.components.IconBadge
import com.fourdfit.app.ui.components.IconCircleButton
import com.fourdfit.app.ui.components.LoadingState
import com.fourdfit.app.ui.components.ScreenHeader
import com.fourdfit.app.ui.components.Tag
import com.fourdfit.app.ui.components.ZodiacEmblem
import com.fourdfit.app.ui.components.entrance
import com.fourdfit.app.ui.components.screenPadding
import com.fourdfit.app.ui.theme.FourD
import com.fourdfit.app.utils.DateUtils

@Composable
fun HoroscopeScreen() {
    val vm = appViewModel { HoroscopeViewModel(it.profileRepository, it.horoscopeRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = FourD.colors

    AdaptiveContainer {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = screenPadding(withBottomBar = true),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = "Daily Horoscope",
                    subtitle = state.date.format(DateUtils.longDate),
                    modifier = Modifier.entrance(0),
                ) {
                    IconCircleButton(Icons.Rounded.Refresh, "Refresh horoscope", vm::refresh, enabled = !state.refreshing)
                }
            }
            item(key = "disclaimer") { DisclaimerNote(HoroscopeEngine.DISCLAIMER) }
            state.sign?.let { sign ->
                item(key = "sign") {
                    GlassCard(Modifier.fillMaxWidth().entrance(1), glow = true) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            ZodiacEmblem(sign, diameter = 148.dp)
                            Spacer(Modifier.height(10.dp))
                            Text(sign.displayName, style = MaterialTheme.typography.displaySmall, color = c.textPrimary)
                            Text(sign.dateRange, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                            Spacer(Modifier.height(8.dp))
                            Tag("${sign.element} sign", color = c.cyan)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                sign.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            val horoscope = state.horoscope
            when {
                horoscope != null -> {
                    item(
                        key = "general",
                    ) { ReadingCard(Icons.Rounded.AutoAwesome, "Today in general", horoscope.general, listOf(c.violet, c.pink)) }
                    item(
                        key = "motivation",
                    ) { ReadingCard(Icons.Rounded.Bolt, "Motivation", horoscope.motivation, listOf(c.amber, c.pink)) }
                    item(
                        key = "wellness",
                    ) { ReadingCard(Icons.Rounded.Spa, "Wellness message", horoscope.wellness, listOf(c.mint, c.cyan)) }
                    item(key = "lucky") { LuckyRow(horoscope) }
                    if (state.error != null) {
                        item(key = "staleNote") {
                            Text(
                                "Showing today's saved reading. ${state.error}",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textMuted,
                            )
                        }
                    }
                }
                state.error != null -> item(key = "error") { ErrorState(state.error ?: "", onRetry = vm::refresh) }
                else -> item(key = "loading") { LoadingState(label = "Fetching today's reading") }
            }
        }
    }
}

@Composable
private fun ReadingCard(
    icon: ImageVector,
    title: String,
    body: String,
    colors: List<Color>,
) {
    val c = FourD.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, colors = colors, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        }
        Spacer(Modifier.height(10.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
    }
}

@Composable
private fun LuckyRow(horoscope: Horoscope) {
    val c = FourD.colors
    val swatch = runCatching { Color(android.graphics.Color.parseColor(horoscope.luckyColorHex)) }.getOrDefault(c.violet)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(
            Modifier
                .weight(1f)
                .clearAndSetSemantics { contentDescription = "Lucky colour: ${horoscope.luckyColor}" },
        ) {
            Text("Lucky colour", style = MaterialTheme.typography.labelLarge, color = c.textSecondary)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(1.dp, c.glassBorder, CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Text(horoscope.luckyColor, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            }
        }
        GlassCard(
            Modifier
                .weight(1f)
                .clearAndSetSemantics { contentDescription = "Lucky number: ${horoscope.luckyNumber}" },
        ) {
            Text("Lucky number", style = MaterialTheme.typography.labelLarge, color = c.textSecondary)
            Spacer(Modifier.height(4.dp))
            Text("${horoscope.luckyNumber}", style = MaterialTheme.typography.displaySmall, color = c.amber)
        }
    }
}
