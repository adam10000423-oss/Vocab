package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.settings.SettingsRepository
import com.example.data.settings.ReminderTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

object StudyReminderScheduler {
    private const val REQUEST_CODE_BASE = 4102

    fun schedule(context: Context, hour: Int, minute: Int = 0) {
        scheduleOne(context, ReminderTime(hour, minute))
    }

    fun scheduleAll(context: Context, times: List<ReminderTime>) {
        times.distinct().forEach { scheduleOne(context, it) }
    }

    fun replaceAll(context: Context, oldTimes: List<ReminderTime>, newTimes: List<ReminderTime>) {
        cancelAll(context, oldTimes)
        scheduleAll(context, newTimes)
    }

    private fun scheduleOne(context: Context, time: ReminderTime) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour.coerceIn(0, 23))
            set(Calendar.MINUTE, time.minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val intent = pendingIntent(context, time)
        manager.cancel(intent)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && manager.canScheduleExactAlarms() ->
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
            else -> manager.setExact(AlarmManager.RTC_WAKEUP, trigger, intent)
        }
    }

    fun cancel(context: Context) {
        cancelAll(context, listOf(ReminderTime(20, 0)))
    }

    fun cancelAll(context: Context, times: List<ReminderTime>) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        times.distinct().forEach { manager.cancel(pendingIntent(context, it)) }
        // Cancel the pre-multi-reminder PendingIntent used by older versions.
        manager.cancel(legacyPendingIntent(context))
    }

    private fun pendingIntent(context: Context, time: ReminderTime) = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE_BASE + time.hour * 60 + time.minute,
        Intent(context, StudyReminderReceiver::class.java).apply {
            action = "com.aistudio.vocabpulse.REMINDER_${time.hour}_${time.minute}"
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun legacyPendingIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE_BASE,
        Intent(context, StudyReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sendReminderIfNeeded(context)
            } finally {
                val settings = SettingsRepository(context).settings.first()
                if (settings.remindersEnabled) {
                    StudyReminderScheduler.scheduleAll(context, settings.reminderTimes)
                }
                pendingResult.finish()
            }
        }
    }

    private suspend fun sendReminderIfNeeded(context: Context) {
        val settings = SettingsRepository(context).settings.first()
        if (!settings.remindersEnabled) return
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val completed = AppDatabase.getInstance(context).studyLogDao().countLogsSince(todayStart)
        if (completed >= settings.dailyGoalCards) return
        val remaining = (settings.dailyGoalCards - completed).coerceAtLeast(0)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel("study_reminders", "每日學習提醒", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, "study_reminders")
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("今天還差 $remaining 張就達標")
            .setContentText("已完成 $completed/${settings.dailyGoalCards} 張，花幾分鐘延續學習紀錄吧！")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        val notificationId = REQUEST_NOTIFICATION_BASE + Calendar.getInstance().let {
            it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
        }
        runCatching { notificationManager.notify(notificationId, notification) }
    }

    private companion object {
        const val REQUEST_NOTIFICATION_BASE = 6200
    }
}

class StudyReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context).settings.first()
                if (settings.remindersEnabled) {
                    StudyReminderScheduler.scheduleAll(context, settings.reminderTimes)
                } else {
                    StudyReminderScheduler.cancelAll(context, settings.reminderTimes)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
