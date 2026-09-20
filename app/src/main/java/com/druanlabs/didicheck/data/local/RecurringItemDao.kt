package com.druanlabs.didicheck.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringItemDao {
    @Query("SELECT * FROM recurring_items WHERE archived = 0 ORDER BY nextDueEpochDay ASC, title ASC")
    fun observeActive(): Flow<List<RecurringItemEntity>>

    @Query("SELECT * FROM recurring_items WHERE archived = 0 ORDER BY nextDueEpochDay ASC, title ASC")
    suspend fun getActive(): List<RecurringItemEntity>

    @Query("SELECT * FROM recurring_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecurringItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecurringItemEntity)

    @Update
    suspend fun update(entity: RecurringItemEntity)

    @Query("DELETE FROM recurring_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE recurring_items SET archived = 1 WHERE id = :id")
    suspend fun archive(id: String)

    @Query("SELECT COUNT(*) FROM recurring_items")
    suspend fun count(): Int
}
