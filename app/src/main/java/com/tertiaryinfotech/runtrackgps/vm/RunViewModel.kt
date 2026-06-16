package com.tertiaryinfotech.runtrackgps.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tertiaryinfotech.runtrackgps.location.LocationManager
import com.tertiaryinfotech.runtrackgps.model.AppScreen
import com.tertiaryinfotech.runtrackgps.model.Coordinate
import com.tertiaryinfotech.runtrackgps.model.RunSession
import com.tertiaryinfotech.runtrackgps.timer.RunTimerManager
import com.tertiaryinfotech.runtrackgps.util.PaceCalculator
import com.tertiaryinfotech.runtrackgps.util.RunStore
import com.tertiaryinfotech.runtrackgps.voice.SpeechFeedbackManager
import com.tertiaryinfotech.runtrackgps.voice.VoiceCommand
import com.tertiaryinfotech.runtrackgps.voice.VoiceCommandManager
import kotlinx.coroutines.launch

/** User-facing alerts surfaced by the view model. */
enum class RunAlert(val title: String, val message: String) {
    LOCATION_DENIED(
        "Location Needed",
        "RunTrack GPS needs location access to track your run. Please enable it in Settings.",
    ),
    INVALID_GOAL(
        "Invalid Distance",
        "Please enter a distance between 0 and 500 km.",
    ),
}

/**
 * Central MVVM coordinator. Owns the four managers, exposes view state as Compose
 * state, and turns user/voice actions into manager calls. Composables observe this
 * object only (mirrors the iOS RunViewModel).
 */
class RunViewModel(app: Application) : AndroidViewModel(app) {

    // MARK: Managers
    val location = LocationManager(app)
    private val timer = RunTimerManager(viewModelScope)
    val feedback = SpeechFeedbackManager(app)
    val voice = VoiceCommandManager(app)
    private val store = RunStore(app)

    // MARK: Navigation + goal state
    var screen by mutableStateOf(AppScreen.HOME)
        private set
    var goalDistanceMeters by mutableStateOf(10_000.0)
        private set
    var customDistanceText by mutableStateOf("")

    // MARK: Live run state
    var distanceMeters by mutableStateOf(0.0)
        private set
    var elapsed by mutableStateOf(0.0)
        private set
    var currentPaceSecPerKm by mutableStateOf<Double?>(null)
        private set
    var averagePaceSecPerKm by mutableStateOf<Double?>(null)
        private set
    var isPaused by mutableStateOf(false)
        private set
    var followUser by mutableStateOf(true)
        private set

    // Map mirrors of the manager flows.
    var route by mutableStateOf<List<Coordinate>>(emptyList())
        private set
    var currentLocation by mutableStateOf<Coordinate?>(null)
        private set
    var isListening by mutableStateOf(false)
        private set
    var isAccuracyPoor by mutableStateOf(false)
        private set

    // Permission snapshots (refreshed after the runtime prompt resolves).
    var isLocationAuthorized by mutableStateOf(false)
        private set

    // MARK: Completed run + alerts + history
    var completedSession by mutableStateOf<RunSession?>(null)
        private set
    var activeAlert by mutableStateOf<RunAlert?>(null)
    var pastRuns by mutableStateOf<List<RunSession>>(emptyList())
        private set

    /** Preset goals shown on the Home screen. */
    val presets = listOf(5_000.0, 10_000.0, 20_000.0, 40_000.0)

    private var startTimeEpoch: Long? = null
    private var milestoneKm = 0

    init {
        collectFlows()
        wireVoiceCommands()
        refreshHistory()
        refreshPermissions()
    }

    val mostRecentRun: RunSession? get() = pastRuns.firstOrNull()

    private fun refreshHistory() { pastRuns = store.allRuns() }

    fun refreshPermissions() {
        isLocationAuthorized = location.isAuthorized
    }

    fun deleteRun(session: RunSession) {
        store.delete(session.id)
        refreshHistory()
    }

    fun clearHistory() {
        store.clear()
        refreshHistory()
    }

    // MARK: Goal selection
    fun selectPreset(meters: Double) {
        goalDistanceMeters = meters
        customDistanceText = ""
    }

    val isPresetSelected: Boolean get() = presets.contains(goalDistanceMeters)

    /** Applies a custom goal typed in kilometres. Returns false if the input is invalid. */
    fun applyCustomGoal(): Boolean {
        val km = customDistanceText.replace(",", ".").toDoubleOrNull()
        if (km == null || km <= 0 || km > 500) {
            activeAlert = RunAlert.INVALID_GOAL
            return false
        }
        goalDistanceMeters = km * 1000
        return true
    }

