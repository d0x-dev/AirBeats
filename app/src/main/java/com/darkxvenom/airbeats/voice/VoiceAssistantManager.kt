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
    private var originalSystemVolume = -1
    private var originalNotificationVolume = -1

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow<String?>(null)
    val lastRecognizedText: StateFlow<String?> = _lastRecognizedText.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    companion object {
        private const val RESTART_DELAY_MS = 2000L
        private const val ERROR_RETRY_DELAY_MS = 2500L
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
            enterCommunicationMode()
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
            exitCommunicationMode()
        }
    }

    fun updateSettings(requireWakeWord: Boolean) {
        this.requireWakeWord = requireWakeWord
    }

    private fun ensureRecognizer() {
        if (speechRecognizer == null) {
            try {
                speechRecognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context.applicationContext).apply {
                        setRecognitionListener(this@VoiceAssistantManager)
                    }
                } else if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                        setRecognitionListener(this@VoiceAssistantManager)
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to create on-device SpeechRecognizer, falling back to default")
                try {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                        setRecognitionListener(this@VoiceAssistantManager)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun enterCommunicationMode() {
        try {
            audioManager?.let { am ->
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try { am.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0) } catch (_: Exception) {}
                    try { am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0) } catch (_: Exception) {}
                    try { am.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0) } catch (_: Exception) {}
                    try { am.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_MUTE, 0) } catch (_: Exception) {}
                } else {
                    try { am.setStreamMute(AudioManager.STREAM_SYSTEM, true) } catch (_: Exception) {}
                    try { am.setStreamMute(AudioManager.STREAM_NOTIFICATION, true) } catch (_: Exception) {}
                    try { am.setStreamMute(AudioManager.STREAM_RING, true) } catch (_: Exception) {}
                }
                try { am.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0) } catch (_: Exception) {}
                isSystemMuted = true
            }
        } catch (e: Exception) {
            Timber.e(e, "Error entering communication mode")
        }
    }

    private fun exitCommunicationMode() {
        try {
            audioManager?.let { am ->
                am.mode = AudioManager.MODE_NORMAL
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try { am.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0) } catch (_: Exception) {}
                    try { am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0) } catch (_: Exception) {}
                    try { am.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0) } catch (_: Exception) {}
                    try { am.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_UNMUTE, 0) } catch (_: Exception) {}
                } else {
                    try { am.setStreamMute(AudioManager.STREAM_SYSTEM, false) } catch (_: Exception) {}
                    try { am.setStreamMute(AudioManager.STREAM_NOTIFICATION, false) } catch (_: Exception) {}
                    try { am.setStreamMute(AudioManager.STREAM_RING, false) } catch (_: Exception) {}
                }
                if (originalSystemVolume != -1) {
                    try { am.setStreamVolume(AudioManager.STREAM_SYSTEM, originalSystemVolume, 0) } catch (_: Exception) {}
                }
                if (originalNotificationVolume != -1) {
                    try { am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalNotificationVolume, 0) } catch (_: Exception) {}
                }
                isSystemMuted = false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error exiting communication mode")
        }
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
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra("android.speech.extra.DICTATION_MODE", true)
                putExtra("android.speech.extra.GET_AUDIO_FOCUS", false)
                putExtra("android.speech.extra.AUDIO_FOCUS", false)
                putExtra("android.speech.extra.SUPPRESS_SOUND", true)
                putExtra("android.speech.extra.BEEP", false)
                putExtra("android.speech.extra.SILENT_RECORDING", true)
                putExtra("android.speech.extra.AUDIO_SOURCE", 7) // VOICE_COMMUNICATION with hardware AEC
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 60000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
            }

            enterCommunicationMode()

            // Mute STREAM_MUSIC during the 350ms start window so Google's start ding is 100% silent
            try {
                audioManager?.let { am ->
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                    mainHandler.postDelayed({
                        try { am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0) } catch (_: Exception) {}
                    }, 350)
                }
            } catch (_: Exception) {}

            recognizer.startListening(intent)
            _isListening.value = true
            isCurrentlyRecognizing = true
        } catch (e: Exception) {
            Timber.e(e, "Error in startRecognitionInternal")
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
        isCurrentlyRecognizing = false
    }

    override fun onError(error: Int) {
        Timber.d("SpeechRecognizer onError: %d", error)
        isCurrentlyRecognizing = false

        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH) {
            // Normal silence timeouts: immediately continue listening without dropping active state
            if (isRunning) {
                mainHandler.post { startRecognitionInternal() }
            }
            return
        }

        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }

        scheduleRestart(ERROR_RETRY_DELAY_MS)
    }

    override fun onResults(results: Bundle?) {
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

        // Immediately seamlessly continue listening
        if (isRunning) {
            mainHandler.post { startRecognitionInternal() }
        }
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
            exitCommunicationMode()
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Timber.e(e, "Error destroying SpeechRecognizer")
            }
            speechRecognizer = null
        }
    }
}
