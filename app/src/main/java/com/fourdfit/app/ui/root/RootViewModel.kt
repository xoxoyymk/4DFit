package com.fourdfit.app.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdfit.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface SessionState {
    data object Loading : SessionState

    data object LoggedOut : SessionState

    data object LoggedIn : SessionState
}

class RootViewModel(
    auth: AuthRepository,
) : ViewModel() {
    val session: StateFlow<SessionState> =
        auth.isLoggedIn
            .map { loggedIn -> if (loggedIn) SessionState.LoggedIn else SessionState.LoggedOut }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)
}
