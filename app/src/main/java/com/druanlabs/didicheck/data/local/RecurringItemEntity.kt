package com.druanlabs.didicheck.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_items")
data class RecurringItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val frequency: String,
    val intervalCount: Int,
    val customUnit: String,
    /** Comma-separated ISO day-of-week values (1=Mon … 7=Sun). */
    val weekDays: String?,
    val dayOfMonth: Int?,
    val startDateEpochDay: Long,
    val nextDueEpochDay: Long,
    val reminderHour: Int?,
    val reminderMinute: Int?,
    /** Comma-separated days-before-due (e.g. "5,3,1,0"). */
    val remindDaysBefore: String?,
    val notes: String?,
    val lastCompletedEpochDay: Long?,
    val createdAt: Long,
    val archived: Boolean,
)
