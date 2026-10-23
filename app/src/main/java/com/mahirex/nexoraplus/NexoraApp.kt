package com.mahirex.nexoraplus

import android.app.Application
import com.mahirex.nexoraplus.di.AppContainer

class NexoraApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }
}
