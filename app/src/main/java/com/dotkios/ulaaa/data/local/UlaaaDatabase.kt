package com.dotkios.ulaaa.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dotkios.ulaaa.data.local.dao.BucketDao
import com.dotkios.ulaaa.data.local.dao.TripDao
import com.dotkios.ulaaa.data.local.entity.BucketItemEntity
import com.dotkios.ulaaa.data.local.entity.TripEntity

@Database(
    entities = [TripEntity::class, BucketItemEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class UlaaaDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun bucketDao(): BucketDao
}
