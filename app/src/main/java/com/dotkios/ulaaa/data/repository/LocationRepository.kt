package com.dotkios.ulaaa.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.dotkios.ulaaa.data.model.GeoPoint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val fused = LocationServices.getFusedLocationProviderClient(context)

    /** Caller must hold a location permission before invoking. Falls back to a default if unknown. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Result<GeoPoint?> = runCatching {
        // Cached fix is instant; only wait for a fresh one if there's no cache.
        val cached = fused.lastLocation.await()
        val location = cached ?: runCatching {
            fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
        }.getOrNull()
        location?.let { GeoPoint(it.latitude, it.longitude) } ?: DEFAULT_LOCATION
    }

    private companion object {
        // Used when the device has no location (e.g. emulator without a set position).
        val DEFAULT_LOCATION = GeoPoint(13.0827, 80.2707) // Chennai
    }

    /** Reverse-geocodes the current location to a city/area name (null if unavailable). */
    suspend fun currentCity(): String? {
        val point = currentLocation().getOrNull() ?: return null
        return reverseCity(point)
    }

    /**
     * A grounded location label for AI prompts: city name plus exact coordinates.
     * Coordinates are always included so recommendations stay anchored even when the
     * Geocoder returns no city (common on emulators). Never null.
     */
    suspend fun currentPlaceLabel(): String {
        val point = currentLocation().getOrNull() ?: return "India"
        val city = reverseCity(point)
        val coords = "latitude %.4f, longitude %.4f".format(point.lat, point.lon)
        return if (city != null) "$city ($coords)" else coords
    }

    private suspend fun reverseCity(point: GeoPoint): String? = withContext(Dispatchers.IO) {
        runCatching {
            @Suppress("DEPRECATION")
            Geocoder(context, Locale.getDefault())
                .getFromLocation(point.lat, point.lon, 1)
                ?.firstOrNull()
                ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
        }.getOrNull()
    }
}
