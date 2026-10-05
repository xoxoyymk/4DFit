package com.fourdfit.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fourdfit.app.domain.model.AppSettings
import com.fourdfit.app.domain.model.ReminderType
import com.fourdfit.app.domain.model.ThemeMode
import com.fourdfit.app.domain.model.UnitSystem
import com.fourdfit.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "fourdfit_settings")

class SettingsRepositoryImpl(
    context: Context,
) : SettingsRepository {
    private val store = context.applicationContext.settingsDataStore

    override val settings: Flow<AppSettings> =
        store.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { p ->
                AppSettings(
                    themeMode = p[THEME]?.let { v -> ThemeMode.entries.firstOrNull { it.name == v } } ?: ThemeMode.DARK,
                    units = p[UNITS]?.let { v -> UnitSystem.entries.firstOrNull { it.name == v } } ?: UnitSystem.METRIC,
                    highContrast = p[HIGH_CONTRAST] ?: false,
                    reduceMotion = p[REDUCE_MOTION] ?: false,
                    reminders = ReminderType.entries.associateWith { p[reminderKey(it)] ?: false },
                    planStartDate = p[PLAN_START]?.let { LocalDate.ofEpochDay(it) },
                )
            }.distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME] = mode.name }
    }

    override suspend fun setUnits(units: UnitSystem) {
        store.edit { it[UNITS] = units.name }
    }

    override suspend fun setHighContrast(enabled: Boolean) {
        store.edit { it[HIGH_CONTRAST] = enabled }
    }

    override suspend fun setReduceMotion(enabled: Boolean) {
        store.edit { it[REDUCE_MOTION] = enabled }
    }

    override suspend fun setReminder(
        type: ReminderType,
        enabled: Boolean,
    ) {
        store.edit { it[reminderKey(type)] = enabled }
    }

    override suspend fun setPlanStart(date: LocalDate) {
        store.edit { it[PLAN_START] = date.toEpochDay() }
    }

    override suspend fun clearUserScopedData() {
        store.edit { prefs ->
            prefs.remove(PLAN_START)
            ReminderType.entries.forEach { prefs.remove(reminderKey(it)) }
        }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val UNITS = stringPreferencesKey("units")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val PLAN_START = longPreferencesKey("plan_start_epoch_day")

        fun reminderKey(type: ReminderType) = booleanPreferencesKey("reminder_${type.name.lowercase()}")
    }
}
