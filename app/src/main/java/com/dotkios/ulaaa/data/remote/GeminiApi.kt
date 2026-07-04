package com.dotkios.ulaaa.data.remote

import com.dotkios.ulaaa.data.remote.dto.GeminiRequest
import com.dotkios.ulaaa.data.remote.dto.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiApi {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generate(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body body: GeminiRequest,
    ): GeminiResponse

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/"
        const val MODEL = "gemini-2.0-flash"
    }
}
