package com.tertiaryinfotech.runtrackgps.util

import kotlin.math.roundToInt

/** Pure pace math + formatting. Pace is stored as seconds-per-kilometre. */
object PaceCalculator {

    /**
     * Average pace over the whole run: `elapsed / distanceKm`.
     * Returns null when distance is too small to be meaningful.
     */
    fun pace(elapsedSeconds: Double, distanceMeters: Double): Double? {
        val km = distanceMeters / 1000.0
        if (km <= 0.01 || elapsedSeconds <= 0) return null
        return elapsedSeconds / km
    }

    /** Formats seconds-per-km as `"6:20 min/km"`. Dash placeholder for null/invalid input. */
    fun format(secPerKm: Double?): String {
        if (secPerKm == null || !secPerKm.isFinite() || secPerKm <= 0) return "--:-- min/km"
        val total = secPerKm.roundToInt()
        return "%d:%02d min/km".format(total / 60, total % 60)
    }

    /** Short pace form without the unit suffix, e.g. `"6:20"`. */
    fun formatShort(secPerKm: Double?): String {
        if (secPerKm == null || !secPerKm.isFinite() || secPerKm <= 0) return "--:--"
        val total = secPerKm.roundToInt()
        return "%d:%02d".format(total / 60, total % 60)
    }

    /** Formats a distance in metres as kilometres with two decimals, e.g. `"6.20 km"`. */
    fun formatKm(meters: Double): String = "%.2f km".format(meters / 1000.0)

    /** Formats seconds as `H:MM:SS` (hours dropped when zero), e.g. `"42:13"`. */
    fun formatTime(seconds: Double): String {
        val total = seconds.roundToInt()
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
