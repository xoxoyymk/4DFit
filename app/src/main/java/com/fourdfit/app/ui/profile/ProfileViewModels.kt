package com.fourdfit.app.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdfit.app.data.local.ImageStorage
import com.fourdfit.app.domain.model.ProgressSummary
import com.fourdfit.app.domain.model.UnitSystem
import com.fourdfit.app.domain.model.UserProfile
import com.fourdfit.app.domain.repository.NutritionRepository
import com.fourdfit.app.domain.repository.ProfileRepository
import com.fourdfit.app.domain.repository.SettingsRepository
import com.fourdfit.app.domain.repository.WorkoutRepository
import com.fourdfit.app.domain.usecase.ProgressCalculator
import com.fourdfit.app.ui.onboarding.FormField
import com.fourdfit.app.ui.onboarding.ProfileFormState
import com.fourdfit.app.ui.onboarding.ProfileFormValidator
import com.fourdfit.app.utils.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    val units: UnitSystem = UnitSystem.METRIC,
    val summary: ProgressSummary = ProgressSummary.EMPTY,
    val mealsDone: Int = 0,
    val planDay: Int = 1,
)

class ProfileViewModel(
    profileRepository: ProfileRepository,
    workoutRepository: WorkoutRepository,
    nutritionRepository: NutritionRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<ProfileUiState> =
        combine(
            profileRepository.observeProfile(),
            settingsRepository.settings,
            workoutRepository.observeHistory(),
            nutritionRepository.observeCompletions(),
            nutritionRepository.observeCurrentDay(),
        ) { profile, settings, history, meals, day ->
            ProfileUiState(false, profile, settings.units, ProgressCalculator.summarize(history, LocalDate.now()), meals.size, day)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())
}

data class EditProfileUiState(
    val loaded: Boolean = false,
    val email: String = "",
    val form: ProfileFormState = ProfileFormState(),
    val errors: Map<FormField, String> = emptyMap(),
    val submitAttempted: Boolean = false,
    val saving: Boolean = false,
    val savingPhoto: Boolean = false,
    val message: String? = null,
    val saved: Boolean = false,
)

class EditProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val imageStorage: ImageStorage,
) : ViewModel() {
    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()
    private var original: UserProfile? = null

    init {
        viewModelScope.launch {
            val profile = profileRepository.observeProfile().filterNotNull().first()
            val units = settingsRepository.settings.first().units
            original = profile
            _state.value = EditProfileUiState(loaded = true, email = profile.email, form = ProfileFormState.from(profile, units))
        }
    }

    fun onFormChange(form: ProfileFormState) =
        _state.update {
            it.copy(form = form, errors = if (it.submitAttempted) ProfileFormValidator.validate(form) else emptyMap())
        }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(savingPhoto = true) }
            val path = imageStorage.saveProfilePhoto(uri)
            _state.update {
                it.copy(
                    savingPhoto = false,
                    form = if (path != null) it.form.copy(photoPath = path) else it.form,
                    message = if (path == null) "That photo couldn't be used. Try a different one." else null,
                )
            }
        }
    }

    fun save() {
        val current = _state.value
        val base = original ?: return
        if (current.saving) return
        val errors = ProfileFormValidator.validate(current.form)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors, submitAttempted = true, message = "Check the highlighted fields.") }
            return
        }
        val form = current.form
        val updated =
            base.copy(
                fullName = form.fullName.trim(),
                dateOfBirth = requireNotNull(form.dateOfBirth),
                gender = requireNotNull(form.gender),
                heightCm = requireNotNull(form.heightCm()),
                activityLevel = requireNotNull(form.activityLevel),
                fitnessGoal = requireNotNull(form.fitnessGoal),
                country = form.country,
                photoPath = form.photoPath,
            )
        viewModelScope.launch {
            _state.update { it.copy(saving = true, message = null) }
            when (val result = profileRepository.updateProfile(updated)) {
                is AppResult.Success -> {
                    imageStorage.cleanupExcept(updated.photoPath)
                    settingsRepository.setUnits(form.heightUnit)
                    _state.update { it.copy(saving = false, saved = true) }
                }
                is AppResult.Error -> _state.update { it.copy(saving = false, message = result.message) }
            }
        }
    }
}
