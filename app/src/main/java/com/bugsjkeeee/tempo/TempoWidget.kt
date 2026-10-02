package com.bugsjkeeee.tempo

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Виджет рабочего стола: «Таймер» — старт с последними настройками, «Генератор» — вкладка генератора. */
class TempoWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_tempo).apply {
            setOnClickPendingIntent(R.id.widget_timer, open(context, ACTION_TIMER, 1))
            setOnClickPendingIntent(R.id.widget_random, open(context, ACTION_RANDOM, 2))
        }
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun open(context: Context, action: String, code: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction("com.bugsjkeeee.tempo.widget.$action")
            .putExtra(EXTRA_ACTION, action)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val EXTRA_ACTION = "tempo_action"
        const val ACTION_TIMER = "timer"
        const val ACTION_RANDOM = "random"
    }
}
