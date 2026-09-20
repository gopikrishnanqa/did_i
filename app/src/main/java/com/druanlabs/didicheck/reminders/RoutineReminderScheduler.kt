package com.druanlabs.didicheck.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.druanlabs.didicheck.DidIApplication
import com.druanlabs.didicheck.MainActivity
import com.druanlabs.didicheck.R
import com.druanlabs.didicheck.data.model.Routine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

/**
 * Per-routine reminders with repeat rules (daily / weekdays / weekly / custom days)
 * and one or more clock times. Empty / OFF = no push; open the app anytime to check.
 */
object RoutineReminderScheduler {
    const val WORK_TAG = "routine_reminder"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun sync(context: Context) {
        GentleReminderScheduler.ensureChannel(context)
        val appContext = context.applicationContext
        scope.launch {
            val work = WorkManager.getInstance(appContext)
            work.cancelAllWorkByTag(WORK_TAG)
            val app = appContext as? DidIApplication ?: return@launch
            val routines = app.repository.routines.first()
            routines.filter { it.hasReminders }.forEach { routine ->
                routine.reminderTimes.forEach { time ->
                    scheduleOne(
                        context = appContext,
                        routineId = routine.id,
                        routineName = routine.name,
                        hour = time.hour,
                        minute = time.minute,
                        days = routine.activeReminderDays(),
                    )
                }
            }
        }
    }

    fun scheduleOne(
        context: Context,
        routineId: String,
        routineName: String,
        hour: Int,
        minute: Int,
        days: Set<DayOfWeek>,
    ) {
        GentleReminderScheduler.ensureChannel(context)
        val delay = delayUntil(hour, minute, days)
        val uniqueName = workName(routineId, hour, minute)
        val daysCsv = days.sortedBy { it.value }.joinToString(",") { it.value.toString() }
        val request = OneTimeWorkRequestBuilder<RoutineReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(WORK_TAG)
            .addTag(routineTag(routineId))
            .setInputData(
                workDataOf(
                    RoutineReminderWorker.KEY_ROUTINE_ID to routineId,
                    RoutineReminderWorker.KEY_ROUTINE_NAME to routineName,
                    RoutineReminderWorker.KEY_HOUR to hour,
                    RoutineReminderWorker.KEY_MINUTE to minute,
                    RoutineReminderWorker.KEY_DAYS to daysCsv,
                ),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelForRoutine(context: Context, routineId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(routineTag(routineId))
    }

    private fun workName(routineId: String, hour: Int, minute: Int): String =
        "routine_rem_${routineId}_${hour}_$minute"

    private fun routineTag(routineId: String): String = "routine_rem_rt_$routineId"

    fun notificationId(routineId: String, hour: Int, minute: Int): Int {
        return 3000 + ("$routineId-$hour-$minute".hashCode().absoluteValue % 50_000)
    }

    fun delayUntil(hour: Int, minute: Int, days: Set<DayOfWeek>): Long {
        val allowed = if (days.isEmpty()) DayOfWeek.entries.toSet() else days
        val now = LocalDateTime.now()
        for (offset in 0..14) {
            val date = LocalDate.now().plusDays(offset.toLong())
            if (date.dayOfWeek !in allowed) continue
            val candidate = LocalDateTime.of(date, LocalTime.of(hour, minute))
            if (candidate.isAfter(now)) {
                return Duration.between(now, candidate).toMillis().coerceAtLeast(1_000L)
            }
        }
        return TimeUnit.DAYS.toMillis(1)
    }
}

class RoutineReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val routineId = inputData.getString(KEY_ROUTINE_ID) ?: return Result.success()
        val hour = inputData.getInt(KEY_HOUR, -1)
        val minute = inputData.getInt(KEY_MINUTE, -1)
        if (hour !in 0..23 || minute !in 0..59) return Result.success()

        val app = applicationContext as? DidIApplication ?: return Result.success()
        val routine = app.repository.getRoutine(routineId) ?: return Result.success()
        if (!routine.hasReminders) return Result.success()
        val stillScheduled = routine.reminderTimes.any { it.hour == hour && it.minute == minute }
        if (!stillScheduled) return Result.success()

        val days = routine.activeReminderDays()
        val todayAllowed = LocalDate.now().dayOfWeek in days
        val name = inputData.getString(KEY_ROUTINE_NAME)?.ifBlank { null } ?: routine.name
        if (todayAllowed) {
            show(routine, hour, minute, name)
        }

        RoutineReminderScheduler.scheduleOne(
            context = applicationContext,
            routineId = routineId,
            routineName = name,
            hour = hour,
            minute = minute,
            days = days,
        )
        return Result.success()
    }

    private fun show(routine: Routine, hour: Int, minute: Int, name: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        GentleReminderScheduler.ensureChannel(applicationContext)
        val id = RoutineReminderScheduler.notificationId(routine.id, hour, minute)
        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open", "checklist")
            putExtra("routineId", routine.id)
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openAction = PendingIntent.getActivity(
            applicationContext,
            id + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val preview = routine.itemPreview.ifBlank { "Open your checklist now." }
        val body = "$preview — Tap to open, or swipe to dismiss."
        val notification = NotificationCompat.Builder(applicationContext, GentleReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(name)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .addAction(0, "Open checklist", openAction)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(id, notification)
    }

    companion object {
        const val KEY_ROUTINE_ID = "routine_id"
        const val KEY_ROUTINE_NAME = "routine_name"
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
        const val KEY_DAYS = "days"
    }
}
