package com.dotkios.ulaaa.data.repository

import android.annotation.SuppressLint
import android.content.Context
import com.dotkios.ulaaa.data.model.GeoPoint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
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
}
