package com.druanlabs.didicheck.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RoutineEntity::class,
        RoutineItemEntity::class,
        RoutineReminderTimeEntity::class,
        QuickActionEntity::class,
        CompletionEntity::class,
        RecurringItemEntity::class,
        ChecklistRunEntity::class,
        ChecklistRunItemEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
abstract class DidIDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun actionDao(): ActionDao
    abstract fun completionDao(): CompletionDao
    abstract fun recurringItemDao(): RecurringItemDao
    abstract fun checklistRunDao(): ChecklistRunDao

    companion object {
        /**
         * Additive: optional photo when finishing a whole routine.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL(
                        "ALTER TABLE routines ADD COLUMN askPhotoOnComplete INTEGER NOT NULL DEFAULT 0"
                    )
                } catch (_: Exception) { }
            }
        }

        /**
         * Additive: daily checklist runs for home widget + photoRequired on items.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL(
                        "ALTER TABLE routine_items ADD COLUMN photoRequired INTEGER NOT NULL DEFAULT 0"
                    )
                } catch (_: Exception) { }
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS checklist_runs (
                        id TEXT NOT NULL PRIMARY KEY,
                        routineId TEXT NOT NULL,
                        epochDay INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_checklist_runs_routineId_epochDay ON checklist_runs(routineId, epochDay)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_checklist_runs_routineId ON checklist_runs(routineId)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS checklist_run_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        runId TEXT NOT NULL,
                        routineItemId TEXT NOT NULL,
                        label TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        photoRequired INTEGER NOT NULL,
                        checkedAt INTEGER,
                        FOREIGN KEY(runId) REFERENCES checklist_runs(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_checklist_run_items_runId ON checklist_run_items(runId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_checklist_run_items_routineItemId ON checklist_run_items(routineItemId)"
                )
            }
        }

        /**
         * Additive: optional photo proof path on completions (app-private file).
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE completions ADD COLUMN photoPath TEXT")
                } catch (_: Exception) { }
            }
        }

        /**
         * Additive: reminder repeat pattern + selected weekdays on routines.
         * Existing reminder times become DAILY so they keep firing.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE routines ADD COLUMN reminderRepeat TEXT NOT NULL DEFAULT 'OFF'")
                } catch (_: Exception) { }
                try {
                    db.execSQL("ALTER TABLE routines ADD COLUMN reminderDays TEXT")
                } catch (_: Exception) { }
                db.execSQL(
                    """
                    UPDATE routines
                    SET reminderRepeat = 'DAILY'
                    WHERE id IN (SELECT DISTINCT routineId FROM routine_reminder_times)
                    """.trimIndent()
                )
            }
        }

        /**
         * Additive: optional per-routine reminder clock times.
         * Empty table = no push; user can still open the app and check.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS routine_reminder_times (
                        id TEXT NOT NULL PRIMARY KEY,
                        routineId TEXT NOT NULL,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        FOREIGN KEY(routineId) REFERENCES routines(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_routine_reminder_times_routineId ON routine_reminder_times(routineId)"
                )
            }
        }

        /**
         * Additive migration: preserves all existing user data.
         * Never drops routines, actions, or completions.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recurring_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL,
                        frequency TEXT NOT NULL,
                        intervalCount INTEGER NOT NULL,
                        customUnit TEXT NOT NULL,
                        weekDays TEXT,
                        dayOfMonth INTEGER,
                        startDateEpochDay INTEGER NOT NULL,
                        nextDueEpochDay INTEGER NOT NULL,
                        reminderHour INTEGER,
                        reminderMinute INTEGER,
                        remindDaysBefore TEXT,
                        notes TEXT,
                        lastCompletedEpochDay INTEGER,
                        createdAt INTEGER NOT NULL,
                        archived INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Brings a v1 database up to v2 columns without destroying rows.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // quick_actions: pinned + pinOrder
                try {
                    db.execSQL("ALTER TABLE quick_actions ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) { /* already present */ }
                try {
                    db.execSQL("ALTER TABLE quick_actions ADD COLUMN pinOrder INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) { }
                // completions: detailItems
                try {
                    db.execSQL("ALTER TABLE completions ADD COLUMN detailItems TEXT")
                } catch (_: Exception) { }
            }
        }

        fun create(context: Context): DidIDatabase {
            return Room.databaseBuilder(context, DidIDatabase::class.java, "didi.db")
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                )
                // Do NOT use destructive migration — user schedules must survive updates.
                .build()
        }
    }
}
