package com.example.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class SnapshotWidgetProvider : AppWidgetProvider() {
    final override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = loadWidgetSnapshot(context)
                ids.forEach { updateWidget(context, manager, it, snapshot) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    protected abstract fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        snapshot: WidgetSnapshot
    )
}

class DailyWordWidgetProvider : SnapshotWidgetProvider() {
    override fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        snapshot: WidgetSnapshot
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_daily_word)
        views.setTextViewText(R.id.daily_word_text, snapshot.dailyWord)
        views.setTextViewText(R.id.daily_word_definition, snapshot.dailyDefinition)
        views.setOnClickPendingIntent(R.id.daily_word_root, openAppPendingIntent(context, 4201))
        manager.updateAppWidget(widgetId, views)
    }
}

class DueReviewWidgetProvider : SnapshotWidgetProvider() {
    override fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        snapshot: WidgetSnapshot
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_due_review)
        views.setTextViewText(R.id.due_review_count, snapshot.dueCards.toString())
        views.setTextViewText(
            R.id.due_review_status,
            if (snapshot.dueCards > 0) "待複習" else "已完成"
        )
        views.setOnClickPendingIntent(R.id.due_review_root, startReviewPendingIntent(context, 4302))
        manager.updateAppWidget(widgetId, views)
    }
}

class StreakWidgetProvider : SnapshotWidgetProvider() {
    override fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        snapshot: WidgetSnapshot
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_streak)
        views.setTextViewText(R.id.streak_days, "${snapshot.streakDays} 天")
        views.setTextViewText(
            R.id.streak_today,
            if (snapshot.completedCards >= snapshot.goalCards) "今日目標已完成"
            else "今日 ${snapshot.completedCards} / ${snapshot.goalCards}"
        )
        views.setProgressBar(R.id.streak_progress, 100, snapshot.goalPercent, false)
        views.setOnClickPendingIntent(R.id.streak_root, startReviewPendingIntent(context, 4402))
        manager.updateAppWidget(widgetId, views)
    }
}
