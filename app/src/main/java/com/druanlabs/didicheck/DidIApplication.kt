package com.druanlabs.didicheck

import android.app.Application
import androidx.work.WorkManager
import com.druanlabs.didicheck.data.MemoryRepository
import com.druanlabs.didicheck.data.local.DidIDatabase
import com.druanlabs.didicheck.data.local.SettingsStore
import com.druanlabs.didicheck.reminders.GentleReminderScheduler
import com.druanlabs.didicheck.reminders.RoutineReminderScheduler
import com.druanlabs.didicheck.widget.DidIWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DidIApplication : Application() {
    lateinit var repository: MemoryRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val database = DidIDatabase.create(this)
        repository = MemoryRepository(
            database = database,
            settings = SettingsStore(this),
            scope = appScope,
        )
        // Cancel legacy recurring-item work if it was scheduled by an older build.
        WorkManager.getInstance(this).cancelUniqueWork("recurring_due_daily")
        GentleReminderScheduler.sync(this)
        RoutineReminderScheduler.sync(this)
        appScope.launch {
            DidIWidgetUpdater.requestUpdate(this@DidIApplication)
        }
    }
}
