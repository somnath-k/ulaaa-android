package com.dotkios.ulaaa.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dotkios.ulaaa.data.local.entity.ChecklistItemEntity
import com.dotkios.ulaaa.data.local.entity.ExpenseEntity
import com.dotkios.ulaaa.data.local.entity.TripMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_items WHERE tripId = :tripId ORDER BY createdAt ASC")
    fun observeByTrip(tripId: String): Flow<List<ChecklistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ChecklistItemEntity)

    @Query("UPDATE checklist_items SET isDone = :done WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM trip_members WHERE tripId = :tripId ORDER BY name ASC")
    fun observeByTrip(tripId: String): Flow<List<TripMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: TripMemberEntity)

    @Query("DELETE FROM trip_members WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY createdAt DESC")
    fun observeByTrip(tripId: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: String)
}
