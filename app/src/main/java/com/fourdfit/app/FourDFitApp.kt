package com.fourdfit.app

import android.app.Application
import com.fourdfit.app.di.AppContainer
import com.fourdfit.app.notifications.NotificationHelper

class FourDFitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannels(this)
        container.start()
    }
}
