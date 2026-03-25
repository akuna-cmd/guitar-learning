package com.example.thetest1

import android.app.Application
import com.example.thetest1.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainApplication : Application() {
    lateinit var appContainer: AppContainer
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)

        applicationScope.launch {
            appContainer.warmUp()
        }
    }
}
