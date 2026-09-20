package com.druanlabs.didicheck.data.model

import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

data class Routine(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val lastCompletedAt: Long?,
    val items: List<RoutineItem>,
    /** Empty = no push reminders; open the app anytime to check. */
    val reminderTimes: List<RoutineReminderTime> = emptyList(),
    val reminderRepeat: ReminderRepeat = ReminderRepeat.OFF,
    /** Used for WEEKLY / CUSTOM (and stored for WEEKDAYS). Empty + DAILY = every day. */
    val reminderDays: Set<DayOfWeek> = emptySet(),
    /** When true, finishing the checklist offers/requires a completion photo. */
    val askPhotoOnComplete: Boolean = false,
) {
    val takePreview: String
        get() = items.filter { it.kind == ItemKind.TAKE }
            .sortedBy { it.sortOrder }
            .take(3)
            .joinToString(" · ") { it.label }

    val itemPreview: String
        get() = items.sortedBy { it.sortOrder }
            .take(4)
            .joinToString(" · ") { it.label }

    val hasReminders: Boolean
        get() = reminderRepeat != ReminderRepeat.OFF && reminderTimes.isNotEmpty()

    /** Days when a reminder may fire. */
    fun activeReminderDays(): Set<DayOfWeek> = when (reminderRepeat) {
        ReminderRepeat.OFF -> emptySet()
        ReminderRepeat.DAILY -> DayOfWeek.entries.toSet()
        ReminderRepeat.WEEKDAYS -> setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )
        ReminderRepeat.WEEKLY,
        ReminderRepeat.CUSTOM -> reminderDays.ifEmpty {
            setOf(DayOfWeek.MONDAY)
        }
    }

    fun reminderScheduleLabel(): String {
        if (!hasReminders) return "No reminders"
        val days = activeReminderDays().sortedBy { it.value }
        val dayText = when {
            days.size == 7 -> "Every day"
            days == setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            ) -> "Every Mon, Tue, Wed, Thu, Fri"
            days.size == 1 -> "Every ${days.first().getDisplayName(TextStyle.FULL, Locale.getDefault())}"
            else -> "Every " + days.joinToString(", ") {
                it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }
        }
        val times = reminderTimes.joinToString(" · ") { it.label() }
        return "$dayText · $times"
    }
}

enum class ReminderRepeat {
    OFF,
    DAILY,
    WEEKDAYS,
    WEEKLY,
    CUSTOM,
}

data class RoutineReminderTime(
    val id: String,
    val hour: Int,
    val minute: Int,
) {
    fun label(): String {
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val amPm = if (hour < 12) "AM" else "PM"
        return "%d:%02d %s".format(h12, minute, amPm)
    }
}

data class RoutineItem(
    val id: String,
    val routineId: String,
    val label: String,
    val kind: ItemKind,
    val sortOrder: Int,
    val photoRequired: Boolean = false,
)

enum class ItemKind { TAKE, CONFIRM }

data class QuickAction(
    val id: String,
    val label: String,
    val lastUsedAt: Long,
    val useCount: Int,
    val pinned: Boolean = false,
    val pinOrder: Int = 0,
)

data class Completion(
    val id: String,
    val actionLabel: String,
    val timestamp: Long,
    val source: CompletionSource,
    val routineName: String?,
    val detailItems: List<String> = emptyList(),
    /** App-private file path for optional photo proof. */
    val photoPath: String? = null,
) {
    val isRoutineSummary: Boolean
        get() = source == CompletionSource.ROUTINE && detailItems.isNotEmpty()

    val detailPreview: String?
        get() = detailItems.take(4).joinToString(" · ").ifBlank { null }

    val hasPhotoProof: Boolean get() = !photoPath.isNullOrBlank()
}

enum class CompletionSource { QUICK, ROUTINE }

/**
 * Local-first suggestion for later smart prompts.
 * No cloud / AI — based on time of day and usage.
 */
data class GentleSuggestion(
    val title: String,
    val body: String,
    val routineId: String? = null,
    val actionLabel: String? = null,
)

/** One calendar-day instance of a routine checklist (widget + future in-app sync). */
data class ChecklistRun(
    val id: String,
    val routineId: String,
    val routineName: String,
    val epochDay: Long,
    val items: List<ChecklistRunItem>,
) {
    val checkedCount: Int get() = items.count { it.isChecked }
    val totalCount: Int get() = items.size
    val progressLabel: String get() = "$checkedCount/$totalCount"
}

data class ChecklistRunItem(
    val id: String,
    val runId: String,
    val routineItemId: String,
    val label: String,
    val sortOrder: Int,
    val photoRequired: Boolean,
    val checkedAt: Long?,
) {
    val isChecked: Boolean get() = checkedAt != null
}
