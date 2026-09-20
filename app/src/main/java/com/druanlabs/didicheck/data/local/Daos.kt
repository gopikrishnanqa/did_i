package com.druanlabs.didicheck.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY sortOrder ASC")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines ORDER BY sortOrder ASC")
    suspend fun getRoutines(): List<RoutineEntity>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutine(id: String): RoutineEntity?

    @Query("SELECT * FROM routine_items ORDER BY sortOrder ASC")
    fun observeItems(): Flow<List<RoutineItemEntity>>

    @Query("SELECT * FROM routine_items WHERE routineId = :routineId ORDER BY sortOrder ASC")
    suspend fun getItems(routineId: String): List<RoutineItemEntity>

    @Query("SELECT COUNT(*) FROM routines")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutine(entity: RoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<RoutineItemEntity>)

    @Query("DELETE FROM routine_items WHERE routineId = :routineId")
    suspend fun deleteItems(routineId: String)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: String)

    @Query("UPDATE routines SET lastCompletedAt = :timestamp WHERE id = :id")
    suspend fun markCompleted(id: String, timestamp: Long)

    @Query("SELECT * FROM routine_reminder_times ORDER BY sortOrder ASC, hour ASC, minute ASC")
    fun observeReminderTimes(): Flow<List<RoutineReminderTimeEntity>>

    @Query("SELECT * FROM routine_reminder_times ORDER BY sortOrder ASC, hour ASC, minute ASC")
    suspend fun getAllReminderTimes(): List<RoutineReminderTimeEntity>

    @Query("SELECT * FROM routine_reminder_times WHERE routineId = :routineId ORDER BY sortOrder ASC, hour ASC, minute ASC")
    suspend fun getReminderTimes(routineId: String): List<RoutineReminderTimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminderTimes(times: List<RoutineReminderTimeEntity>)

    @Query("DELETE FROM routine_reminder_times WHERE routineId = :routineId")
    suspend fun deleteReminderTimes(routineId: String)

    @Transaction
    suspend fun saveRoutine(
        routine: RoutineEntity,
        items: List<RoutineItemEntity>,
        reminderTimes: List<RoutineReminderTimeEntity> = emptyList(),
    ) {
        upsertRoutine(routine)
        deleteItems(routine.id)
        upsertItems(items)
        deleteReminderTimes(routine.id)
        if (reminderTimes.isNotEmpty()) {
            upsertReminderTimes(reminderTimes)
        }
    }
}

@Dao
interface ActionDao {
    @Query(
        """
        SELECT * FROM quick_actions
        ORDER BY pinned DESC, pinOrder ASC, useCount DESC, lastUsedAt DESC
        """
    )
    fun observeActions(): Flow<List<QuickActionEntity>>

    @Query(
        """
        SELECT * FROM quick_actions
        ORDER BY pinned DESC, pinOrder ASC, useCount DESC, lastUsedAt DESC
        """
    )
    suspend fun getActions(): List<QuickActionEntity>

    @Query("SELECT * FROM quick_actions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): QuickActionEntity?

    @Query("SELECT * FROM quick_actions WHERE LOWER(label) = LOWER(:label) LIMIT 1")
    suspend fun findByLabel(label: String): QuickActionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: QuickActionEntity)

    @Update
    suspend fun update(entity: QuickActionEntity)

    @Query("DELETE FROM quick_actions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM quick_actions")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(pinOrder), -1) FROM quick_actions WHERE pinned = 1")
    suspend fun maxPinOrder(): Int
}

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions WHERE id = :id")
    suspend fun get(id: String): CompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CompletionEntity)

    @Query("DELETE FROM completions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE completions SET timestamp = :timestamp WHERE id = :id")
    suspend fun updateTime(id: String, timestamp: Long)

    @Query("UPDATE completions SET photoPath = :photoPath WHERE id = :id")
    suspend fun updatePhotoPath(id: String, photoPath: String?)

    @Query(
        """
        SELECT * FROM completions
        WHERE LOWER(actionLabel) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(IFNULL(detailItems, '')) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(IFNULL(routineName, '')) LIKE '%' || LOWER(:query) || '%'
        ORDER BY timestamp DESC
        """
    )
    suspend fun search(query: String): List<CompletionEntity>

    @Query("SELECT * FROM completions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<CompletionEntity>
}

@Dao
interface ChecklistRunDao {
    @Query("SELECT * FROM checklist_runs WHERE routineId = :routineId AND epochDay = :epochDay LIMIT 1")
    suspend fun getRun(routineId: String, epochDay: Long): ChecklistRunEntity?

    @Query("SELECT * FROM checklist_runs WHERE id = :runId LIMIT 1")
    suspend fun getRunById(runId: String): ChecklistRunEntity?

    @Query("SELECT * FROM checklist_run_items WHERE runId = :runId ORDER BY sortOrder ASC")
    suspend fun getItems(runId: String): List<ChecklistRunItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRun(run: ChecklistRunEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<ChecklistRunItemEntity>)

    @Query("UPDATE checklist_run_items SET checkedAt = :checkedAt WHERE id = :itemId")
    suspend fun setCheckedAt(itemId: String, checkedAt: Long?)

    @Query("SELECT * FROM checklist_run_items WHERE id = :itemId LIMIT 1")
    suspend fun getItem(itemId: String): ChecklistRunItemEntity?

    @Transaction
    suspend fun insertRunWithItems(run: ChecklistRunEntity, items: List<ChecklistRunItemEntity>) {
        upsertRun(run)
        upsertItems(items)
    }
}
