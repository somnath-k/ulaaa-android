package com.dotkios.ulaaa.data.remote

import com.dotkios.ulaaa.data.remote.dto.PexelsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PexelsApi {

    @GET("v1/search")
    suspend fun search(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 1,
        @Query("orientation") orientation: String = "landscape",
    ): PexelsResponse

    companion object {
        const val BASE_URL = "https://api.pexels.com/"
    }
}
