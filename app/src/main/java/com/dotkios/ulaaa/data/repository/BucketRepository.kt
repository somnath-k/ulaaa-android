package com.dotkios.ulaaa.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.dotkios.ulaaa.data.local.dao.BucketDao
import com.dotkios.ulaaa.data.local.entity.BucketItemEntity
import com.dotkios.ulaaa.data.model.BucketItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface BucketRepository {
    val items: Flow<List<BucketItem>>
    suspend fun add(name: String, location: String, note: String)
    suspend fun remove(id: String)
}

@Singleton
class BucketRepositoryImpl @Inject constructor(
    private val dao: BucketDao,
) : BucketRepository {

    override val items: Flow<List<BucketItem>> =
        dao.observeAll().map { list -> list.map { it.toBucketItem() } }

    override suspend fun add(name: String, location: String, note: String) {
        dao.upsert(
            BucketItemEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                location = location.trim(),
                note = note.trim(),
                colorArgb = PALETTE.random().toArgb().toLong(),
                savedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun remove(id: String) = dao.delete(id)

    private fun BucketItemEntity.toBucketItem(): BucketItem = BucketItem(
        id = id,
        name = name,
        location = location,
        note = note,
        accent = Color(colorArgb.toInt()),
    )

    private companion object {
        val PALETTE = listOf(
            Color(0xFF0E7C7B),
            Color(0xFFFF6B57),
            Color(0xFFF4A259),
            Color(0xFF5A6FEA),
        )
    }
}
