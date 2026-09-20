package com.druanlabs.didicheck.data.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class RecurringCategory {
    ROUTINE,
    BILL,
    REMINDER,
}

enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY,
    CUSTOM,
}

/**
 * A flexible recurring responsibility — separate from Before I Go checklists.
 */
data class RecurringItem(
    val id: String,
    val title: String,
    val category: RecurringCategory,
    val frequency: RecurrenceFrequency,
    /** Every N units (days for DAILY/CUSTOM-days, months for CUSTOM-months, etc.). */
    val intervalCount: Int = 1,
    /** For CUSTOM: DAYS or MONTHS. */
    val customUnit: CustomUnit = CustomUnit.DAYS,
    /** ISO day-of-week values for WEEKLY (1=Mon … 7=Sun). */
    val weekDays: Set<DayOfWeek> = emptySet(),
    /** Day of month for MONTHLY (1–31). Clamped for short months. */
    val dayOfMonth: Int? = null,
    val startDate: LocalDate,
    val nextDueDate: LocalDate,
    val reminderHour: Int? = 9,
    val reminderMinute: Int? = 0,
    /** Days before due to notify (0 = on the day). */
    val remindDaysBefore: List<Int> = emptyList(),
    val notes: String? = null,
    val lastCompletedDate: LocalDate? = null,
    val createdAt: Long,
    val archived: Boolean = false,
) {
    fun daysUntilDue(today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, nextDueDate)

    fun isOverdue(today: LocalDate = LocalDate.now()): Boolean =
        nextDueDate.isBefore(today)

    fun isDueToday(today: LocalDate = LocalDate.now()): Boolean =
        nextDueDate == today

    fun dueLabel(today: LocalDate = LocalDate.now()): String {
        val days = daysUntilDue(today)
        return when {
            days < 0 -> "Overdue by ${-days} day${if (days == -1L) "" else "s"}"
            days == 0L -> "Due today"
            days == 1L -> "Due tomorrow"
            else -> "Due in $days days"
        }
    }
}

enum class CustomUnit { DAYS, MONTHS }

data class RecurringDraft(
    val id: String = "",
    val title: String = "",
    val category: RecurringCategory = RecurringCategory.ROUTINE,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.DAILY,
    val intervalCount: Int = 1,
    val customUnit: CustomUnit = CustomUnit.DAYS,
    val weekDays: Set<DayOfWeek> = emptySet(),
    val dayOfMonth: Int = LocalDate.now().dayOfMonth.coerceIn(1, 28),
    val startDate: LocalDate = LocalDate.now(),
    val nextDueDate: LocalDate = LocalDate.now(),
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val remindDaysBefore: List<Int> = listOf(0),
    val notes: String = "",
)
