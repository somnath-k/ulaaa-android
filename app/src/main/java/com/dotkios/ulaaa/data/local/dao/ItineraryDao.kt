package com.dotkios.ulaaa.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dotkios.ulaaa.data.local.entity.ItineraryStopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItineraryDao {
    @Query("SELECT * FROM itinerary_stops WHERE tripId = :tripId ORDER BY day ASC, orderIndex ASC")
    fun observeByTrip(tripId: String): Flow<List<ItineraryStopEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stops: List<ItineraryStopEntity>)

    @Query("DELETE FROM itinerary_stops WHERE tripId = :tripId")
    suspend fun deleteByTrip(tripId: String)
}
