package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VocabWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = loadWidgetSnapshot(context)

                val startReview = PendingIntent.getActivity(
                    context,
                    REQUEST_START_REVIEW,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(MainActivity.EXTRA_WIDGET_ACTION, MainActivity.WIDGET_ACTION_REVIEW)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                ids.forEach { id ->
                    val views = RemoteViews(context.packageName, R.layout.widget_vocabulary)
                    views.setTextViewText(
                        R.id.widget_progress_text,
                        "今日 ${snapshot.completedCards} / ${snapshot.goalCards} 張"
                    )
                    views.setProgressBar(
                        R.id.widget_progress,
                        100,
                        snapshot.goalPercent,
                        false
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, startReview)
                    manager.updateAppWidget(id, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val REQUEST_START_REVIEW = 4102

        fun requestUpdate(context: Context) {
            requestAllWidgetUpdates(context)
        }
    }
}
