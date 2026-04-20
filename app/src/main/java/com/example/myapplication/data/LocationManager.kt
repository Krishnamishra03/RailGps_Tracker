package com.example.myapplication.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        1000L
    ).apply {
        setMinUpdateIntervalMillis(500L)
        setWaitForAccurateLocation(false) // Don't wait, show whatever is available
    }.build()

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<LocationData> = callbackFlow {
        // Try to get last known location immediately
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                Log.d("LocationManager", "Last known location found: ${it.latitude}")
                trySend(LocationData(it.latitude, it.longitude, it.accuracy, it.time))
            }
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Log.d("LocationManager", "New location update: ${location.latitude}")
                    trySend(
                        LocationData(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            timestamp = location.time
                        )
                    )
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper() // Use MainLooper for better stability on some devices
            ).addOnFailureListener { e ->
                Log.e("LocationManager", "GPS Request Failed", e)
                close(e)
            }
        } catch (e: Exception) {
            Log.e("LocationManager", "Fatal location error", e)
            close(e)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
