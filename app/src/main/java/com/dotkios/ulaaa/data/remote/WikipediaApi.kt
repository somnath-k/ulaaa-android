package com.dotkios.ulaaa.data.remote

import com.dotkios.ulaaa.data.remote.dto.WikiSummary
import retrofit2.http.GET
import retrofit2.http.Path

interface WikipediaApi {

    /** Keyless page summary: title, extract (description), and a thumbnail image. */
    @GET("api/rest_v1/page/summary/{title}")
    suspend fun summary(@Path("title") title: String): WikiSummary

    companion object {
        const val BASE_URL = "https://en.wikipedia.org/"
    }
}
