package com.flykespice.chip8ide.data

import android.app.Application
import com.flykespice.chip8ide.di.appModule
import org.koin.core.context.startKoin

class Chip8Application : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(appModule)
        }
    }
}