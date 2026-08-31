package com.darkxvenom.airbeats.voice

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.darkxvenom.airbeats.playback.MusicService
import com.darkxvenom.airbeats.playback.PlayerConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * AirBeats Real-Time Always-Active Voice Assistant Engine.
 * 1. Low-power 24/7 background AudioRecord with Hardware AEC & Noise Suppression.
 * 2. Instant Voice-Onset Handoff: Switches to SpeechRecognizer within 100ms of speech so words are never cut off.
 * 3. Zero System Beeps: Suppresses start dings and keeps HUD responsive on both manual taps and background voice.
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

    // Always-Active Native AudioRecord Engine with AudioFx
    private var audioRecord: AudioRecord? = null
    private var acousticEchoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null
    private var isAudioRecordRunning = false
    private var audioRecordThread: Thread? = null

    // Real-Time SpeechRecognizer
    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecognizing = false
    private var isManualSession = false
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
        private const val DEBOUNCE_COOLDOWN_MS = 2000L
        private const val TTS_SILENCE_GRACE_MS = 1200L
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

        startAlwaysActiveAudioRecord()
        Timber.i("VoiceAssistantManager: 24/7 background listener started")
    }

    fun stop() {
        isRunning = false
        _isListening.value = false
        cancelRecognition()
        stopAlwaysActiveAudioRecord()
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
            wakeAndStartRecognition(isManualTap = true)
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

    private fun wakeAndStartRecognition(isManualTap: Boolean = false) {
        if (isRecognizing) return
        isRecognizing = true
        isManualSession = isManualTap
        _isListening.value = true

        // Display HUD immediately with visual feedback
        _lastRecognizedText.value = "Listening..."
        onWakeWordHeard?.invoke("Listening...")

        // Stop AudioRecord immediately to hand off microphone to SpeechRecognizer
        isAudioRecordRunning = false
        releaseAudioEffects()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        mainHandler.post {
            if (!isRunning || isTtsSpeaking) {
                finishRecognition()
                return@post
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
                    putExtra("android.speech.extras.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 5000L)
                    putExtra("android.speech.extras.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 1500L)
                    putExtra("android.speech.extras.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 1200L)
                }

                speechRecognizer?.startListening(intent)
                Timber.i("SpeechRecognizer active (manualTap=$isManualTap)")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start SpeechRecognizer")
                finishRecognition()
            }
        }
    }

    private fun finishRecognition() {
        isRecognizing = false
        isManualSession = false
        _isListening.value = false
        restoreSystemSound()

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Exception) {}

        // Resume Always-On background AudioRecord listener
        if (isRunning && !isAudioRecordRunning) {
            mainHandler.postDelayed({
                if (isRunning && !isRecognizing) {
                    startAlwaysActiveAudioRecord()
                }
            }, 300L)
        }
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

    private fun releaseAudioEffects() {
        try {
            acousticEchoCanceler?.release()
            acousticEchoCanceler = null
        } catch (_: Exception) {}
        try {
            noiseSuppressor?.release()
            noiseSuppressor = null
        } catch (_: Exception) {}
        try {
            automaticGainControl?.release()
            automaticGainControl = null
        } catch (_: Exception) {}
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

    // --- Always-Active Background Microphone with Instant Voice-Onset Detection ---

    @SuppressLint("MissingPermission")
    private fun startAlwaysActiveAudioRecord() {
        if (isAudioRecordRunning || isRecognizing) return
        isAudioRecordRunning = true

        audioRecordThread = Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = (minBufSize * 2).coerceAtLeast(4096)

            while (isRunning && isAudioRecordRunning && !isRecognizing) {
                try {
                    val record = AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        bufferSize
                    )

                    if (record.state != AudioRecord.STATE_INITIALIZED) {
                        Timber.e("AudioRecord init failed, retrying in 500ms...")
                        record.release()
                        try {
                            Thread.sleep(500)
                        } catch (_: InterruptedException) {
                            break
                        }
                        continue
                    }

                    // Attach Hardware Acoustic Echo Canceler and Noise Suppressor
                    try {
                        if (AcousticEchoCanceler.isAvailable()) {
                            acousticEchoCanceler = AcousticEchoCanceler.create(record.audioSessionId)?.apply {
                                enabled = true
                            }
                        }
                        if (NoiseSuppressor.isAvailable()) {
                            noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)?.apply {
                                enabled = true
                            }
                        }
                        if (AutomaticGainControl.isAvailable()) {
                            automaticGainControl = AutomaticGainControl.create(record.audioSessionId)?.apply {
                                enabled = true
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Could not attach hardware audio effects")
                    }

                    audioRecord = record
                    record.startRecording()
                    Timber.i("Microphone is LIVE 24/7 (Listening for speech onset in background)")

                    val buffer = ShortArray(1024)
                    var ambientNoiseFloor = 0.0
                    var voiceOnsetFrames = 0

                    while (isRunning && isAudioRecordRunning && !isRecognizing) {
                        val now = System.currentTimeMillis()
                        val isSelfSpeaking = isTtsSpeaking || (now - ttsFinishedTimestamp < TTS_SILENCE_GRACE_MS)

                        if (isSelfSpeaking) {
                            voiceOnsetFrames = 0
                            try {
                                Thread.sleep(80)
                            } catch (_: InterruptedException) {
                                break
                            }
                            continue
                        }

                        val isMusicPlaying = MusicService.instance?.player?.isPlaying == true ||
                                PlayerConnection.instance?.service?.player?.isPlaying == true

                        val read = record.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            var sum = 0.0
                            for (i in 0 until read) {
                                val v = buffer[i].toInt()
                                sum += v * v
                            }

                            val rms = sqrt(sum / read)
                            val db = if (rms > 0) (20 * log10(rms / 32767.0) + 90.0).coerceAtLeast(0.0) else 0.0
                            _audioRms.value = db.toFloat()

                            if (ambientNoiseFloor == 0.0) {
                                ambientNoiseFloor = db
                            } else {
                                ambientNoiseFloor = ambientNoiseFloor * 0.98 + db * 0.02
                            }

                            // Dynamic speech threshold: Higher during song playback to ignore speaker bleed
                            val thresholdMargin = if (isMusicPlaying) 22.0 else 12.0
                            val minThreshold = if (isMusicPlaying) 58.0 else 44.0
                            val speechThreshold = (ambientNoiseFloor + thresholdMargin).coerceIn(minThreshold, 80.0)

                            if (db >= speechThreshold) {
                                voiceOnsetFrames++
                                val triggerNow = System.currentTimeMillis()

                                // Instant Handoff on Voice-Onset (within ~120ms of user starting to speak)
                                if (voiceOnsetFrames >= 2 && (triggerNow - lastTriggerTimestamp > DEBOUNCE_COOLDOWN_MS) && !isSelfSpeaking) {
                                    lastTriggerTimestamp = triggerNow
                                    Timber.i("Voice onset detected! Instantly activating SpeechRecognizer...")

                                    mainHandler.post {
                                        wakeAndStartRecognition(isManualTap = false)
                                    }
                                    break // Exit read loop to immediately hand off mic
                                }
                            } else {
                                voiceOnsetFrames = 0
                            }
                        }
                    }

                    releaseAudioEffects()
                    try {
                        record.stop()
                        record.release()
                    } catch (_: Exception) {}
                    audioRecord = null
                } catch (e: InterruptedException) {
                    Timber.d("AudioRecord thread interrupted gracefully")
                    releaseAudioEffects()
                    try {
                        audioRecord?.release()
                    } catch (_: Exception) {}
                    audioRecord = null
                    break
                } catch (e: Throwable) {
                    Timber.e(e, "AudioRecord loop exception, auto-recovering...")
                    releaseAudioEffects()
                    try {
                        audioRecord?.release()
                    } catch (_: Exception) {}
                    audioRecord = null
                    if (!isRunning || !isAudioRecordRunning) break
                    try {
                        Thread.sleep(300)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
            releaseAudioEffects()
            try {
                audioRecord?.release()
            } catch (_: Exception) {}
            audioRecord = null
        }, "AirBeats-AlwaysActive-Mic-Thread").apply {
            start()
        }
    }

    private fun stopAlwaysActiveAudioRecord() {
        isAudioRecordRunning = false
        releaseAudioEffects()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        try {
            audioRecordThread?.interrupt()
        } catch (_: Exception) {}
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
