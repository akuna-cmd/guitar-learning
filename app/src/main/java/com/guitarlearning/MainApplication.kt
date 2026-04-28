package com.guitarlearning

import android.app.Application
import com.guitarlearning.di.AppDispatchers
import com.guitarlearning.di.AppWarmup
import com.guitarlearning.presentation.goals.GoalReminderScheduler
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
        GoalReminderScheduler.ensureNotificationChannel(this)
        val applicationScope = CoroutineScope(SupervisorJob() + appDispatchers.default)

        applicationScope.launch {
            appWarmup.warm()
        }
    }
}
