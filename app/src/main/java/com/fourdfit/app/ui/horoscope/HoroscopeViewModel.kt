package com.fourdfit.app.ui.horoscope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdfit.app.domain.model.Horoscope
import com.fourdfit.app.domain.model.ZodiacSign
import com.fourdfit.app.domain.repository.HoroscopeRepository
import com.fourdfit.app.domain.repository.ProfileRepository
import com.fourdfit.app.utils.AppResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HoroscopeUiState(
    val date: LocalDate = LocalDate.now(),
    val sign: ZodiacSign? = null,
    val horoscope: Horoscope? = null,
    val refreshing: Boolean = false,
    val error: String? = null,
) {
    val loading: Boolean get() = horoscope == null && error == null
}

private data class Status(
    val refreshing: Boolean = false,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HoroscopeViewModel(
    profileRepository: ProfileRepository,
    private val horoscopeRepository: HoroscopeRepository,
) : ViewModel() {
    private val today = LocalDate.now()
    private val status = MutableStateFlow(Status())
    private val sign = profileRepository.observeProfile().map { it?.zodiacSign }.distinctUntilChanged()

    val state: StateFlow<HoroscopeUiState> =
        combine(
            sign,
            sign.flatMapLatest { s -> if (s == null) flowOf(null) else horoscopeRepository.observe(s, today) },
            status,
        ) { s, h, st -> HoroscopeUiState(today, s, h, st.refreshing, st.error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HoroscopeUiState())

    init {
        viewModelScope.launch {
            sign.filterNotNull().collect { load(it, force = false) }
        }
    }

    fun refresh() {
        val current = state.value.sign ?: return
        viewModelScope.launch { load(current, force = true) }
    }

    private suspend fun load(
        sign: ZodiacSign,
        force: Boolean,
    ) {
        status.update { it.copy(refreshing = true, error = null) }
        val result = horoscopeRepository.refresh(sign, today, force)
        status.value = Status(refreshing = false, error = (result as? AppResult.Error)?.message)
    }
}
