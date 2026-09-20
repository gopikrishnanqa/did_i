package com.druanlabs.didicheck.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.druanlabs.didicheck.DidIApplication
import com.druanlabs.didicheck.MainActivity
import com.druanlabs.didicheck.R
import com.druanlabs.didicheck.data.model.CompletionSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DidICheckWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id ->
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_MARK) {
            val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
            if (label.isBlank()) return
            val app = context.applicationContext as? DidIApplication ?: return
            scope.launch {
                app.repository.markDone(label, CompletionSource.QUICK)
                DidIWidgetUpdater.requestUpdate(context)
            }
        }
    }

    companion object {
        const val ACTION_MARK = "com.druanlabs.didicheck.widget.MARK"
        const val EXTRA_LABEL = "label"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val app = context.applicationContext as? DidIApplication
            val views = RemoteViews(context.packageName, R.layout.widget_didi)
            views.setOnClickPendingIntent(
                R.id.widget_title,
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            views.setOnClickPendingIntent(
                R.id.widget_add,
                PendingIntent.getActivity(
                    context,
                    1,
                    Intent(context, MainActivity::class.java).putExtra("open", "did_i_do_it"),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )

            val buttonIds = listOf(
                R.id.widget_action_1,
                R.id.widget_action_2,
                R.id.widget_action_3,
            )
            if (app == null) {
                buttonIds.forEach { views.setTextViewText(it, "—") }
                manager.updateAppWidget(widgetId, views)
                return
            }

            scope.launch {
                val pinned = app.repository.quickActions.first()
                    .filter { it.pinned }
                    .take(3)
                buttonIds.forEachIndexed { index, viewId ->
                    val action = pinned.getOrNull(index)
                    if (action == null) {
                        views.setViewVisibility(viewId, android.view.View.GONE)
                    } else {
                        views.setViewVisibility(viewId, android.view.View.VISIBLE)
                        views.setTextViewText(viewId, action.label)
                        val markIntent = Intent(context, DidICheckWidgetProvider::class.java).apply {
                            this.action = ACTION_MARK
                            putExtra(EXTRA_LABEL, action.label)
                        }
                        views.setOnClickPendingIntent(
                            viewId,
                            PendingIntent.getBroadcast(
                                context,
                                100 + index,
                                markIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                            ),
                        )
                    }
                }
                manager.updateAppWidget(widgetId, views)
            }
        }
    }
}

object DidIWidgetUpdater {
    fun requestUpdate(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, DidICheckWidgetProvider::class.java),
        )
        if (ids.isEmpty()) return
        val intent = Intent(context, DidICheckWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }
}
