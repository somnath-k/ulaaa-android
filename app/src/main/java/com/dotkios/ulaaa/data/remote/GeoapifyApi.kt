package com.dotkios.ulaaa.data.remote

import com.dotkios.ulaaa.data.remote.dto.GeoapifyResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GeoapifyApi {

    /**
     * Places API — POIs near a point.
     * @param filter e.g. "circle:<lon>,<lat>,<radiusMeters>"
     * @param bias   e.g. "proximity:<lon>,<lat>"
     */
    @GET("v2/places")
    suspend fun nearbyPlaces(
        @Query("categories") categories: String,
        @Query("filter") filter: String,
        @Query("bias") bias: String,
        @Query("limit") limit: Int,
        @Query("apiKey") apiKey: String,
    ): GeoapifyResponse

    companion object {
        const val BASE_URL = "https://api.geoapify.com/"
    }
}
