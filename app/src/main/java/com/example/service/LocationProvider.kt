package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.example.model.LocationCoordinate
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class LocationProvider(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(): LocationCoordinate? {
        // Try high accuracy via Google Play Services first with a 10-second timeout
        val fusedResult = withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine { continuation ->
                val cts = CancellationTokenSource()
                try {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cts.token
                    ).addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            continuation.resume(
                                LocationCoordinate(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    accuracyMeters = location.accuracy,
                                    timestamp = location.time,
                                    provider = location.provider ?: "FusedLocation"
                                )
                            )
                        } else {
                            // Try last known location
                            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                                if (lastLoc != null) {
                                    continuation.resume(
                                        LocationCoordinate(
                                            latitude = lastLoc.latitude,
                                            longitude = lastLoc.longitude,
                                            accuracyMeters = lastLoc.accuracy,
                                            timestamp = lastLoc.time,
                                            provider = lastLoc.provider ?: "LastKnown"
                                        )
                                    )
                                } else {
                                    continuation.resume(null)
                                }
                            }.addOnFailureListener {
                                continuation.resume(null)
                            }
                        }
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                } catch (e: SecurityException) {
                    continuation.resume(null)
                } catch (e: Exception) {
                    continuation.resume(null)
                }

                continuation.invokeOnCancellation {
                    cts.cancel()
                }
            }
        }

        if (fusedResult != null) return fusedResult

        // Fallback to system LocationManager
        return getSystemLocationFallback()
    }

    @SuppressLint("MissingPermission")
    private fun getSystemLocationFallback(): LocationCoordinate? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return null

            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null

            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            }

            bestLocation?.let {
                LocationCoordinate(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracyMeters = it.accuracy,
                    timestamp = it.time,
                    provider = it.provider ?: "SystemFallback"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
