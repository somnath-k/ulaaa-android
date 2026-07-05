package com.dotkios.ulaaa

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.module.http.HttpRequestUtil

@HiltAndroidApp
class UlaaaApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        initMapLibre()
    }

    /**
     * MapLibre renders Ola Maps vector tiles. Every request to api.olamaps.io must carry
     * the api_key as a query param, so we route MapLibre's HTTP through an interceptor.
     */
    private fun initMapLibre() {
        MapLibre.getInstance(this)

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                if (request.url.host == OLA_HOST) {
                    val keyed = request.url.newBuilder()
                        .addQueryParameter("api_key", BuildConfig.OLA_MAPS_API_KEY)
                        .build()
                    chain.proceed(request.newBuilder().url(keyed).build())
                } else {
                    chain.proceed(request)
                }
            }
            .build()
        HttpRequestUtil.setOkHttpClient(client)
    }

    /**
     * Coil uses its own HTTP stack — Wikimedia (upload.wikimedia.org) 403s a blank/default
     * User-Agent, so images silently failed. Give Coil a client that sends a real User-Agent.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient {
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "Ulaaa/1.0 (https://github.com/somnath-k/ulaaa-android)")
                        .build()
                    chain.proceed(request)
                }
                .build()
        }
        .crossfade(true)
        .build()

    private companion object {
        const val OLA_HOST = "api.olamaps.io"
    }
}
