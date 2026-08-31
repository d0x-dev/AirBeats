package com.darkxvenom.airbeats.voice

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
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
 * Pure-Silent Non-Intrusive Voice Assistant Engine for AirBeats.
 * 1. Single continuous silent AudioRecord background monitor (ZERO beeps, NO volume modifications).
 * 2. On-demand speech session on voice activity or 'Speak' button tap.
 * 3. Shows HUD pill with live streaming text and executes music actions instantly.
 */
class VoiceAssistantManager(
    private val context: Context,
    private val onWakeWordHeard: ((String) -> Unit)? = null,
    private val onCommandRecognized: (VoiceCommand, String) -> Unit
) : RecognitionListener {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var isRunning = false
    private var requireWakeWord = false
    private var isRecognizing = false
    private var lastTriggerTimestamp = 0L

    @Volatile
    private var isTtsSpeaking = false
    private var ttsFinishedTimestamp = 0L

    // 100% Silent Background AudioRecord Engine (Never alters system volumes, never beeps)
    private var audioRecord: AudioRecord? = null
    private var acousticEchoCanceler: AcousticEchoCanceler? = null
    private var automaticGainControl: AutomaticGainControl? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var isAudioRecordRunning = false
    private var audioRecordThread: Thread? = null

    // On-Demand Speech Recognizer
    private var speechRecognizer: SpeechRecognizer? = null

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
        private const val DEBOUNCE_COOLDOWN_MS = 600L
        private const val TTS_SILENCE_GRACE_MS = 800L
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

        startSilentAudioRecord()
        Timber.i("VoiceAssistantManager: Silent background monitor started (requireWakeWord=%b)", requireWakeWord)
    }

    fun stop() {
        isRunning = false
        _isListening.value = false
        cancelRecognition()
        stopSilentAudioRecord()
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
            _lastRecognizedText.value = "Listening..."
            onWakeWordHeard?.invoke("Listening...")
            startSpeechSession(isManualTap = true)
        }
    }

    private fun startSpeechSession(isManualTap: Boolean = false) {
        if (!isRunning || isTtsSpeaking || isRecognizing) return
        isRecognizing = true
        _isListening.value = true

        // Pause silent AudioRecord while SpeechRecognizer has the mic
        isAudioRecordRunning = false
        releaseAudioEffects()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        // Safe 60ms delay to let OEM audio HAL driver release hardware mic cleanly
        mainHandler.postDelayed({
            if (!isRunning || isTtsSpeaking) {
                finishSpeechSession()
                return@postDelayed
            }
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
                    ) {
                        try {
                            SpeechRecognizer.createOnDeviceSpeechRecognizer(context.applicationContext)
                        } catch (_: Throwable) {
                            SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                        }
                    } else {
                        SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                    }.apply {
                        setRecognitionListener(this@VoiceAssistantManager)
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                    putExtra("android.speech.extras.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 7000L)
                    putExtra("android.speech.extras.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 2500L)
                    putExtra("android.speech.extras.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 2000L)
                }

                speechRecognizer?.startListening(intent)
                Timber.i("SpeechRecognizer active (manualTap=%b)", isManualTap)
            } catch (e: Exception) {
                Timber.e(e, "Failed to start SpeechRecognizer session")
                finishSpeechSession()
            }
        }, 60L)
    }

    private fun finishSpeechSession() {
        isRecognizing = false
        _isListening.value = false

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Exception) {}

        // Resume silent background AudioRecord monitoring
        if (isRunning && !isAudioRecordRunning) {
            mainHandler.postDelayed({
                if (isRunning && !isRecognizing) {
                    startSilentAudioRecord()
                }
            }, 300L)
        }
    }

    private fun cancelRecognition() {
        isRecognizing = false
        _isListening.value = false
        mainHandler.post {
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
            automaticGainControl?.release()
            automaticGainControl = null
        } catch (_: Exception) {}
        try {
            noiseSuppressor?.release()
            noiseSuppressor = null
        } catch (_: Exception) {}
    }

    // --- RecognitionListener Callbacks ---

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
        Timber.d("SpeechRecognizer onError: %d", error)
        _isListening.value = false
        finishSpeechSession()
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
                    mainHandler.postDelayed({
                        if (isRunning) {
                            startSpeechSession(isManualTap = true)
                        }
                    }, 200L)
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

        finishSpeechSession()
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

    // --- 100% Silent Continuous AudioRecord Background Engine ---

    @SuppressLint("MissingPermission")
    private fun startSilentAudioRecord() {
        if (isAudioRecordRunning || isRecognizing) return
        isAudioRecordRunning = true

        audioRecordThread = Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = (minBufSize * 2).coerceAtLeast(4096)

            while (isRunning && isAudioRecordRunning && !isRecognizing) {
                try {
                    val record = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
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

                    // Attach Hardware Acoustic Echo Canceler and AGC
                    try {
                        if (AcousticEchoCanceler.isAvailable()) {
                            acousticEchoCanceler = AcousticEchoCanceler.create(record.audioSessionId)?.apply {
                                enabled = true
                            }
                        }
                        if (AutomaticGainControl.isAvailable()) {
                            automaticGainControl = AutomaticGainControl.create(record.audioSessionId)?.apply {
                                enabled = true
                            }
                        }
                        if (NoiseSuppressor.isAvailable()) {
                            noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)?.apply {
                                enabled = true
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Could not attach hardware audio effects")
                    }

                    audioRecord = record
                    record.startRecording()
                    Timber.i("Silent background microphone active (0 beeps, 0 volume changes)")

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
                                ambientNoiseFloor = ambientNoiseFloor * 0.96 + db * 0.04
                            }

                            val margin = if (isMusicPlaying) 8.0 else 3.5
                            val minDb = if (isMusicPlaying) 42.0 else 18.0
                            val speechThreshold = (ambientNoiseFloor + margin).coerceIn(minDb, 68.0)

                            if (db >= speechThreshold) {
                                voiceOnsetFrames++
                                val triggerNow = System.currentTimeMillis()

                                if (voiceOnsetFrames >= 1 && (triggerNow - lastTriggerTimestamp > DEBOUNCE_COOLDOWN_MS) && !isSelfSpeaking) {
                                    lastTriggerTimestamp = triggerNow
                                    Timber.i("Voice onset detected (db=%.1f) -> Starting recognition session...", db)

                                    mainHandler.post {
                                        _lastRecognizedText.value = "Listening..."
                                        onWakeWordHeard?.invoke("Listening...")
                                        startSpeechSession(isManualTap = false)
                                    }
                                    break
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
        }, "AirBeats-Silent-AudioRecord-Thread").apply {
            start()
        }
    }

    private fun stopSilentAudioRecord() {
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
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
    }
}
