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
 * Google Gemini-style Always-Active Voice Assistant Engine with Hardware Acoustic Echo Cancellation.
 * 1. Continuous Always-On Mic with AEC & Noise Suppression to eliminate speaker music bleed.
 * 2. Adaptive threshold during music playback so playing songs never trigger false listening.
 * 3. Zero System Beeps & Zero Phantom HUD Popups: HUD only appears on explicit tap or verified speech.
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

    // Always-Active Native AudioRecord Streaming Engine with AudioFx
    private var audioRecord: AudioRecord? = null
    private var acousticEchoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null
    private var isAudioRecordRunning = false
    private var audioRecordThread: Thread? = null

    // On-Demand SpeechRecognizer for full command parsing
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
        private const val DEBOUNCE_COOLDOWN_MS = 3000L
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

        startAlwaysActiveAudioRecord()
        Timber.i("VoiceAssistantManager: Always-Active Background Microphone running with AEC")
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

        // ONLY pop up HUD immediately if user explicitly pressed 'Speak'
        if (isManualTap) {
            _lastRecognizedText.value = "Listening..."
            onWakeWordHeard?.invoke("AirBeats")
        }

        // Temporarily pause AudioRecord to hand off mic to SpeechRecognizer
        isAudioRecordRunning = false
        releaseAudioEffects()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

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
                Timber.i("SpeechRecognizer active (manualTap=$isManualTap)")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start SpeechRecognizer")
                finishRecognition()
            }
        }, 100L)
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

        // Seamlessly resume Always-On background AudioRecord hotword listener
        if (isRunning && !isAudioRecordRunning) {
            mainHandler.postDelayed({
                if (isRunning && !isRecognizing) {
                    startAlwaysActiveAudioRecord()
                }
            }, 250L)
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
            // Only update HUD if it is an explicit user session or real words are streaming
            if (isManualSession || partialText.length >= 4) {
                onWakeWordHeard?.invoke(partialText)
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    // --- Always-Active Background Microphone & Hotword Detector with Echo Cancellation ---

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
                        Thread.sleep(500)
                        continue
                    }

                    // Attach Hardware Acoustic Echo Canceler and Noise Suppressor
                    try {
                        if (AcousticEchoCanceler.isAvailable()) {
                            acousticEchoCanceler = AcousticEchoCanceler.create(record.audioSessionId)?.apply {
                                enabled = true
                            }
                            Timber.d("Hardware AcousticEchoCanceler attached successfully")
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
                    Timber.i("Microphone is LIVE 24/7 (AEC Active, Listening in background)")

                    val buffer = ShortArray(1024)
                    var ambientNoiseFloor = 0.0
                    var speechFrames = 0
                    var silenceFrames = 0
                    var isCollectingUtterance = false
                    var utteranceFrameCount = 0

                    while (isRunning && isAudioRecordRunning && !isRecognizing) {
                        val now = System.currentTimeMillis()
                        val isSelfSpeaking = isTtsSpeaking || (now - ttsFinishedTimestamp < TTS_SILENCE_GRACE_MS)

                        if (isSelfSpeaking) {
                            speechFrames = 0
                            silenceFrames = 0
                            isCollectingUtterance = false
                            utteranceFrameCount = 0
                            Thread.sleep(80)
                            continue
                        }

                        val isMusicPlaying = MusicService.instance?.player?.isPlaying == true ||
                                PlayerConnection.instance?.service?.player?.isPlaying == true

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

                            // Dynamic speech threshold: Significantly higher during active song playback to reject speaker bleed
                            val thresholdMargin = if (isMusicPlaying) 26.0 else 16.0
                            val minThreshold = if (isMusicPlaying) 60.0 else 46.0
                            val speechThreshold = (ambientNoiseFloor + thresholdMargin).coerceIn(minThreshold, 82.0)

                            if (db >= speechThreshold) {
                                speechFrames++
                                silenceFrames = 0
                                if (speechFrames >= 3 && !isCollectingUtterance) {
                                    isCollectingUtterance = true
                                    utteranceFrameCount = 0
                                }
                                if (isCollectingUtterance) {
                                    utteranceFrameCount++
                                }
                            } else {
                                if (isCollectingUtterance) {
                                    silenceFrames++
                                    utteranceFrameCount++

                                    // Spoken phrase completed (approx 400ms of silence after speaking)
                                    if (silenceFrames >= 6) {
                                        isCollectingUtterance = false
                                        speechFrames = 0
                                        silenceFrames = 0

                                        val triggerNow = System.currentTimeMillis()
                                        // Utterance matches genuine spoken command duration (400ms to 2.5s)
                                        if (utteranceFrameCount in 8..160 && (triggerNow - lastTriggerTimestamp > DEBOUNCE_COOLDOWN_MS) && !isSelfSpeaking) {
                                            lastTriggerTimestamp = triggerNow
                                            Timber.i("Voice phrase detected in background -> Processing speech silently")

                                            mainHandler.post {
                                                wakeAndStartRecognition(isManualTap = false)
                                            }
                                            break // Exit read loop to hand off mic
                                        }
                                        utteranceFrameCount = 0
                                    }
                                } else {
                                    speechFrames = 0
                                }
                            }
                        }
                    }

                    releaseAudioEffects()
                    try {
                        record.stop()
                        record.release()
                    } catch (_: Exception) {}
                    audioRecord = null
                } catch (e: Exception) {
                    Timber.e(e, "AudioRecord loop exception, auto-recovering...")
                    releaseAudioEffects()
                    try {
                        audioRecord?.release()
                    } catch (_: Exception) {}
                    audioRecord = null
                    Thread.sleep(300)
                }
            }
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
