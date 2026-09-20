package com.druanlabs.didicheck.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.druanlabs.didicheck.MainActivity
import com.druanlabs.didicheck.R
import com.druanlabs.didicheck.DidIApplication
import java.util.Calendar
import java.util.concurrent.TimeUnit

object GentleReminderScheduler {
    const val CHANNEL_ID = "gentle_reminders"
    private const val MORNING_WORK = "gentle_morning"
    private const val EVENING_WORK = "gentle_evening"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gentle reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Calm nudges to check or remember everyday things"
        }
        manager.createNotificationChannel(channel)
    }

    fun sync(context: Context) {
        ensureChannel(context)
        val app = context.applicationContext as? DidIApplication
        val work = WorkManager.getInstance(context)
        // Always cancel then re-enqueue based on prefs asynchronously via workers reading prefs.
        work.cancelUniqueWork(MORNING_WORK)
        work.cancelUniqueWork(EVENING_WORK)

        // Enqueue; workers no-op if disabled.
        work.enqueueUniquePeriodicWork(
            MORNING_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<GentleReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntilHour(8), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(GentleReminderWorker.KEY_KIND to GentleReminderWorker.KIND_MORNING))
                .build(),
        )
        work.enqueueUniquePeriodicWork(
            EVENING_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<GentleReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntilHour(20), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(GentleReminderWorker.KEY_KIND to GentleReminderWorker.KIND_EVENING))
                .build(),
        )
        // Silence unused warning if app null (unit tests).
        app?.let { }
    }

    private fun delayUntilHour(hour: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now) || equals(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}

class GentleReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val kind = inputData.getString(KEY_KIND) ?: return Result.success()
        val app = applicationContext as? DidIApplication ?: return Result.success()
        val enabled = when (kind) {
            KIND_MORNING -> app.repository.isMorningReminderEnabled()
            KIND_EVENING -> app.repository.isEveningReminderEnabled()
            else -> false
        }
        if (!enabled) return Result.success()

        val (title, body) = when (kind) {
            KIND_MORNING -> "Good morning" to "Don't forget your usual things."
            else -> "Evening" to "Anything you want to remember today?"
        }
        show(title, body, if (kind == KIND_MORNING) 1001 else 1002)
        return Result.success()
    }

    private fun show(title: String, body: String, id: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        GentleReminderScheduler.ensureChannel(applicationContext)
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, GentleReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(id, notification)
    }

    companion object {
        const val KEY_KIND = "kind"
        const val KIND_MORNING = "morning"
        const val KIND_EVENING = "evening"
    }
}
