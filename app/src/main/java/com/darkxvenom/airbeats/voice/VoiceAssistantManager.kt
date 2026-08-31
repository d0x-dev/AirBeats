package com.darkxvenom.airbeats.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Continuous Real-Time SpeechRecognizer Engine for AirBeats.
 * Provides 100% identical listening power in background as clicking 'Speak'.
 * 1. Always-on SpeechRecognizer continuous loop (no AudioRecord handoff latency).
 * 2. Zero System Beeps: Suppresses start dings on every cycle.
 * 3. Shows HUD pill with live streaming text on 'Hi AirBeats' and direct commands.
 */
class VoiceAssistantManager(
    private val context: Context,
    private val onWakeWordHeard: ((String) -> Unit)? = null,
    private val onCommandRecognized: (VoiceCommand, String) -> Unit
) : RecognitionListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var isRunning = false
    private var requireWakeWord = false
    private var isRecognizing = false
    private var isManualSession = false

    @Volatile
    private var isTtsSpeaking = false
    private var ttsFinishedTimestamp = 0L

    private var speechRecognizer: SpeechRecognizer? = null
    private var originalSystemVolume = -1
    private var originalNotificationVolume = -1
    private var isSystemMuted = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow<String?>(null)
    val lastRecognizedText: StateFlow<String?> = _lastRecognizedText.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    companion object {
        private const val RESTART_DELAY_MS = 80L
        private const val TTS_SILENCE_GRACE_MS = 1000L
    }

    fun setTtsSpeaking(speaking: Boolean) {
        isTtsSpeaking = speaking
        if (speaking) {
            cancelRecognition()
        } else {
            ttsFinishedTimestamp = System.currentTimeMillis()
            mainHandler.postDelayed({
                if (isRunning && !isTtsSpeaking) {
                    startContinuousRecognition()
                }
            }, TTS_SILENCE_GRACE_MS)
        }
    }

    fun start(requireWakeWord: Boolean = false) {
        this.requireWakeWord = requireWakeWord
        if (isRunning) return
        isRunning = true
        _isListening.value = true

        mainHandler.post {
            startContinuousRecognition()
        }
        Timber.i("VoiceAssistantManager: Continuous Full-Power SpeechRecognizer started")
    }

    fun stop() {
        isRunning = false
        _isListening.value = false
        cancelRecognition()
    }

    fun updateSettings(requireWakeWord: Boolean) {
        this.requireWakeWord = requireWakeWord
    }

    /**
     * Triggered manually when user taps 'Speak' in notification or HUD.
     */
    fun triggerListeningSession() {
        mainHandler.post {
            val now = System.currentTimeMillis()
            if (isTtsSpeaking || (now - ttsFinishedTimestamp < TTS_SILENCE_GRACE_MS)) {
                return@post
            }
            isManualSession = true
            _lastRecognizedText.value = "Listening..."
            onWakeWordHeard?.invoke("Listening...")
            restartRecognition(forceManual = true)
        }
    }

    private fun muteSystemSound() {
        try {
            audioManager?.let { am ->
                if (originalSystemVolume == -1) {
                    originalSystemVolume = am.getStreamVolume(AudioManager.STREAM_SYSTEM)
                }
                if (originalNotificationVolume == -1) {
                    originalNotificationVolume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try { am.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0) } catch (_: Exception) {}
                    try { am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0) } catch (_: Exception) {}
                } else {
                    try { am.setStreamMute(AudioManager.STREAM_SYSTEM, true) } catch (_: Exception) {}
                    try { am.setStreamMute(AudioManager.STREAM_NOTIFICATION, true) } catch (_: Exception) {}
                }
                try { am.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0) } catch (_: Exception) {}
                isSystemMuted = true
            }
        } catch (_: Exception) {}
    }

    private fun restoreSystemSound() {
        if (!isSystemMuted) return
        try {
            audioManager?.let { am ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try { am.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0) } catch (_: Exception) {}
                    try { am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0) } catch (_: Exception) {}
                } else {
                    try { am.setStreamMute(AudioManager.STREAM_SYSTEM, false) } catch (_: Exception) {}
                    try { am.setStreamMute(AudioManager.STREAM_NOTIFICATION, false) } catch (_: Exception) {}
                }
                if (originalSystemVolume != -1) {
                    try { am.setStreamVolume(AudioManager.STREAM_SYSTEM, originalSystemVolume, 0) } catch (_: Exception) {}
                }
                if (originalNotificationVolume != -1) {
                    try { am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalNotificationVolume, 0) } catch (_: Exception) {}
                }
                isSystemMuted = false
            }
        } catch (_: Exception) {}
    }

    private fun startContinuousRecognition() {
        if (!isRunning || isTtsSpeaking || isRecognizing) return
        isRecognizing = true
        _isListening.value = true

        try {
            muteSystemSound()

            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                    setRecognitionListener(this@VoiceAssistantManager)
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                putExtra("android.speech.extra.DICTATION_MODE", true)
                putExtra("android.speech.extras.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 6000L)
                putExtra("android.speech.extras.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 2000L)
                putExtra("android.speech.extras.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 1500L)
            }

            speechRecognizer?.startListening(intent)
            Timber.d("Continuous SpeechRecognizer active")
        } catch (e: Exception) {
            Timber.e(e, "Error starting continuous SpeechRecognizer")
            scheduleAutoRestart()
        }
    }

    private fun restartRecognition(forceManual: Boolean = false) {
        isRecognizing = false
        if (forceManual) isManualSession = true
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}

        mainHandler.postDelayed({
            if (isRunning && !isTtsSpeaking) {
                startContinuousRecognition()
            }
        }, RESTART_DELAY_MS)
    }

    private fun scheduleAutoRestart() {
        isRecognizing = false
        isManualSession = false
        restoreSystemSound()

        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}

        mainHandler.postDelayed({
            if (isRunning && !isTtsSpeaking && !isRecognizing) {
                startContinuousRecognition()
            }
        }, RESTART_DELAY_MS)
    }

    private fun cancelRecognition() {
        isRecognizing = false
        isManualSession = false
        _isListening.value = false
        mainHandler.post {
            restoreSystemSound()
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
    }

    // --- RecognitionListener Callbacks ---

    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
        restoreSystemSound()
    }

    override fun onBeginningOfSpeech() {
        _isListening.value = true
    }

    override fun onRmsChanged(rmsdB: Float) {
        _audioRms.value = rmsdB
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
    }

    override fun onError(error: Int) {
        Timber.d("Continuous SpeechRecognizer onError: %d", error)
        _isListening.value = false

        // In continuous mode, silently restart recognition
        scheduleAutoRestart()
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

        if (!matches.isNullOrEmpty()) {
            val topText = matches.first().trim()
            _lastRecognizedText.value = topText
            Timber.i("Voice Assistant recognized: %s", matches.joinToString(" | "))

            var commandExecuted = false
            for (candidate in matches) {
                val command = VoiceCommandParser.parse(candidate.trim(), requireWakeWord = requireWakeWord)
                if (command !is VoiceCommand.Unknown) {
                    onCommandRecognized(command, candidate.trim())
                    commandExecuted = true
                    break
                }
            }

            // Check if user spoke ONLY the wake word (e.g. "Hi AirBeats", "Hey AirBeats")
            if (!commandExecuted && VoiceCommandParser.containsWakeWord(topText)) {
                val directCmd = VoiceCommandParser.parse(topText, requireWakeWord = false)
                if (directCmd is VoiceCommand.Unknown) {
                    Timber.i("Wake word detected ('%s') -> Showing HUD & listening for command...", topText)
                    onWakeWordHeard?.invoke("Listening...")
                    restartRecognition(forceManual = true)
                    return
                }
            }

            // Fallback: If direct commands are accepted or user spoke a song title
            if (!commandExecuted && topText.isNotBlank()) {
                val directCommand = VoiceCommandParser.parse(topText, requireWakeWord = false)
                if (directCommand !is VoiceCommand.Unknown) {
                    onCommandRecognized(directCommand, topText)
                } else if (topText.length >= 3) {
                    onCommandRecognized(VoiceCommand.PlaySong(topText), topText)
                }
            }
        }

        scheduleAutoRestart()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partialText = matches?.firstOrNull()?.trim()
        if (!partialText.isNullOrBlank()) {
            _lastRecognizedText.value = partialText
            // Show real-time streaming HUD on spoken words
            onWakeWordHeard?.invoke(partialText)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        stop()
        mainHandler.post {
            restoreSystemSound()
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
    }
}
