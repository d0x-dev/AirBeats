package com.darkxvenom.airbeats.voice

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
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
 * Hands-Free Background Voice Assistant Manager.
 * 1. Background: Continuous 100% silent AudioRecord VAD gatekeeper (Zero beeps, Zero audio ducking).
 * 2. On Voice Activity or 'Speak' tap: Activates speech recognition to accurately transcribe commands & song titles.
 * 3. Works 24/7 in the background and locked screen without continuous beeping.
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
    private var lastTriggerTimestamp = 0L

    @Volatile
    private var isTtsSpeaking = false
    private var ttsFinishedTimestamp = 0L

    // Pure Native AudioRecord Streaming Engine
    private var audioRecord: AudioRecord? = null
    private var isAudioRecordRunning = false
    private var audioRecordThread: Thread? = null

    // Single-shot SpeechRecognizer
    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecognizing = false
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
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val DEBOUNCE_COOLDOWN_MS = 2500L
        private const val TTS_SILENCE_GRACE_MS = 1500L
    }

    fun setTtsSpeaking(speaking: Boolean) {
        isTtsSpeaking = speaking
        if (speaking) {
            cancelRecognition()
        } else {
            ttsFinishedTimestamp = System.currentTimeMillis()
        }
    }

    fun start(requireWakeWord: Boolean = false) {
        this.requireWakeWord = requireWakeWord
        if (isRunning) return
        isRunning = true
        _isListening.value = true

        startContinuousAudioRecord()
        Timber.i("VoiceAssistantManager started hands-free background listening")
    }

    fun stop() {
        isRunning = false
        _isListening.value = false
        cancelRecognition()
        stopContinuousAudioRecord()
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
            startSpeechRecognition(isManualTap = true)
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

    private fun startSpeechRecognition(isManualTap: Boolean = false) {
        if (isRecognizing) return
        isRecognizing = true
        _isListening.value = true

        // 1. Temporarily pause AudioRecord so SpeechRecognizer has exclusive microphone access
        stopContinuousAudioRecord()

        if (isManualTap) {
            _lastRecognizedText.value = "Listening..."
            onWakeWordHeard?.invoke("AirBeats")
        }

        mainHandler.postDelayed({
            if (!isRunning || isTtsSpeaking) {
                finishRecognition()
                return@postDelayed
            }

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
                    putExtra("android.speech.extras.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 1500L)
                    putExtra("android.speech.extras.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 1500L)
                }

                speechRecognizer?.startListening(intent)
                Timber.i("SpeechRecognizer started (manualTap=$isManualTap)")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start SpeechRecognizer")
                finishRecognition()
            }
        }, 120L)
    }

    private fun finishRecognition() {
        isRecognizing = false
        _isListening.value = false
        restoreSystemSound()

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Exception) {}

        // Resume silent background AudioRecord monitoring
        if (isRunning && !isAudioRecordRunning) {
            mainHandler.postDelayed({
                if (isRunning && !isRecognizing) {
                    startContinuousAudioRecord()
                }
            }, 300L)
        }
    }

    private fun cancelRecognition() {
        isRecognizing = false
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
        Timber.d("SpeechRecognizer onError: %d", error)
        _isListening.value = false
        finishRecognition()
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

        if (!matches.isNullOrEmpty()) {
            val topText = matches.first().trim()
            _lastRecognizedText.value = topText
            Timber.i("Voice Assistant recognized spoken text: %s", matches.joinToString(" | "))

            var commandExecuted = false
            for (candidate in matches) {
                val command = VoiceCommandParser.parse(candidate.trim(), requireWakeWord = requireWakeWord)
                if (command !is VoiceCommand.Unknown) {
                    onCommandRecognized(command, candidate.trim())
                    commandExecuted = true
                    break
                }
            }

            // Fallback: If wake word was not strictly required or if user spoke a song title
            if (!commandExecuted && !requireWakeWord && topText.isNotBlank()) {
                val directCommand = VoiceCommandParser.parse(topText, requireWakeWord = false)
                if (directCommand !is VoiceCommand.Unknown) {
                    onCommandRecognized(directCommand, topText)
                } else if (topText.length >= 3) {
                    onCommandRecognized(VoiceCommand.PlaySong(topText), topText)
                }
            }
        }

        finishRecognition()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partialText = matches?.firstOrNull()?.trim()
        if (!partialText.isNullOrBlank()) {
            _lastRecognizedText.value = partialText
            onWakeWordHeard?.invoke(partialText)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    // --- Continuous Background Silent AudioRecord Engine ---

    @SuppressLint("MissingPermission")
    private fun startContinuousAudioRecord() {
        if (isAudioRecordRunning || isRecognizing) return
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
                Timber.i("Continuous background AudioRecord active (Hands-free, 0 beeps)")

                val buffer = ShortArray(1024)
                var ambientNoiseFloor = 0.0
                var speechFrames = 0
                var silenceFrames = 0
                var isCollectingSpeech = false
                var speechDurationFrames = 0

                while (isRunning && isAudioRecordRunning && !isRecognizing) {
                    val now = System.currentTimeMillis()
                    val isSelfSpeaking = isTtsSpeaking || (now - ttsFinishedTimestamp < TTS_SILENCE_GRACE_MS)

                    if (isSelfSpeaking) {
                        speechFrames = 0
                        silenceFrames = 0
                        isCollectingSpeech = false
                        speechDurationFrames = 0
                        Thread.sleep(80)
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

                        val speechThreshold = (ambientNoiseFloor + 14.0).coerceIn(40.0, 72.0)

                        if (db >= speechThreshold) {
                            speechFrames++
                            silenceFrames = 0
                            if (speechFrames >= 3 && !isCollectingSpeech) {
                                isCollectingSpeech = true
                                speechDurationFrames = 0
                            }
                            if (isCollectingSpeech) {
                                speechDurationFrames++
                            }
                        } else {
                            if (isCollectingSpeech) {
                                silenceFrames++
                                speechDurationFrames++

                                // End of speech detected (approx 450ms of silence after speech)
                                if (silenceFrames >= 7) {
                                    isCollectingSpeech = false
                                    speechFrames = 0
                                    silenceFrames = 0

                                    val triggerNow = System.currentTimeMillis()
                                    if (speechDurationFrames in 8..200 && (triggerNow - lastTriggerTimestamp > DEBOUNCE_COOLDOWN_MS) && !isSelfSpeaking) {
                                        lastTriggerTimestamp = triggerNow
                                        Timber.d("Voice activity detected in background ($speechDurationFrames frames) -> Starting speech recognition")

                                        mainHandler.post {
                                            startSpeechRecognition(isManualTap = false)
                                        }
                                    }
                                    speechDurationFrames = 0
                                }
                            } else {
                                speechFrames = 0
                            }
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
        }, "AirBeats-Background-Voice-Thread").apply {
            start()
        }
    }

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
            restoreSystemSound()
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
    }
}
