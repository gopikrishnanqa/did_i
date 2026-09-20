package com.druanlabs.didicheck.data.recurrence

import com.druanlabs.didicheck.data.model.CustomUnit
import com.druanlabs.didicheck.data.model.RecurrenceFrequency
import com.druanlabs.didicheck.data.model.RecurringItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.min

/**
 * Calculates the next occurrence after a completion (or from a start date).
 * Handles short months for monthly/quarterly schedules.
 */
object RecurrenceEngine {

    fun nextAfterCompletion(item: RecurringItem, completedOn: LocalDate = LocalDate.now()): LocalDate {
        return nextFrom(item, after = completedOn)
    }

    fun initialNextDue(item: RecurringItem, today: LocalDate = LocalDate.now()): LocalDate {
        return when {
            item.nextDueDate >= today -> item.nextDueDate
            else -> nextFrom(item, after = item.nextDueDate.minusDays(1), stopAtOrAfter = today)
                ?: item.nextDueDate
        }
    }

    fun nextFrom(
        item: RecurringItem,
        after: LocalDate,
        stopAtOrAfter: LocalDate? = null,
    ): LocalDate {
        var cursor = after
        // Safety cap to avoid infinite loops on bad configs
        repeat(800) {
            cursor = step(item, cursor)
            if (stopAtOrAfter == null || !cursor.isBefore(stopAtOrAfter)) {
                return cursor
            }
        }
        return cursor
    }

    private fun step(item: RecurringItem, after: LocalDate): LocalDate {
        val n = item.intervalCount.coerceAtLeast(1)
        return when (item.frequency) {
            RecurrenceFrequency.DAILY -> after.plusDays(n.toLong())
            RecurrenceFrequency.WEEKLY -> nextWeekly(after, item.weekDays.ifEmpty {
                setOf(after.dayOfWeek)
            })
            RecurrenceFrequency.MONTHLY -> nextMonthly(after, item.dayOfMonth ?: after.dayOfMonth, 1)
            RecurrenceFrequency.QUARTERLY -> nextMonthly(after, item.dayOfMonth ?: after.dayOfMonth, 3)
            RecurrenceFrequency.YEARLY -> nextYearly(after, item.dayOfMonth ?: after.dayOfMonth, after.monthValue)
            RecurrenceFrequency.CUSTOM -> when (item.customUnit) {
                CustomUnit.DAYS -> after.plusDays(n.toLong())
                CustomUnit.MONTHS -> nextMonthly(after, item.dayOfMonth ?: after.dayOfMonth, n)
            }
        }
    }

    private fun nextWeekly(after: LocalDate, days: Set<DayOfWeek>): LocalDate {
        val ordered = days.sortedBy { it.value }
        // Search the next 14 days for the soonest selected weekday after `after`
        for (offset in 1..14) {
            val candidate = after.plusDays(offset.toLong())
            if (candidate.dayOfWeek in days) return candidate
        }
        // Fallback: next occurrence of the first selected day
        val target = ordered.first()
        return after.with(TemporalAdjusters.next(target))
    }

    /**
     * Advance by [monthStep] months, landing on [desiredDay] clamped to month length.
     * Example: desired 31 in February → 28/29.
     */
    private fun nextMonthly(after: LocalDate, desiredDay: Int, monthStep: Int): LocalDate {
        val base = after.plusMonths(monthStep.toLong())
        return clampDay(base.year, base.monthValue, desiredDay)
    }

    private fun nextYearly(after: LocalDate, desiredDay: Int, month: Int): LocalDate {
        var year = after.year + 1
        // If we haven't reached this year's anniversary yet from `after`, step from after's month
        val thisYear = clampDay(after.year, month, desiredDay)
        if (thisYear.isAfter(after)) return thisYear
        return clampDay(year, month, desiredDay)
    }

    fun clampDay(year: Int, month: Int, desiredDay: Int): LocalDate {
        val first = LocalDate.of(year, month, 1)
        val max = first.lengthOfMonth()
        return LocalDate.of(year, month, min(desiredDay.coerceIn(1, 31), max))
    }
}
