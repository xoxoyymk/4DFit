package com.fourdfit.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdfit.app.data.sensor.StepCounterManager
import com.fourdfit.app.domain.model.AppSettings
import com.fourdfit.app.domain.model.ReminderType
import com.fourdfit.app.domain.model.ThemeMode
import com.fourdfit.app.domain.model.UnitSystem
import com.fourdfit.app.domain.repository.AuthRepository
import com.fourdfit.app.domain.repository.SettingsRepository
import com.fourdfit.app.utils.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val busy: Boolean = false,
    val message: String? = null,
)

private data class SettingsStatus(
    val busy: Boolean = false,
    val message: String? = null,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val stepCounter: StepCounterManager,
) : ViewModel() {
    private val status = MutableStateFlow(SettingsStatus())

    val state: StateFlow<SettingsUiState> =
        combine(settingsRepository.settings, status) { s, st ->
            SettingsUiState(s, st.busy, st.message)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(mode: ThemeMode) = launchIo { settingsRepository.setThemeMode(mode) }

    fun setUnits(units: UnitSystem) = launchIo { settingsRepository.setUnits(units) }

    fun setHighContrast(enabled: Boolean) = launchIo { settingsRepository.setHighContrast(enabled) }

    fun setReduceMotion(enabled: Boolean) = launchIo { settingsRepository.setReduceMotion(enabled) }

    fun setReminder(
        type: ReminderType,
        enabled: Boolean,
    ) = launchIo { settingsRepository.setReminder(type, enabled) }

    fun showMessage(message: String) = status.update { it.copy(message = message) }

    fun clearMessage() = status.update { it.copy(message = null) }

    fun logout() {
        viewModelScope.launch {
            status.update { it.copy(busy = true) }
            stepCounter.resetForLogout()
            authRepository.logout()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            status.update { it.copy(busy = true, message = null) }
            when (val result = authRepository.deleteAccount()) {
                is AppResult.Success -> stepCounter.resetForLogout()
                is AppResult.Error -> status.update { SettingsStatus(busy = false, message = "Account not deleted: ${result.message}") }
            }
        }
    }

    private fun launchIo(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
