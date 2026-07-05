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

    /** Caller must hold a location permission before invoking. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Result<GeoPoint?> = runCatching {
        val location = fused.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            null,
        ).await()
        location?.let { GeoPoint(it.latitude, it.longitude) }
    }

    /** Reverse-geocodes the current location to a city/area name (null if unavailable). */
    suspend fun currentCity(): String? {
        val point = currentLocation().getOrNull() ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.getDefault())
                    .getFromLocation(point.lat, point.lon, 1)
                    ?.firstOrNull()
                    ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
            }.getOrNull()
        }
    }
}
