package com.dotkios.ulaaa.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "checklist_items",
    indices = [Index("tripId")],
)
data class ChecklistItemEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val text: String,
    val isDone: Boolean,
    val createdAt: Long,
)

@Entity(
    tableName = "trip_members",
    indices = [Index("tripId")],
)
data class TripMemberEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val uid: String,
    val name: String,
)

@Entity(
    tableName = "expenses",
    indices = [Index("tripId")],
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val title: String,
    val amount: Double,
    val paidBy: String,
    val createdAt: Long,
)
