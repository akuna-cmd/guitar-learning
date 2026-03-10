package com.example.thetest1

import android.app.Application
import com.example.thetest1.di.AppContainer

class MainApplication : Application() {
    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}