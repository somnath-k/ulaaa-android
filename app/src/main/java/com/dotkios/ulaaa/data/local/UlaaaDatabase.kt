package com.dotkios.ulaaa.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dotkios.ulaaa.data.local.dao.BucketDao
import com.dotkios.ulaaa.data.local.dao.ChecklistDao
import com.dotkios.ulaaa.data.local.dao.ExpenseDao
import com.dotkios.ulaaa.data.local.dao.MemberDao
import com.dotkios.ulaaa.data.local.dao.TripDao
import com.dotkios.ulaaa.data.local.entity.BucketItemEntity
import com.dotkios.ulaaa.data.local.entity.ChecklistItemEntity
import com.dotkios.ulaaa.data.local.entity.ExpenseEntity
import com.dotkios.ulaaa.data.local.entity.TripEntity
import com.dotkios.ulaaa.data.local.entity.TripMemberEntity

@Database(
    entities = [
        TripEntity::class,
        BucketItemEntity::class,
        ChecklistItemEntity::class,
        TripMemberEntity::class,
        ExpenseEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class UlaaaDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun bucketDao(): BucketDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun memberDao(): MemberDao
    abstract fun expenseDao(): ExpenseDao
}