    // MARK: Run lifecycle
    fun startRun() {
        refreshPermissions()
        if (!location.isAuthorized) {
            activeAlert = RunAlert.LOCATION_DENIED
            return
        }
        resetForNewRun()
        startTimeEpoch = System.currentTimeMillis()
        location.startTracking()
        timer.start()
        voice.startListening()
        feedback.announceStarted()
        isPaused = false
        screen = AppScreen.RUNNING
    }

    fun pause() {
        if (screen != AppScreen.RUNNING || isPaused) return
        isPaused = true
        timer.pause()
        location.pauseTracking()
        feedback.announcePaused()
    }

    fun resume() {
        if (screen != AppScreen.RUNNING || !isPaused) return
        isPaused = false
        timer.resume()
        location.resumeTracking()
        feedback.announceResumed()
    }

    /** Stops the run. [completed] is true when the goal was reached. */
    fun stop(completed: Boolean) {
        if (screen != AppScreen.RUNNING) return
        timer.stop()
        location.stopTracking()
        voice.stopListening()
        if (completed) feedback.announceGoalReached() else feedback.announceStopped()
        completedSession = buildSession(completed)
        screen = AppScreen.COMPLETION
    }

    // MARK: Completion actions
    fun saveRun() {
        completedSession?.let {
            store.save(it)
            refreshHistory()
        }
    }

    fun startNewRun() {
        completedSession = null
        resetForNewRun()
        screen = AppScreen.HOME
    }

    fun recenter() { followUser = true }

    fun setFollow(value: Boolean) { followUser = value }

    // MARK: Private
    private fun resetForNewRun() {
        location.reset()
        timer.reset()
        feedback.reset()
        distanceMeters = 0.0
        elapsed = 0.0
        currentPaceSecPerKm = null
        averagePaceSecPerKm = null
        milestoneKm = 0
        route = emptyList()
        followUser = true
    }

    private fun collectFlows() {
        viewModelScope.launch {
            location.totalDistanceMeters.collect { handleDistance(it) }
        }
        viewModelScope.launch { location.route.collect { route = it } }
        viewModelScope.launch { location.currentLocation.collect { currentLocation = it } }
        viewModelScope.launch { location.isAccuracyPoor.collect { isAccuracyPoor = it } }
        viewModelScope.launch { voice.isListening.collect { isListening = it } }
        viewModelScope.launch {
            timer.elapsed.collect { value ->
                elapsed = value
                averagePaceSecPerKm = PaceCalculator.pace(value, distanceMeters)
            }
        }
    }

    private fun handleDistance(meters: Double) {
        if (screen != AppScreen.RUNNING) return
        distanceMeters = meters
        averagePaceSecPerKm = PaceCalculator.pace(elapsed, meters)
        currentPaceSecPerKm = averagePaceSecPerKm
        announceMilestonesIfNeeded(meters)
        if (meters >= goalDistanceMeters && goalDistanceMeters > 0) stop(completed = true)
    }

    private fun announceMilestonesIfNeeded(meters: Double) {
        val fraction = if (goalDistanceMeters > 0) meters / goalDistanceMeters else 0.0
        val completedKm = (meters / 1000).toInt()
        if (completedKm > milestoneKm) {
            milestoneKm = completedKm
            feedback.announceKilometre(completedKm)
        }
        if (fraction >= 0.5) feedback.announceHalfway()
        if (fraction >= 0.9) feedback.announceNinetyPercent()
    }

    private fun buildSession(isCompleted: Boolean) = RunSession(
        goalDistanceMeters = goalDistanceMeters,
        distanceMeters = distanceMeters,
        elapsedTime = elapsed,
        averagePaceSecPerKm = PaceCalculator.pace(elapsed, distanceMeters),
        currentPaceSecPerKm = currentPaceSecPerKm,
        routeCoordinates = route,
        startTimeEpoch = startTimeEpoch,
        endTimeEpoch = System.currentTimeMillis(),
        isCompleted = isCompleted,
    )

    private fun wireVoiceCommands() {
        voice.onCommand = { command ->
            when (command) {
                VoiceCommand.START -> if (screen == AppScreen.HOME) startRun()
                VoiceCommand.PAUSE -> pause()
                VoiceCommand.RESUME -> resume()
                VoiceCommand.STOP -> stop(completed = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        feedback.shutdown()
        voice.stopListening()
    }
}
