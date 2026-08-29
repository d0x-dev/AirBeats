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

class VoiceAssistantManager(
    private val context: Context,
    private val onWakeWordHeard: ((String) -> Unit)? = null,
    private val onCommandRecognized: (VoiceCommand, String) -> Unit
) : RecognitionListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var speechRecognizer: SpeechRecognizer? = null
    private var isRunning = false
    private var requireWakeWord = true
    private var isCurrentlyRecognizing = false
    private var isSystemMuted = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow<String?>(null)
    val lastRecognizedText: StateFlow<String?> = _lastRecognizedText.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    companion object {
        private const val RESTART_DELAY_MS = 300L
        private const val ERROR_RETRY_DELAY_MS = 800L
    }

    private val restartRunnable = Runnable {
        if (isRunning) {
            startRecognitionInternal()
        }
    }

    fun start(requireWakeWord: Boolean = true) {
        this.requireWakeWord = requireWakeWord
        if (isRunning) return
        isRunning = true

        mainHandler.post {
            muteSystemSound()
            ensureRecognizer()
            startRecognitionInternal()
        }
    }

    fun stop() {
        isRunning = false
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Timber.e(e, "Error stopping SpeechRecognizer")
            }
            _isListening.value = false
            isCurrentlyRecognizing = false
            restoreSystemSound()
        }
    }

    fun updateSettings(requireWakeWord: Boolean) {
        this.requireWakeWord = requireWakeWord
    }

    private fun ensureRecognizer() {
        if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                    setRecognitionListener(this@VoiceAssistantManager)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to create SpeechRecognizer")
            }
        }
    }

    private fun muteSystemSound() {
        if (isSystemMuted) return
        try {
            audioManager?.let { am ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
                    am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
                    am.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
                    am.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_MUTE, 0)
                } else {
                    am.setStreamMute(AudioManager.STREAM_SYSTEM, true)
                    am.setStreamMute(AudioManager.STREAM_NOTIFICATION, true)
                    am.setStreamMute(AudioManager.STREAM_RING, true)
                }
                isSystemMuted = true
            }
        } catch (_: Exception) {}
    }

    private fun restoreSystemSound() {
        if (!isSystemMuted) return
        try {
            audioManager?.let { am ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
                    am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
                    am.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
                    am.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_UNMUTE, 0)
                } else {
                    am.setStreamMute(AudioManager.STREAM_SYSTEM, false)
                    am.setStreamMute(AudioManager.STREAM_NOTIFICATION, false)
                    am.setStreamMute(AudioManager.STREAM_RING, false)
                }
                isSystemMuted = false
            }
        } catch (_: Exception) {}
    }

    private fun startRecognitionInternal() {
        if (!isRunning) return
        if (speechRecognizer == null) {
            ensureRecognizer()
        }

        val recognizer = speechRecognizer ?: run {
            Timber.w("SpeechRecognizer is not available on this device")
            scheduleRestart(ERROR_RETRY_DELAY_MS)
            return
        }

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra("android.speech.extra.DICTATION_MODE", true)
                putExtra("android.speech.extra.GET_AUDIO_FOCUS", false)
                putExtra("android.speech.extra.AUDIO_FOCUS", false)
                putExtra("android.speech.extra.SUPPRESS_SOUND", true)
                putExtra("android.speech.extra.BEEP", false)
                putExtra("android.speech.extra.SILENT_RECORDING", true)
                putExtra("android.speech.extra.AUDIO_SOURCE", 6) // VOICE_RECOGNITION with echo cancellation
                putExtra("android.speech.extras.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 3000L)
                putExtra("android.speech.extras.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 2000L)
                putExtra("android.speech.extras.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 2000L)
            }

            // Ensure system sounds are muted to prevent Google SpeechRecognizer ding
            muteSystemSound()

            if (isCurrentlyRecognizing) {
                try { recognizer.cancel() } catch (_: Exception) {}
            }
            recognizer.startListening(intent)
            _isListening.value = true
            isCurrentlyRecognizing = true
        } catch (e: Exception) {
            Timber.e(e, "Error in startRecognitionInternal")
            _isListening.value = false
            isCurrentlyRecognizing = false
            scheduleRestart(ERROR_RETRY_DELAY_MS)
        }
    }

    private fun scheduleRestart(delayMs: Long = RESTART_DELAY_MS) {
        if (!isRunning) return
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.postDelayed(restartRunnable, delayMs)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
        isCurrentlyRecognizing = true
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
        isCurrentlyRecognizing = false
    }

    override fun onError(error: Int) {
        _isListening.value = false
        isCurrentlyRecognizing = false
        Timber.d("SpeechRecognizer onError: %d", error)

        val delay = when (error) {
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            SpeechRecognizer.ERROR_NO_MATCH -> RESTART_DELAY_MS
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_CLIENT -> {
                try {
                    speechRecognizer?.destroy()
                } catch (_: Exception) {}
                speechRecognizer = null
                ERROR_RETRY_DELAY_MS
            }
            else -> ERROR_RETRY_DELAY_MS
        }

        scheduleRestart(delay)
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        isCurrentlyRecognizing = false

        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val topText = matches.first().trim()
            _lastRecognizedText.value = topText
            Timber.i("Spoken candidates recognized: %s", matches.joinToString(" | "))

            for (candidate in matches) {
                val command = VoiceCommandParser.parse(candidate.trim(), requireWakeWord = requireWakeWord)
                if (command !is VoiceCommand.Unknown) {
                    onCommandRecognized(command, candidate.trim())
                    break
                }
            }
        }

        scheduleRestart(RESTART_DELAY_MS)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partialText = matches?.firstOrNull()?.trim()
        if (!partialText.isNullOrBlank()) {
            _lastRecognizedText.value = partialText
            if (VoiceCommandParser.containsWakeWord(partialText)) {
                onWakeWordHeard?.invoke(partialText)
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        stop()
        mainHandler.post {
            restoreSystemSound()
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Timber.e(e, "Error destroying SpeechRecognizer")
            }
            speechRecognizer = null
        }
    }
}
