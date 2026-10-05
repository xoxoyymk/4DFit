package com.fourdfit.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourdfit.app.di.LocalAppContainer
import com.fourdfit.app.domain.model.AppSettings
import com.fourdfit.app.ui.root.FourDFitRoot
import com.fourdfit.app.ui.theme.FourDFitTheme
import com.fourdfit.app.ui.theme.isDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val container = (application as FourDFitApp).container

        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val dark = isDarkTheme(settings.themeMode)

            DisposableEffect(dark) {
                val style =
                    if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }

            CompositionLocalProvider(LocalAppContainer provides container) {
                FourDFitTheme(
                    darkTheme = dark,
                    highContrast = settings.highContrast,
                    reduceMotion = settings.reduceMotion,
                ) {
                    FourDFitRoot()
                }
            }
        }
    }
}
