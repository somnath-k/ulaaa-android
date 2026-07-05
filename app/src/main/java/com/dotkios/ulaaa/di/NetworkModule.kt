package com.dotkios.ulaaa.di

import com.dotkios.ulaaa.BuildConfig
import com.dotkios.ulaaa.data.remote.GeminiApi
import com.dotkios.ulaaa.data.remote.GeoapifyApi
import com.dotkios.ulaaa.data.remote.PexelsApi
import com.dotkios.ulaaa.data.remote.WikipediaApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideGeoapifyApi(client: OkHttpClient, json: Json): GeoapifyApi =
        retrofit(GeoapifyApi.BASE_URL, client, json).create(GeoapifyApi::class.java)

    @Provides
    @Singleton
    fun provideGeminiApi(client: OkHttpClient, json: Json): GeminiApi =
        retrofit(GeminiApi.BASE_URL, client, json).create(GeminiApi::class.java)

    @Provides
    @Singleton
    fun provideWikipediaApi(client: OkHttpClient, json: Json): WikipediaApi =
        retrofit(WikipediaApi.BASE_URL, client, json).create(WikipediaApi::class.java)

    @Provides
    @Singleton
    fun providePexelsApi(client: OkHttpClient, json: Json): PexelsApi =
        retrofit(PexelsApi.BASE_URL, client, json).create(PexelsApi::class.java)

    private fun retrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
}
