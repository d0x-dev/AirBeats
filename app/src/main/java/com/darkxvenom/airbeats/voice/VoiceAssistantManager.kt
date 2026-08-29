package com.darkxvenom.airbeats.voice

import android.content.Context
import android.content.Intent
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
 * High-accuracy On-Device Voice Assistant Manager.
 * Transcribes spoken voice into exact commands without continuous background beep loops.
 */
class VoiceAssistantManager(
    private val context: Context,
    private val onWakeWordHeard: ((String) -> Unit)? = null,
    private val onCommandRecognized: (VoiceCommand, String) -> Unit
) : RecognitionListener {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRunning = false
    private var requireWakeWord = true
    private var isListeningSessionActive = false
    private var lastTriggerTimestamp = 0L

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow<String?>(null)
    val lastRecognizedText: StateFlow<String?> = _lastRecognizedText.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    fun start(requireWakeWord: Boolean = true) {
        this.requireWakeWord = requireWakeWord
        if (isRunning) return
        isRunning = true
        _isListening.value = true

        mainHandler.post {
            ensureRecognizer()
            startListeningInternal()
        }
    }

    fun stop() {
        isRunning = false
        _isListening.value = false
        isListeningSessionActive = false

        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Timber.e(e, "Error stopping SpeechRecognizer")
            }
        }
    }

    fun updateSettings(requireWakeWord: Boolean) {
        this.requireWakeWord = requireWakeWord
    }

    fun triggerListeningSession() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            isListeningSessionActive = false
            startListeningInternal()
        }
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

    private fun startListeningInternal() {
        if (!isRunning) return
        ensureRecognizer()

        val recognizer = speechRecognizer ?: run {
            Timber.w("SpeechRecognizer unavailable")
            return
        }

        if (isListeningSessionActive) return

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
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 40000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            }

            recognizer.startListening(intent)
            isListeningSessionActive = true
            _isListening.value = true
        } catch (e: Exception) {
            Timber.e(e, "Error starting speech recognition")
            isListeningSessionActive = false
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        isListeningSessionActive = true
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {
        isListeningSessionActive = true
    }

    override fun onRmsChanged(rmsdB: Float) {
        _audioRms.value = rmsdB
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        isListeningSessionActive = false
    }

    override fun onError(error: Int) {
        isListeningSessionActive = false
        Timber.d("SpeechRecognizer onError: %d", error)

        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH) {
            if (isRunning) {
                mainHandler.postDelayed({ startListeningInternal() }, 1500L)
            }
            return
        }

        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }

        if (isRunning) {
            mainHandler.postDelayed({ startListeningInternal() }, 2000L)
        }
    }

    override fun onResults(results: Bundle?) {
        isListeningSessionActive = false

        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val topText = matches.first().trim()
            _lastRecognizedText.value = topText
            Timber.i("Spoken command candidate: %s", topText)

            val now = System.currentTimeMillis()
            if (now - lastTriggerTimestamp > 3000L) {
                lastTriggerTimestamp = now

                var matchedCommand: VoiceCommand? = null
                var matchedText = topText

                for (candidate in matches) {
                    val parsed = VoiceCommandParser.parse(candidate.trim(), requireWakeWord = requireWakeWord)
                    if (parsed !is VoiceCommand.Unknown) {
                        matchedCommand = parsed
                        matchedText = candidate.trim()
                        break
                    }
                }

                // If not matched directly with wake word, check if direct command or specific song name
                if (matchedCommand == null || matchedCommand is VoiceCommand.Unknown) {
                    val directParsed = VoiceCommandParser.parse(topText, requireWakeWord = false)
                    if (directParsed !is VoiceCommand.Unknown) {
                        matchedCommand = directParsed
                    } else if (topText.isNotBlank()) {
                        // User spoke a specific song title like "Starboy" or "Blinding Lights"
                        matchedCommand = VoiceCommand.PlaySong(topText)
                    }
                }

                matchedCommand?.let { cmd ->
                    onWakeWordHeard?.invoke(matchedText)
                    onCommandRecognized(cmd, matchedText)
                }
            }
        }

        if (isRunning) {
            mainHandler.postDelayed({ startListeningInternal() }, 1000L)
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
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Timber.e(e, "Error destroying SpeechRecognizer")
            }
            speechRecognizer = null
        }
    }
}
