package com.dotkios.ulaaa.data.repository

import com.dotkios.ulaaa.data.local.dao.ChecklistDao
import com.dotkios.ulaaa.data.local.dao.ExpenseDao
import com.dotkios.ulaaa.data.local.dao.MemberDao
import com.dotkios.ulaaa.data.local.entity.ChecklistItemEntity
import com.dotkios.ulaaa.data.local.entity.ExpenseEntity
import com.dotkios.ulaaa.data.local.entity.TripMemberEntity
import com.dotkios.ulaaa.data.model.ChecklistItem
import com.dotkios.ulaaa.data.model.Expense
import com.dotkios.ulaaa.data.model.TripMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface TripDetailRepository {
    fun checklist(tripId: String): Flow<List<ChecklistItem>>
    fun members(tripId: String): Flow<List<TripMember>>
    fun expenses(tripId: String): Flow<List<Expense>>

    suspend fun addChecklistItem(tripId: String, text: String)
    suspend fun setChecklistDone(itemId: String, done: Boolean)
    suspend fun deleteChecklistItem(itemId: String)

    suspend fun addMember(tripId: String, uid: String, name: String)
    suspend fun deleteMember(memberId: String)

    suspend fun addExpense(tripId: String, title: String, amount: Double, paidBy: String)
    suspend fun deleteExpense(expenseId: String)
}

@Singleton
class TripDetailRepositoryImpl @Inject constructor(
    private val checklistDao: ChecklistDao,
    private val memberDao: MemberDao,
    private val expenseDao: ExpenseDao,
) : TripDetailRepository {

    override fun checklist(tripId: String): Flow<List<ChecklistItem>> =
        checklistDao.observeByTrip(tripId).map { list ->
            list.map { ChecklistItem(it.id, it.text, it.isDone) }
        }

    override fun members(tripId: String): Flow<List<TripMember>> =
        memberDao.observeByTrip(tripId).map { list ->
            list.map { TripMember(it.id, it.uid, it.name) }
        }

    override fun expenses(tripId: String): Flow<List<Expense>> =
        expenseDao.observeByTrip(tripId).map { list ->
            list.map { Expense(it.id, it.title, it.amount, it.paidBy) }
        }

    override suspend fun addChecklistItem(tripId: String, text: String) {
        if (text.isBlank()) return
        checklistDao.upsert(
            ChecklistItemEntity(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                text = text.trim(),
                isDone = false,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun setChecklistDone(itemId: String, done: Boolean) =
        checklistDao.setDone(itemId, done)

    override suspend fun deleteChecklistItem(itemId: String) = checklistDao.delete(itemId)

    override suspend fun addMember(tripId: String, uid: String, name: String) {
        if (name.isBlank()) return
        memberDao.upsert(
            TripMemberEntity(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                uid = uid,
                name = name.trim(),
            ),
        )
    }

    override suspend fun deleteMember(memberId: String) = memberDao.delete(memberId)

    override suspend fun addExpense(tripId: String, title: String, amount: Double, paidBy: String) {
        if (title.isBlank() || amount <= 0.0) return
        expenseDao.upsert(
            ExpenseEntity(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                title = title.trim(),
                amount = amount,
                paidBy = paidBy,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun deleteExpense(expenseId: String) = expenseDao.delete(expenseId)
}
