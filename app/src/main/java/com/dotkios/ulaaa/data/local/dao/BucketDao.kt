package com.dotkios.ulaaa.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dotkios.ulaaa.data.local.entity.BucketItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BucketDao {

    @Query("SELECT * FROM bucket_items ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<BucketItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BucketItemEntity)

    @Query("DELETE FROM bucket_items WHERE id = :id")
    suspend fun delete(id: String)
}
