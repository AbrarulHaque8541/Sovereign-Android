package com.sovereign.app

import android.app.Application
import com.sovereign.di.AppModule

class SovereignApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppModule.init(this)
    }
}