package com.dotkios.ulaaa.data.repository

import com.dotkios.ulaaa.data.local.dao.TripDao
import com.dotkios.ulaaa.data.local.entity.TripEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeTripDao : TripDao {
    private val state = MutableStateFlow<List<TripEntity>>(emptyList())
    override fun observeAll(): Flow<List<TripEntity>> = state
    override suspend fun upsert(trip: TripEntity) =
        state.update { current -> current.filterNot { it.id == trip.id } + trip }
    override suspend fun delete(id: String) =
        state.update { current -> current.filterNot { it.id == id } }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TripRepositoryImplTest {

    @Test
    fun `addTrip persists and maps to a domain Trip`() = runTest {
        val repo = TripRepositoryImpl(FakeTripDao())

        repo.addTrip(
            title = "Goa Getaway",
            destination = "Goa",
            startMillis = 1_700_000_000_000L,
            endMillis = 1_700_500_000_000L,
            squadSize = 4,
        )

        val trips = repo.trips.first()
        assertEquals(1, trips.size)
        assertEquals("Goa Getaway", trips[0].title)
        assertEquals("Goa", trips[0].destination)
        assertEquals(4, trips[0].squadSize)
        assertNotEquals("Dates TBD", trips[0].dateRange)
    }

    @Test
    fun `missing dates fall back to a TBD label`() = runTest {
        val repo = TripRepositoryImpl(FakeTripDao())

        repo.addTrip("Someday Trip", "Anywhere", startMillis = 0L, endMillis = 0L, squadSize = 1)

        assertEquals("Dates TBD", repo.trips.first().first().dateRange)
    }

    @Test
    fun `squad size is coerced to at least one`() = runTest {
        val repo = TripRepositoryImpl(FakeTripDao())

        repo.addTrip("Solo Trip", "Ladakh", startMillis = 1L, endMillis = 1L, squadSize = 0)

        assertTrue(repo.trips.first().first().squadSize >= 1)
    }
}
