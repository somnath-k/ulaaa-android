package com.dotkios.ulaaa.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dotkios.ulaaa.data.local.dao.BucketDao
import com.dotkios.ulaaa.data.local.entity.BucketItemEntity

// Only the bucket list is local now — trips and their content live in Firestore.
@Database(
    entities = [BucketItemEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class UlaaaDatabase : RoomDatabase() {
    abstract fun bucketDao(): BucketDao
}
