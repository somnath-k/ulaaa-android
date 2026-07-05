package com.dotkios.ulaaa.data.repository

import com.dotkios.ulaaa.BuildConfig
import com.dotkios.ulaaa.data.remote.GooglePlacesApi
import com.dotkios.ulaaa.data.remote.PexelsApi
import com.dotkios.ulaaa.data.remote.WikipediaApi
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** A resolved place image (and, when available, a short description / suggestion). */
data class PlaceImage(
    val url: String?,
    val description: String?,
)

interface PlaceImageRepository {
    /** Best image + description for [query], trying Wikipedia then Pexels; nulls if none. */
    suspend fun resolve(query: String, fallbackKeyword: String): PlaceImage
}

@Singleton
class PlaceImageRepositoryImpl @Inject constructor(
    private val wikipediaApi: WikipediaApi,
    private val pexelsApi: PexelsApi,
    private val googlePlacesApi: GooglePlacesApi,
) : PlaceImageRepository {

    private val cache = ConcurrentHashMap<String, PlaceImage>()

    override suspend fun resolve(query: String, fallbackKeyword: String): PlaceImage {
        val key = query.trim().ifBlank { fallbackKeyword.trim() }
        cache[key]?.let { return it }

        // Google Places (real place photos) first when configured, else Wikipedia, else Pexels.
        val resolved = fromGooglePlaces(key)
            ?: fromWikipedia(key)
            ?: fromPexels(key.ifBlank { fallbackKeyword })
            ?: PlaceImage(url = null, description = null)

        cache[key] = resolved
        return resolved
    }

    private suspend fun fromGooglePlaces(query: String): PlaceImage? = runCatching {
        val apiKey = BuildConfig.GOOGLE_PLACES_API_KEY
        if (apiKey.isBlank() || query.isBlank()) return null
        val response = googlePlacesApi.textSearch(query = query, apiKey = apiKey)
        val ref = response.results.firstOrNull()?.photos?.firstOrNull()?.photoReference ?: return null
        PlaceImage(url = GooglePlacesApi.photoUrl(ref, apiKey), description = null)
    }.getOrNull()

    private suspend fun fromWikipedia(query: String): PlaceImage? = runCatching {
        if (query.isBlank()) return null
        val summary = wikipediaApi.summary(query)
        val url = summary.originalimage?.source ?: summary.thumbnail?.source
        if (url != null) PlaceImage(url = url, description = summary.extract) else null
    }.getOrNull()

    private suspend fun fromPexels(query: String): PlaceImage? = runCatching {
        val apiKey = BuildConfig.PEXELS_API_KEY
        if (apiKey.isBlank() || query.isBlank()) return null
        val response = pexelsApi.search(apiKey = apiKey, query = query)
        val url = response.photos.firstOrNull()?.src?.let { it.large ?: it.medium }
        url?.let { PlaceImage(url = it, description = null) }
    }.getOrNull()
}
