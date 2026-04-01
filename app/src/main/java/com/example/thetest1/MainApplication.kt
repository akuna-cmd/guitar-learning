package com.example.thetest1

import android.app.Application
import com.example.thetest1.di.AppDispatchers
import com.example.thetest1.di.AppWarmup
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {
    @Inject
    lateinit var appWarmup: AppWarmup

    @Inject
    lateinit var appDispatchers: AppDispatchers

    override fun onCreate() {
        super.onCreate()
        val applicationScope = CoroutineScope(SupervisorJob() + appDispatchers.default)

        applicationScope.launch {
            appWarmup.warm()
        }
    }
}
