package com.druanlabs.didicheck.widget.checklist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Refresh checklist widgets on date/timezone/boot so they show today's run.
 */
class ChecklistWidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            -> ChecklistWidgetUpdater.requestUpdate(context)
        }
    }
}
