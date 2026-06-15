package com.tertiaryinfotech.runtrackgps.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Speaks audio feedback using Android [TextToSpeech]. Milestone announcements are
 * de-duplicated so each one fires at most once per run (mirrors iOS SpeechFeedbackManager).
 */
class SpeechFeedbackManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    /** Queued utterances spoken once TTS finishes initializing. */
    private val pending = mutableListOf<String>()
    /** Keys of milestone announcements already spoken this run (e.g. "km-1", "half"). */
    private val spokenMilestones = mutableSetOf<String>()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ready = true
                pending.forEach { speakNow(it) }
                pending.clear()
            }
        }
    }

    // MARK: Core speak
    private fun speak(text: String) {
        if (ready) speakNow(text) else pending.add(text)
    }

    private fun speakNow(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    /** Speaks once per unique [key] per run. */
    private fun speakOnce(key: String, text: String) {
        if (!spokenMilestones.add(key)) return
        speak(text)
    }

    // MARK: Lifecycle announcements (always spoken)
    fun announceStarted() = speak("Run started. Good luck!")
    fun announcePaused() = speak("Run paused.")
    fun announceResumed() = speak("Run resumed.")
    fun announceStopped() = speak("Run stopped.")

    // MARK: Milestone announcements (de-duplicated)
    fun announceKilometre(km: Int) {
        val unit = if (km == 1) "kilometre" else "kilometres"
        speakOnce("km-$km", "You have completed $km $unit.")
    }

    fun announceHalfway() = speakOnce("half", "Halfway completed. Keep going!")
    fun announceNinetyPercent() = speakOnce("ninety", "90 percent completed. Almost there!")
    fun announceGoalReached() = speakOnce("goal", "Your goal is reached. Well done!")

    /** Clears milestone history for a new run. */
    fun reset() {
        spokenMilestones.clear()
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
