package com.fourdfit.app.ui.onboarding

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdfit.app.data.local.ImageStorage
import com.fourdfit.app.domain.model.RegistrationData
import com.fourdfit.app.domain.repository.AuthRepository
import com.fourdfit.app.utils.AppResult
import com.fourdfit.app.utils.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { REGISTER, SIGN_IN }

data class OnboardingUiState(
    val mode: AuthMode = AuthMode.REGISTER,
    val form: ProfileFormState = ProfileFormState(),
    val email: String = "",
    val password: String = "",
    val acceptedTerms: Boolean = false,
    val signInEmail: String = "",
    val signInPassword: String = "",
    val errors: Map<FormField, String> = emptyMap(),
    val submitAttempted: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSavingPhoto: Boolean = false,
    val message: String? = null,
)

class OnboardingViewModel(
    private val auth: AuthRepository,
    private val imageStorage: ImageStorage,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun setMode(mode: AuthMode) =
        _state.update {
            it.copy(mode = mode, errors = emptyMap(), submitAttempted = false, message = null)
        }

    fun onFormChange(form: ProfileFormState) = edit { it.copy(form = form) }

    fun onEmailChange(value: String) = edit { it.copy(email = value.filterNot(Char::isWhitespace).take(254)) }

    fun onPasswordChange(value: String) = edit { it.copy(password = value.take(128)) }

    fun onTermsChange(value: Boolean) = edit { it.copy(acceptedTerms = value) }

    fun onSignInEmailChange(value: String) = edit { it.copy(signInEmail = value.filterNot(Char::isWhitespace).take(254)) }

    fun onSignInPasswordChange(value: String) = edit { it.copy(signInPassword = value.take(128)) }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isSavingPhoto = true) }
            val path = imageStorage.saveProfilePhoto(uri)
            _state.update {
                it.copy(
                    isSavingPhoto = false,
                    form = if (path != null) it.form.copy(photoPath = path) else it.form,
                    message = if (path == null) "That photo couldn't be used. Try a different one." else it.message,
                )
            }
        }
    }

    fun register() {
        val current = _state.value
        if (current.isSubmitting) return
        val errors = validateRegistration(current)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors, submitAttempted = true, message = "Check the highlighted fields.") }
            return
        }
        val form = current.form
        val data =
            RegistrationData(
                fullName = form.fullName.trim(),
                email = current.email.trim(),
                password = current.password,
                dateOfBirth = requireNotNull(form.dateOfBirth),
                gender = requireNotNull(form.gender),
                heightCm = requireNotNull(form.heightCm()),
                activityLevel = requireNotNull(form.activityLevel),
                fitnessGoal = requireNotNull(form.fitnessGoal),
                country = form.country,
                photoPath = form.photoPath,
            )
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, message = null) }
            when (val result = auth.register(data)) {
                is AppResult.Success -> _state.update { it.copy(isSubmitting = false, password = "") }
                is AppResult.Error -> _state.update { it.copy(isSubmitting = false, message = result.message) }
            }
        }
    }

    fun signIn() {
        val current = _state.value
        if (current.isSubmitting) return
        val errors = validateSignIn(current)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors, submitAttempted = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, message = null) }
            when (val result = auth.login(current.signInEmail.trim(), current.signInPassword)) {
                is AppResult.Success -> _state.update { it.copy(isSubmitting = false, signInPassword = "") }
                is AppResult.Error -> _state.update { it.copy(isSubmitting = false, message = result.message) }
            }
        }
    }

    /** Applies a change and, after the first submit attempt, re-validates live. */
    private fun edit(transform: (OnboardingUiState) -> OnboardingUiState) =
        _state.update { old ->
            val next = transform(old)
            if (!next.submitAttempted) {
                next
            } else {
                next.copy(errors = if (next.mode == AuthMode.REGISTER) validateRegistration(next) else validateSignIn(next))
            }
        }

    private fun validateRegistration(s: OnboardingUiState): Map<FormField, String> =
        ProfileFormValidator.validate(s.form) +
            buildMap<FormField, String> {
                Validators.email(s.email)?.let { put(FormField.EMAIL, it) }
                Validators.newPassword(s.password)?.let { put(FormField.PASSWORD, it) }
                if (!s.acceptedTerms) put(FormField.CONSENT, "Accept the Terms of Use and Privacy Policy to continue")
            }

    private fun validateSignIn(s: OnboardingUiState): Map<FormField, String> =
        buildMap {
            Validators.email(s.signInEmail)?.let { put(FormField.EMAIL, it) }
            Validators.existingPassword(s.signInPassword)?.let { put(FormField.PASSWORD, it) }
        }
}
