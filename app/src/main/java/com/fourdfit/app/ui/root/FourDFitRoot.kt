package com.fourdfit.app.ui.root

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourdfit.app.di.LocalAppContainer
import com.fourdfit.app.di.appViewModel
import com.fourdfit.app.navigation.AppNavHost
import com.fourdfit.app.ui.components.FourDBackground
import com.fourdfit.app.ui.onboarding.OnboardingScreen

@Composable
fun FourDFitRoot() {
    val vm = appViewModel { RootViewModel(it.authRepository) }
    val session by vm.session.collectAsStateWithLifecycle()

    FourDBackground(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = session,
            transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(250)) },
            label = "session",
        ) { state ->
            when (state) {
                SessionState.Loading -> Box(Modifier.fillMaxSize())
                SessionState.LoggedOut -> OnboardingScreen()
                SessionState.LoggedIn -> SignedInApp()
            }
        }
    }
}

@Composable
private fun SignedInApp() {
    val container = LocalAppContainer.current
    // Pull fresh server data and push anything recorded offline.
    LaunchedEffect(Unit) { container.syncManager.syncAll() }
    // Count steps only while the app is in the foreground; the hardware keeps counting otherwise.
    LifecycleResumeEffect(Unit) {
        container.stepCounter.refresh()
        onPauseOrDispose { container.stepCounter.stop() }
    }
    AppNavHost()
}
