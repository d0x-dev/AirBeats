package com.darkxvenom.airbeats.voice

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Intelligent Silent AudioRecord Voice Assistant Manager.
 * Uses 100% silent AudioRecord for background idle monitoring, and single-turn on-device
 * transcription to recognize specific commands (e.g., "play Starboy", "pause", "next", "volume 80").
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
    private var isTranscribing = false
    private var lastTriggerTimestamp = 0L

    @Volatile
    private var isTtsSpeaking = false
    private var ttsFinishedTimestamp = 0L

    // Pure Native AudioRecord Streaming Engine
    private var audioRecord: AudioRecord? = null
    private var isAudioRecordRunning = false
    private var audioRecordThread: Thread? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow<String?>(null)
    val lastRecognizedText: StateFlow<String?> = _lastRecognizedText.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    fun setTtsSpeaking(speaking: Boolean) {
        isTtsSpeaking = speaking
        if (!speaking) {
            ttsFinishedTimestamp = System.currentTimeMillis()
        }
    }

    fun start(requireWakeWord: Boolean = true) {
        this.requireWakeWord = requireWakeWord
        if (isRunning) return
        isRunning = true
        _isListening.value = true

        startContinuousAudioRecord()
    }

    fun stop() {
        isRunning = false
        _isListening.value = false
        stopContinuousAudioRecord()

        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Timber.e(e, "Error stopping SpeechRecognizer")
            }
            isTranscribing = false
        }
    }

    fun updateSettings(requireWakeWord: Boolean) {
        this.requireWakeWord = requireWakeWord
    }

    fun triggerListeningSession() {
        mainHandler.post {
            startOnDemandTranscription()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startContinuousAudioRecord() {
        if (isAudioRecordRunning) return
        isAudioRecordRunning = true

        audioRecordThread = Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = (minBufSize * 2).coerceAtLeast(4096)

            try {
                val record = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    Timber.e("AudioRecord failed to initialize")
                    record.release()
                    return@Thread
                }

                audioRecord = record
                record.startRecording()
                Timber.i("Continuous silent AudioRecord active")

                val buffer = ShortArray(1024)
                var ambientNoiseFloor = 0.0
                var speechFrames = 0

                while (isRunning && isAudioRecordRunning) {
                    val now = System.currentTimeMillis()
                    val isSelfSpeaking = isTtsSpeaking || (now - ttsFinishedTimestamp < 1500L)

                    if (isTranscribing || isSelfSpeaking) {
                        // Pause / Ignore AudioRecord while speech recognizer is processing or TTS feedback is playing!
                        speechFrames = 0
                        Thread.sleep(100)
                        continue
                    }

                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        var sum = 0.0
                        var maxVal = 0
                        for (i in 0 until read) {
                            val v = buffer[i].toInt()
                            sum += v * v
                            val absV = abs(v)
                            if (absV > maxVal) maxVal = absV
                        }
                        val rms = sqrt(sum / read)
                        val db = if (rms > 0) (20 * log10(rms / 32767.0) + 90.0).coerceAtLeast(0.0) else 0.0
                        _audioRms.value = db.toFloat()

                        if (ambientNoiseFloor == 0.0) {
                            ambientNoiseFloor = db
                        } else {
                            ambientNoiseFloor = ambientNoiseFloor * 0.98 + db * 0.02
                        }

                        val speechThreshold = (ambientNoiseFloor + 12.0).coerceIn(35.0, 68.0)

                        if (db >= speechThreshold) {
                            speechFrames++
                            if (speechFrames >= 3) {
                                speechFrames = 0
                                if (now - lastTriggerTimestamp > 3000L && !isTranscribing && !isSelfSpeaking) {
                                    lastTriggerTimestamp = now
                                    mainHandler.post {
                                        startOnDemandTranscription()
                                    }
                                }
                            }
                        } else {
                            if (speechFrames > 0) speechFrames--
                        }
                    }
                }

                try {
                    record.stop()
                    record.release()
                } catch (_: Exception) {}
                audioRecord = null
            } catch (e: Exception) {
                Timber.e(e, "Error in AudioRecord continuous loop")
            }
        }, "AirBeats-Silent-AudioRecord-Thread").apply {
            start()
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
                Timber.e(e, "Failed to create SpeechRecognizer")
                try {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                        setRecognitionListener(this@VoiceAssistantManager)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun startOnDemandTranscription() {
        if (!isRunning) return
        val now = System.currentTimeMillis()
        if (isTtsSpeaking || (now - ttsFinishedTimestamp < 1500L)) return
        ensureRecognizer()

        val recognizer = speechRecognizer ?: return
        if (isTranscribing) return
        isTranscribing = true

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
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 4000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            }

            recognizer.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Timber.e(e, "Error starting on-demand transcription")
            isTranscribing = false
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {
        _isListening.value = true
    }

    override fun onRmsChanged(rmsdB: Float) {
        _audioRms.value = rmsdB
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        isTranscribing = false
        Timber.d("On-demand SpeechRecognizer onError: %d", error)

        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
    }

    override fun onResults(results: Bundle?) {
        isTranscribing = false

        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val topText = matches.first().trim()
            _lastRecognizedText.value = topText
            Timber.i("Spoken command recognized: %s", topText)

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

            // If not matched directly with wake word, check direct command or specific song query
            if (matchedCommand == null || matchedCommand is VoiceCommand.Unknown) {
                val directParsed = VoiceCommandParser.parse(topText, requireWakeWord = false)
                if (directParsed !is VoiceCommand.Unknown) {
                    matchedCommand = directParsed
                } else if (topText.isNotBlank()) {
                    // Fallback to specific song title (e.g. "Starboy")
                    matchedCommand = VoiceCommand.PlaySong(topText)
                }
            }

            matchedCommand?.let { cmd ->
                onWakeWordHeard?.invoke(matchedText)
                onCommandRecognized(cmd, matchedText)
            }
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

    private fun stopContinuousAudioRecord() {
        isAudioRecordRunning = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        audioRecordThread?.interrupt()
        audioRecordThread = null
    }

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
