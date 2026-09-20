package com.druanlabs.didicheck.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
    val lastCompletedAt: Long?,
    /** OFF, DAILY, WEEKDAYS, WEEKLY, CUSTOM */
    val reminderRepeat: String = "OFF",
    /** CSV of DayOfWeek values (1=Mon … 7=Sun). Null/empty with DAILY = every day. */
    val reminderDays: String? = null,
    /** When true, offer a photo when finishing this checklist. */
    val askPhotoOnComplete: Boolean = false,
)

@Entity(
    tableName = "routine_items",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("routineId")],
)
data class RoutineItemEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    val label: String,
    val kind: String,
    val sortOrder: Int,
    val photoRequired: Boolean = false,
)

@Entity(
    tableName = "routine_reminder_times",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("routineId")],
)
data class RoutineReminderTimeEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    val hour: Int,
    val minute: Int,
    val sortOrder: Int = 0,
)

@Entity(tableName = "quick_actions")
data class QuickActionEntity(
    @PrimaryKey val id: String,
    val label: String,
    val lastUsedAt: Long,
    val useCount: Int,
    val createdAt: Long,
    val pinned: Boolean = false,
    val pinOrder: Int = 0,
)

@Entity(tableName = "completions")
data class CompletionEntity(
    @PrimaryKey val id: String,
    val actionLabel: String,
    val timestamp: Long,
    val source: String,
    val routineName: String?,
    /** Newline-separated checked items for routine summaries. */
    val detailItems: String? = null,
    /** Absolute path in app-private storage; may be missing if file was cleared. */
    val photoPath: String? = null,
)

@Entity(
    tableName = "checklist_runs",
    indices = [
        Index(value = ["routineId", "epochDay"], unique = true),
        Index("routineId"),
    ],
)
data class ChecklistRunEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    /** LocalDate.toEpochDay() for the calendar day this run belongs to. */
    val epochDay: Long,
    val createdAt: Long,
)

@Entity(
    tableName = "checklist_run_items",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("runId"), Index("routineItemId")],
)
data class ChecklistRunItemEntity(
    @PrimaryKey val id: String,
    val runId: String,
    val routineItemId: String,
    val label: String,
    val sortOrder: Int,
    val photoRequired: Boolean = false,
    /** Null = not ticked; otherwise epoch millis when ticked. */
    val checkedAt: Long? = null,
)
