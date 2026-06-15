package com.tertiaryinfotech.runtrackgps.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Recognized voice commands. */
enum class VoiceCommand { START, PAUSE, RESUME, STOP }

/**
 * Continuous on-device speech recognition that maps spoken keywords to
 * [VoiceCommand]s. Voice *commands* are a foreground feature — Android suspends the
 * mic in the background (voice *feedback* still works there). Mirrors the iOS
 * VoiceCommandManager built on the Speech framework.
 *
 * SpeechRecognizer is main-thread-only, so all engine calls hop to the main looper.
 */
class VoiceCommandManager(private val context: Context) {

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastCommandText = MutableStateFlow("")
    val lastCommandText: StateFlow<String> = _lastCommandText.asStateFlow()

    /** Invoked on the main thread when a command is detected. */
    var onCommand: ((VoiceCommand) -> Unit)? = null

    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var wantListening = false

    // Debounce: ignore the same command if fired within this window.
    private val debounceMs = 2_000L
    private var lastCommand: VoiceCommand? = null
    private var lastCommandTime = 0L

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    // MARK: Listening lifecycle
    fun startListening() {
        if (_isListening.value || !isAvailable) return
        wantListening = true
        main.post { beginRecognition() }
    }

    fun stopListening() {
        wantListening = false
        main.post {
            recognizer?.destroy()
            recognizer = null
            _isListening.value = false
        }
    }

    private fun beginRecognition() {
        if (!wantListening) return
        recognizer?.destroy()
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        sr.setRecognitionListener(listener)
        recognizer = sr
        val intent = RecognizerIntent.getVoiceDetailsIntent(context).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        }
        try {
            sr.startListening(intent)
            _isListening.value = true
        } catch (_: Exception) {
            _isListening.value = false
        }
    }

    /** Restarts the engine (a recognition session ends after each utterance/silence). */
    private fun restart() {
        if (!wantListening) return
        main.postDelayed({ beginRecognition() }, 250)
    }

    private val listener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            handle(results)
            restart()
        }

        override fun onPartialResults(partialResults: Bundle?) = handle(partialResults)

        override fun onError(error: Int) {
            // Busy/timeout/no-match are expected in a continuous loop — just restart.
            restart()
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun handle(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        val phrase = matches.firstOrNull() ?: return
        _lastCommandText.value = phrase
        parse(phrase.lowercase())?.let { fire(it) }
    }

    // MARK: Keyword parsing (order matters — resume/pause/stop before generic start)
    private fun parse(text: String): VoiceCommand? = when {
        text.contains("resume") -> VoiceCommand.RESUME
        text.contains("pause") -> VoiceCommand.PAUSE
        text.contains("stop") || text.contains("finish") -> VoiceCommand.STOP
        text.contains("start") || text.contains("begin") -> VoiceCommand.START
        else -> null
    }

    private fun fire(command: VoiceCommand) {
        val now = System.currentTimeMillis()
        if (command == lastCommand && now - lastCommandTime < debounceMs) return
        lastCommand = command
        lastCommandTime = now
        main.post { onCommand?.invoke(command) }
    }
}
