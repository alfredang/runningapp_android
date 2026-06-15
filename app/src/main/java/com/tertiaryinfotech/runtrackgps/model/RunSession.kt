package com.tertiaryinfotech.runtrackgps.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A geographic coordinate. Stored through this lightweight wrapper so routes are
 * easily JSON-serializable (mirrors the iOS `Coordinate` type).
 */
@Serializable
data class Coordinate(
    val latitude: Double,
    val longitude: Double,
)

/**
 * The full record of a single run. Used live (during a run) and for persistence
 * of completed runs (see [com.tertiaryinfotech.runtrackgps.util.RunStore]).
 */
@Serializable
data class RunSession(
    val id: String = UUID.randomUUID().toString(),
    val goalDistanceMeters: Double,
    val distanceMeters: Double = 0.0,
    val elapsedTime: Double = 0.0,            // seconds
    val averagePaceSecPerKm: Double? = null,
    val currentPaceSecPerKm: Double? = null,
    val routeCoordinates: List<Coordinate> = emptyList(),
    val startTimeEpoch: Long? = null,         // millis
    val endTimeEpoch: Long? = null,           // millis
    val isCompleted: Boolean = false,
) {
    val goalDistanceKm: Double get() = goalDistanceMeters / 1000.0
    val distanceKm: Double get() = distanceMeters / 1000.0

    val remainingMeters: Double get() = (goalDistanceMeters - distanceMeters).coerceAtLeast(0.0)
    val remainingKm: Double get() = remainingMeters / 1000.0

    /** 0..1 progress toward the goal. */
    val progressFraction: Double
        get() = if (goalDistanceMeters > 0) (distanceMeters / goalDistanceMeters).coerceIn(0.0, 1.0) else 0.0
}
