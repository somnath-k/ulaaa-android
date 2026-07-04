package com.dotkios.ulaaa.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.dotkios.ulaaa.data.local.dao.TripDao
import com.dotkios.ulaaa.data.local.entity.TripEntity
import com.dotkios.ulaaa.data.model.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface TripRepository {
    val trips: Flow<List<Trip>>
    fun trip(id: String): Flow<Trip?>
    suspend fun addTrip(
        title: String,
        destination: String,
        startMillis: Long,
        endMillis: Long,
        squadSize: Int,
    )
    suspend fun deleteTrip(id: String)
}

@Singleton
class TripRepositoryImpl @Inject constructor(
    private val dao: TripDao,
) : TripRepository {

    override val trips: Flow<List<Trip>> =
        dao.observeAll().map { list -> list.map { it.toTrip() } }

    override fun trip(id: String): Flow<Trip?> =
        dao.observeAll().map { list -> list.firstOrNull { it.id == id }?.toTrip() }

    override suspend fun addTrip(
        title: String,
        destination: String,
        startMillis: Long,
        endMillis: Long,
        squadSize: Int,
    ) {
        dao.upsert(
            TripEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                destination = destination.trim(),
                startMillis = startMillis,
                endMillis = endMillis,
                squadSize = squadSize.coerceAtLeast(1),
                colorArgb = PALETTE.random().toArgb().toLong(),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun deleteTrip(id: String) = dao.delete(id)

    private fun TripEntity.toTrip(): Trip = Trip(
        id = id,
        title = title,
        destination = destination,
        dateRange = formatRange(startMillis, endMillis),
        squadSize = squadSize,
        accent = Color(colorArgb.toInt()),
    )

    private fun formatRange(startMillis: Long, endMillis: Long): String {
        if (startMillis <= 0L) return "Dates TBD"
        val start = DAY_FMT.format(Date(startMillis))
        val end = DAY_FMT.format(Date(endMillis.coerceAtLeast(startMillis)))
        return if (start == end) start else "$start – $end"
    }

    private companion object {
        val DAY_FMT = SimpleDateFormat("MMM d", Locale.getDefault())
        val PALETTE = listOf(
            Color(0xFF0E7C7B),
            Color(0xFFFF6B57),
            Color(0xFFF4A259),
            Color(0xFF5A6FEA),
        )
    }
}
