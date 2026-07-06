package com.dotkios.ulaaa.data.repository

import androidx.compose.ui.graphics.Color
import com.dotkios.ulaaa.BuildConfig
import com.dotkios.ulaaa.data.model.Landmark
import com.dotkios.ulaaa.data.remote.GeoapifyApi
import com.dotkios.ulaaa.data.remote.dto.GeoapifyProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

interface PlacesRepository {
    suspend fun nearbyLandmarks(lat: Double, lon: Double, radiusMeters: Int = 20_000): Result<List<Landmark>>
}

@Singleton
class PlacesRepositoryImpl @Inject constructor(
    private val api: GeoapifyApi,
    private val placeImageRepository: PlaceImageRepository,
) : PlacesRepository {

    override suspend fun nearbyLandmarks(
        lat: Double,
        lon: Double,
        radiusMeters: Int,
    ): Result<List<Landmark>> = runCatching {
        // Prefer close places (~20 km); widen to 40/60 km only when too few nearby.
        val radii = listOf(radiusMeters.coerceAtMost(20_000), 40_000, 60_000).distinct()
        val features = radii.firstNotNullOfOrNull { radius ->
            fetchFeatures(lat, lon, radius).takeIf { it.size >= MIN_PLACES }
        } ?: fetchFeatures(lat, lon, radii.last())

        val base = diversify(features.filter { !it.name.isNullOrBlank() })
            .mapIndexed { index, props ->
                Landmark(
                    id = props.placeId ?: "geoapify-$index",
                    name = props.name!!,
                    category = props.categories.firstOrNull()?.prettyCategory() ?: "Place",
                    distanceKm = ((props.distance ?: 0) / 100).toDouble() / 10.0,
                    rating = 0.0, // Geoapify has no rating; card hides it when 0.
                    accent = PALETTE[index % PALETTE.size],
                    lat = props.lat,
                    lon = props.lon,
                )
            }

        // Enrich each place with a real image + description (Wikipedia → Pexels), in parallel.
        coroutineScope {
            base.map { landmark ->
                async {
                    val image = placeImageRepository.resolve(landmark.name, landmark.category)
                    landmark.copy(imageUrl = image.url, description = image.description)
                }
            }.awaitAll()
        }
    }

    private suspend fun fetchFeatures(lat: Double, lon: Double, radiusMeters: Int) =
        api.nearbyPlaces(
            categories = CATEGORIES,
            filter = "circle:$lon,$lat,$radiusMeters",
            bias = "proximity:$lon,$lat",
            limit = 40,
            apiKey = BuildConfig.GEOAPIFY_API_KEY,
        ).features.mapNotNull { it.properties }

    /**
     * Caps places of worship so temples don't crowd out other place types, then
     * takes the nearest [MAX_RESULTS]. If everything nearby is a temple, keeps them.
     */
    private fun diversify(features: List<GeoapifyProperties>): List<GeoapifyProperties> {
        val (worship, others) = features.partition { p ->
            p.categories.any { it.startsWith("religion") }
        }
        val mixed = if (others.isEmpty()) worship else others + worship.take(MAX_WORSHIP)
        return mixed.sortedBy { it.distance ?: Int.MAX_VALUE }.take(MAX_RESULTS)
    }

    private fun String.prettyCategory(): String =
        substringAfterLast('.').replace('_', ' ').replaceFirstChar { it.uppercase() }

    private companion object {
        // Below this many places within a radius, widen the search.
        const val MIN_PLACES = 5

        // How many places to show, and the cap on temples so they don't dominate.
        const val MAX_RESULTS = 20
        const val MAX_WORSHIP = 5

        // Only leisure and tourism spots on the map.
        const val CATEGORIES = "tourism,leisure"
        val PALETTE = listOf(
            Color(0xFF0E7C7B),
            Color(0xFFFF6B57),
            Color(0xFFF4A259),
            Color(0xFF5A6FEA),
        )
    }
}
