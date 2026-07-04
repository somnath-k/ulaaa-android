package com.dotkios.ulaaa.data.repository

import androidx.compose.ui.graphics.Color
import com.dotkios.ulaaa.BuildConfig
import com.dotkios.ulaaa.data.model.Landmark
import com.dotkios.ulaaa.data.remote.GeoapifyApi
import javax.inject.Inject
import javax.inject.Singleton

interface PlacesRepository {
    suspend fun nearbyLandmarks(lat: Double, lon: Double, radiusMeters: Int = 5000): Result<List<Landmark>>
}

@Singleton
class PlacesRepositoryImpl @Inject constructor(
    private val api: GeoapifyApi,
) : PlacesRepository {

    override suspend fun nearbyLandmarks(
        lat: Double,
        lon: Double,
        radiusMeters: Int,
    ): Result<List<Landmark>> = runCatching {
        val response = api.nearbyPlaces(
            categories = CATEGORIES,
            filter = "circle:$lon,$lat,$radiusMeters",
            bias = "proximity:$lon,$lat",
            limit = 20,
            apiKey = BuildConfig.GEOAPIFY_API_KEY,
        )
        response.features
            .mapNotNull { it.properties }
            .filter { !it.name.isNullOrBlank() }
            .mapIndexed { index, props ->
                Landmark(
                    id = props.placeId ?: "geoapify-$index",
                    name = props.name!!,
                    category = props.categories.firstOrNull()?.prettyCategory() ?: "Place",
                    distanceKm = ((props.distance ?: 0) / 100).toDouble() / 10.0,
                    rating = 0.0, // Geoapify has no rating; card hides it when 0.
                    accent = PALETTE[index % PALETTE.size],
                )
            }
    }

    private fun String.prettyCategory(): String =
        substringAfterLast('.').replace('_', ' ').replaceFirstChar { it.uppercase() }

    private companion object {
        const val CATEGORIES =
            "tourism.sights,tourism.attraction,entertainment.museum,leisure.park,natural"
        val PALETTE = listOf(
            Color(0xFF0E7C7B),
            Color(0xFFFF6B57),
            Color(0xFFF4A259),
            Color(0xFF5A6FEA),
        )
    }
}
