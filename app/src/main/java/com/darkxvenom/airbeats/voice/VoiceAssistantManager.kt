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
 * High-Accuracy Hybrid Voice Assistant Manager.
 * 1. Background: 100% Pure Silent AudioRecord VAD gatekeeper (Zero beeps, Zero audio ducking).
 * 2. On-Demand: Single-shot, silent SpeechRecognizer for precise transcription of song titles and commands.
 */
class VoiceAssistantManager(
    private val context: Context,
    private val onWakeWordHeard: ((String) -> Unit)? = null,
    private val onCommandRecognized: (VoiceCommand, String) -> Unit
) : RecognitionListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var isRunning = false
    private var requireWakeWord = true
    private var lastTriggerTimestamp = 0L

    @Volatile
    private var isTtsSpeaking = false
    private var ttsFinishedTimestamp = 0L

    // Pure Native AudioRecord Streaming Engine
    private var audioRecord: AudioRecord? = null
    private var isAudioRecordRunning = false
    private var audioRecordThread: Thread? = null

    // Single-shot On-Demand SpeechRecognizer
    private var singleShotRecognizer: SpeechRecognizer? = null
    private var isSingleShotActive = false
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
        private const val DEBOUNCE_COOLDOWN_MS = 3500L
        private const val TTS_SILENCE_GRACE_MS = 1500L
    }

    fun setTtsSpeaking(speaking: Boolean) {
        isTtsSpeaking = speaking
        if (speaking) {
            cancelSingleShotRecognizer()
        } else {
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
        cancelSingleShotRecognizer()
        stopContinuousAudioRecord()
    }

    fun updateSettings(requireWakeWord: Boolean) {
        this.requireWakeWord = requireWakeWord
    }

    /**
     * Triggered when user taps 'Speak' in notification or UI HUD.
     * Starts a dedicated, single-shot silent speech recognition session to transcribe speech.
     */
    fun triggerListeningSession() {
        mainHandler.post {
            val now = System.currentTimeMillis()
            if (isTtsSpeaking || (now - ttsFinishedTimestamp < TTS_SILENCE_GRACE_MS)) {
                return@post
            }
            startSingleShotRecognition(requireWake = false)
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

    private fun startSingleShotRecognition(requireWake: Boolean = false) {
        if (isSingleShotActive) return
        isSingleShotActive = true

        // 1. Temporarily pause AudioRecord so SpeechRecognizer has exclusive microphone access
        stopContinuousAudioRecord()

        _lastRecognizedText.value = "Listening..."
        onWakeWordHeard?.invoke("AirBeats")

        try {
            muteSystemSound()

            if (singleShotRecognizer == null) {
                singleShotRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                    setRecognitionListener(this@VoiceAssistantManager)
                }
            }

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
                putExtra("android.speech.extras.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 8000L)
                putExtra("android.speech.extras.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 1500L)
                putExtra("android.speech.extras.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 1500L)
            }

            singleShotRecognizer?.startListening(intent)
            Timber.i("Single-shot SpeechRecognizer started in silence (transcribing speech on-demand)")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start single-shot SpeechRecognizer")
            finishSingleShotRecognition()
        }
    }

    private fun finishSingleShotRecognition() {
        isSingleShotActive = false
        restoreSystemSound()

        try {
            singleShotRecognizer?.stopListening()
            singleShotRecognizer?.cancel()
        } catch (_: Exception) {}

        // Resume silent background AudioRecord monitoring
        if (isRunning && !isAudioRecordRunning) {
            mainHandler.postDelayed({
                if (isRunning && !isSingleShotActive) {
                    startContinuousAudioRecord()
                }
            }, 500L)
        }
    }

    private fun cancelSingleShotRecognizer() {
        isSingleShotActive = false
        mainHandler.post {
            restoreSystemSound()
            try {
                singleShotRecognizer?.cancel()
                singleShotRecognizer?.destroy()
            } catch (_: Exception) {}
            singleShotRecognizer = null
        }
    }

    // --- RecognitionListener Callbacks (For Single-Shot Session) ---

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

    override fun onEndOfSpeech() {
        _isListening.value = false
    }

    override fun onError(error: Int) {
        Timber.d("Single-shot SpeechRecognizer onError: %d", error)
        _isListening.value = false
        finishSingleShotRecognition()
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

        if (!matches.isNullOrEmpty()) {
            val topText = matches.first().trim()
            _lastRecognizedText.value = topText
            Timber.i("Voice Assistant recognized: %s", matches.joinToString(" | "))

            for (candidate in matches) {
                val command = VoiceCommandParser.parse(candidate.trim(), requireWakeWord = false)
                if (command !is VoiceCommand.Unknown) {
                    onCommandRecognized(command, candidate.trim())
                    break
                }
            }
        }

        finishSingleShotRecognition()
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
        if (isAudioRecordRunning || isSingleShotActive) return
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
                Timber.i("Continuous silent AudioRecord monitoring active (0 beeps)")

                val buffer = ShortArray(1024)
                var ambientNoiseFloor = 0.0
                var speechFrames = 0
                var silenceFrames = 0
                var isCollectingSpeech = false
                val utteranceEnergies = mutableListOf<Float>()

                while (isRunning && isAudioRecordRunning && !isSingleShotActive) {
                    val now = System.currentTimeMillis()
                    val isSelfSpeaking = isTtsSpeaking || (now - ttsFinishedTimestamp < TTS_SILENCE_GRACE_MS)

                    if (isSelfSpeaking) {
                        speechFrames = 0
                        silenceFrames = 0
                        isCollectingSpeech = false
                        utteranceEnergies.clear()
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

                        val speechThreshold = (ambientNoiseFloor + 12.0).coerceIn(35.0, 68.0)

                        if (db >= speechThreshold) {
                            speechFrames++
                            silenceFrames = 0
                            if (speechFrames >= 2 && !isCollectingSpeech) {
                                isCollectingSpeech = true
                                utteranceEnergies.clear()
                            }
                            if (isCollectingSpeech) {
                                utteranceEnergies.add(db.toFloat())
                            }
                        } else {
                            if (isCollectingSpeech) {
                                silenceFrames++
                                utteranceEnergies.add(db.toFloat())

                                // End of speech detected (approx 600ms of silence after speech)
                                if (silenceFrames >= 10) {
                                    isCollectingSpeech = false
                                    speechFrames = 0
                                    silenceFrames = 0

                                    val totalDurationFrames = utteranceEnergies.size
                                    val triggerNow = System.currentTimeMillis()

                                    if (totalDurationFrames in 6..220 && (triggerNow - lastTriggerTimestamp > DEBOUNCE_COOLDOWN_MS) && !isSelfSpeaking) {
                                        lastTriggerTimestamp = triggerNow

                                        // Launch speech transcription
                                        mainHandler.post {
                                            startSingleShotRecognition(requireWake = requireWakeWord)
                                        }
                                    }
                                    utteranceEnergies.clear()
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
        }, "AirBeats-Silent-AudioRecord-Thread").apply {
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
                singleShotRecognizer?.destroy()
            } catch (_: Exception) {}
            singleShotRecognizer = null
        }
    }
}
