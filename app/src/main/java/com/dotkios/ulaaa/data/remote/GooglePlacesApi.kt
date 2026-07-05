package com.dotkios.ulaaa.data.remote

import com.dotkios.ulaaa.data.remote.dto.GooglePlacesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GooglePlacesApi {

    @GET("maps/api/place/textsearch/json")
    suspend fun textSearch(
        @Query("query") query: String,
        @Query("key") apiKey: String,
    ): GooglePlacesResponse

    companion object {
        const val BASE_URL = "https://maps.googleapis.com/"

        fun photoUrl(photoReference: String, apiKey: String): String =
            "${BASE_URL}maps/api/place/photo?maxwidth=800&photo_reference=$photoReference&key=$apiKey"
    }
}
