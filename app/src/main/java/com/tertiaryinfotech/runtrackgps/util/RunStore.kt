package com.tertiaryinfotech.runtrackgps.util

import android.content.Context
import com.tertiaryinfotech.runtrackgps.model.RunSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Lightweight persistence of completed runs as JSON in SharedPreferences.
 * Kept deliberately simple — no Room for a lightweight app (mirrors the iOS RunStore).
 */
class RunStore(context: Context) {

    private val prefs = context.getSharedPreferences("RunTrackGPS", Context.MODE_PRIVATE)
    private val key = "savedRuns"
    private val maxStored = 50
    private val json = Json { ignoreUnknownKeys = true }

    /** All saved runs, most recent first. */
    fun allRuns(): List<RunSession> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        val runs = runCatching { json.decodeFromString<List<RunSession>>(raw) }.getOrDefault(emptyList())
        return runs.sortedByDescending { it.endTimeEpoch ?: Long.MIN_VALUE }
    }

    /**
     * Appends a run and trims history to [maxStored]. Route coordinates are dropped
     * to keep on-device storage small — history only needs distance/time/pace/date.
     */
    fun save(session: RunSession) {
        val summary = session.copy(routeCoordinates = emptyList())
        val runs = allRuns().filterNot { it.id == summary.id }.toMutableList()
        runs.add(0, summary)
        persist(runs.take(maxStored))
    }

    /** Removes a saved run by id. */
    fun delete(id: String) = persist(allRuns().filterNot { it.id == id })

    /** Deletes all saved runs. */
    fun clear() = prefs.edit().remove(key).apply()

    private fun persist(runs: List<RunSession>) {
        prefs.edit().putString(key, json.encodeToString(runs)).apply()
    }
}
