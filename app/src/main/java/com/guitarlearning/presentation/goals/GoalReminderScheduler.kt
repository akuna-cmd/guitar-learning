package com.guitarlearning.presentation.goals

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.guitarlearning.MainActivity
import com.guitarlearning.R
import com.guitarlearning.domain.model.Goal

private const val GOAL_REMINDER_CHANNEL_ID = "goal_reminders"
private const val EXTRA_GOAL_SYNC_ID = "goal_sync_id"
private const val EXTRA_GOAL_TITLE = "goal_title"
private const val EXTRA_GOAL_DEADLINE = "goal_deadline"

object GoalReminderScheduler {

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(GOAL_REMINDER_CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            GOAL_REMINDER_CHANNEL_ID,
            context.getString(R.string.goal_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.goal_reminder_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun schedule(context: Context, goal: Goal) {
        ensureNotificationChannel(context)
        if (goal.deadline <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, GoalReminderReceiver::class.java).apply {
            putExtra(EXTRA_GOAL_SYNC_ID, goal.syncId)
            putExtra(EXTRA_GOAL_TITLE, goal.description)
            putExtra(EXTRA_GOAL_DEADLINE, goal.deadline)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            goal.syncId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            goal.deadline,
            pendingIntent
        )
    }

    fun cancel(context: Context, goal: Goal) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, GoalReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            goal.syncId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}

class GoalReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        GoalReminderScheduler.ensureNotificationChannel(context)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val goalTitle = intent.getStringExtra(EXTRA_GOAL_TITLE).orEmpty()
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            goalTitle.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, GOAL_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.goal_reminder_notification_title))
            .setContentText(goalTitle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(goalTitle.hashCode(), notification)
    }
}
