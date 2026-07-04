package com.dotkios.ulaaa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bucket_items")
data class BucketItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val location: String,
    val note: String,
    val colorArgb: Long,
    val savedAt: Long,
)
