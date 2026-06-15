package com.tertiaryinfotech.runtrackgps.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.tertiaryinfotech.runtrackgps.model.Coordinate

/**
 * Live route map backed by Google Maps Compose.
 *
 * The Android counterpart to the iOS `RouteMapView` (MapKit). It draws the route
 * polyline, a start marker and a "you" marker, and follows the user when [followUser].
 */
@Composable
fun RouteMap(
    route: List<Coordinate>,
    currentLocation: Coordinate?,
    followUser: Boolean,
    locationPermitted: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onCameraMovedByUser: () -> Unit = {},
) {
    val cameraPositionState = rememberCameraPositionState()
    var didCenterOnce by remember { mutableStateOf(false) }

    // Follow the user: recenter the camera on each new fix while follow is on.
    LaunchedEffect(currentLocation, followUser) {
        val c = currentLocation
        if (followUser && c != null) {
            val target = LatLng(c.latitude, c.longitude)
            val update = CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(target, 16f),
            )
            if (didCenterOnce) cameraPositionState.animate(update) else cameraPositionState.move(update)
            didCenterOnce = true
        }
    }

    // A user-initiated pan/zoom turns off follow mode.
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving &&
            cameraPositionState.cameraMoveStartedReason ==
            com.google.maps.android.compose.CameraMoveStartedReason.GESTURE
        ) {
            onCameraMovedByUser()
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            mapType = MapType.NORMAL,
            isMyLocationEnabled = locationPermitted,
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = true,
        ),
    ) {
        if (route.size >= 2) {
            Polyline(
                points = route.map { LatLng(it.latitude, it.longitude) },
                color = accent,
                width = 16f,
            )
        }
        route.firstOrNull()?.let { start ->
            Marker(
                state = MarkerState(LatLng(start.latitude, start.longitude)),
                title = "Start",
            )
        }
    }
}
