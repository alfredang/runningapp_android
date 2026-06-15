package com.tertiaryinfotech.runtrackgps.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.tertiaryinfotech.runtrackgps.model.Coordinate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wraps FusedLocationProvider: requests fixes, filters noisy GPS, accumulates
 * distance, and publishes the route for the map + view model to observe.
 *
 * A foreground [LocationService] is started while tracking so location keeps
 * flowing when the screen is locked (Android's background-location requirement).
 * Mirrors the iOS LocationManager (CoreLocation) and its filtering rules.
 */
class LocationManager(private val context: Context) {

    // MARK: Published state
    private val _route = MutableStateFlow<List<Coordinate>>(emptyList())
    val route: StateFlow<List<Coordinate>> = _route.asStateFlow()

    private val _currentLocation = MutableStateFlow<Coordinate?>(null)
    val currentLocation: StateFlow<Coordinate?> = _currentLocation.asStateFlow()

    private val _totalDistanceMeters = MutableStateFlow(0.0)
    val totalDistanceMeters: StateFlow<Double> = _totalDistanceMeters.asStateFlow()

    private val _isAccuracyPoor = MutableStateFlow(false)
    val isAccuracyPoor: StateFlow<Boolean> = _isAccuracyPoor.asStateFlow()

    // MARK: Filtering thresholds (identical to iOS)
    private val maxAcceptableAccuracy = 20.0      // metres
    private val maxFixAgeMs = 5_000L              // 5 seconds
    private val maxRealisticSpeed = 12.0          // m/s (~43 km/h, faster than any runner)
    private val minMoveDistance = 2.0             // metres (ignore jitter while standing)

    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var lastAccepted: Location? = null
    private var isTracking = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { handleLocation(it) }
        }
    }

    // MARK: Permissions
    val isAuthorized: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** "Always"/background location, needed for full background tracking. */
    val hasBackgroundAuthorization: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        } else isAuthorized

    // MARK: Tracking lifecycle
    fun startTracking() {
        if (!isAuthorized) return
        isTracking = true
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMinUpdateDistanceMeters(0f)
            .build()
        try {
            client.requestLocationUpdates(request, callback, context.mainLooper)
        } catch (_: SecurityException) {
        }
        startForegroundService()
    }

    /** Stops feeding new fixes into the distance total without discarding the route. */
    fun pauseTracking() {
        isTracking = false
        lastAccepted = null   // avoid a huge jump segment across the pause gap
    }

    fun resumeTracking() {
        isTracking = true
    }

    fun stopTracking() {
        isTracking = false
        client.removeLocationUpdates(callback)
        stopForegroundService()
    }

    /** Clears all accumulated data for a fresh run. */
    fun reset() {
        _route.value = emptyList()
        _totalDistanceMeters.value = 0.0
        lastAccepted = null
        _isAccuracyPoor.value = false
    }

    // MARK: Fix handling (filtering rules mirror the iOS implementation)
    private fun handleLocation(newLocation: Location) {
        // Always surface the latest fix to the map, even if rejected for distance.
        _currentLocation.value = Coordinate(newLocation.latitude, newLocation.longitude)

        // 1) Reject poor / invalid accuracy.
        if (!newLocation.hasAccuracy() || newLocation.accuracy < 0f ||
            newLocation.accuracy > maxAcceptableAccuracy
        ) {
            _isAccuracyPoor.value = true
            return
        }
        _isAccuracyPoor.value = false

        // 2) Reject stale fixes.
        if (kotlin.math.abs(System.currentTimeMillis() - newLocation.time) > maxFixAgeMs) return

        // Only accumulate distance while actively tracking (not paused).
        if (!isTracking) return

        val last = lastAccepted
        if (last == null) {
            lastAccepted = newLocation
            appendCoordinate(newLocation)
            return
        }

        val segment = newLocation.distanceTo(last).toDouble()
        val intervalSec = (newLocation.time - last.time) / 1000.0

        // 3) Reject unrealistic jumps (teleport-like speed).
        if (intervalSec > 0 && segment / intervalSec > maxRealisticSpeed) return

        // 4) Reject near-duplicates / jitter while standing still.
        if (segment < minMoveDistance) return

        _totalDistanceMeters.value += segment
        lastAccepted = newLocation
        appendCoordinate(newLocation)
    }

    private fun appendCoordinate(location: Location) {
        _route.value = _route.value + Coordinate(location.latitude, location.longitude)
    }

    private fun startForegroundService() {
        val intent = Intent(context, LocationService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopForegroundService() {
        context.stopService(Intent(context, LocationService::class.java))
    }
}
