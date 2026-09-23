package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.settings.SettingsRepository
import com.example.data.learning.StudyCheckInStore
import com.example.data.stats.LearningProgress
import com.example.data.stats.LearningStats
import java.time.LocalDate
import kotlinx.coroutines.flow.first

data class WidgetSnapshot(
    val completedCards: Int,
    val goalCards: Int,
    val goalPercent: Int,
    val dueCards: Int,
    val streakDays: Int,
    val dailyWord: String,
    val dailyDefinition: String
)

internal suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot {
    val database = AppDatabase.getInstance(context)
    val cards = database.flashcardDao().getAllCards().first()
    val logs = database.studyLogDao().getAllLogs().first()
    val settings = SettingsRepository(context).settings.first()
    val progress = LearningProgress.calculate(logs, settings.dailyGoalCards)
    val dailyCard = cards.takeIf { it.isNotEmpty() }?.let { available ->
        available[Math.floorMod(LocalDate.now().toEpochDay(), available.size.toLong()).toInt()]
    }
    return WidgetSnapshot(
        completedCards = progress.completedCards,
        goalCards = progress.goalCards,
        goalPercent = (progress.goalFraction * 100).toInt().coerceIn(0, 100),
        dueCards = database.flashcardDao().getDueCardCount(System.currentTimeMillis()).first(),
        streakDays = LearningStats.calculateStreak(
            logs,
            extraActiveDays = StudyCheckInStore(context).dates.value
        ).current,
        dailyWord = dailyCard?.word?.ifBlank { "尚未新增" } ?: "尚未新增",
        dailyDefinition = dailyCard?.definition?.ifBlank { "尚未填寫解釋" }
            ?: "新增單字後會顯示在這裡"
    )
}

internal fun openAppPendingIntent(context: Context, requestCode: Int): PendingIntent =
    PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

internal fun startReviewPendingIntent(context: Context, requestCode: Int): PendingIntent =
    PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_WIDGET_ACTION, MainActivity.WIDGET_ACTION_REVIEW)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

internal fun requestAllWidgetUpdates(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    listOf(
        VocabWidgetProvider::class.java,
        DailyWordWidgetProvider::class.java,
        DueReviewWidgetProvider::class.java,
        StreakWidgetProvider::class.java
    ).forEach { providerClass ->
        val ids = manager.getAppWidgetIds(ComponentName(context, providerClass))
        if (ids.isNotEmpty()) {
            context.sendBroadcast(
                Intent(context, providerClass).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
            )
        }
    }
}
