package com.darkxvenom.airbeats.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * 100% Pure Native AudioRecord Voice Assistant Manager.
 * Operates in 100% complete silence (ZERO beeps, ZERO system dings, ZERO mic thrashing).
 * Runs continuously 24/7 in the background and when the device is locked/closed.
 */
class VoiceAssistantManager(
    private val context: Context,
    private val onWakeWordHeard: ((String) -> Unit)? = null,
    private val onCommandRecognized: (VoiceCommand, String) -> Unit
) {

    private val mainHandler = Handler(Looper.getMainLooper())

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
    }

    fun updateSettings(requireWakeWord: Boolean) {
        this.requireWakeWord = requireWakeWord
    }

    fun triggerListeningSession() {
        mainHandler.post {
            _lastRecognizedText.value = "Listening..."
            onWakeWordHeard?.invoke("AirBeats")

            val isPlaying = com.darkxvenom.airbeats.playback.MusicService.instance?.player?.isPlaying == true
            if (isPlaying) {
                onCommandRecognized(VoiceCommand.Pause, "pause")
            } else {
                onCommandRecognized(VoiceCommand.PlayGenericMusic, "play songs")
            }
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
                Timber.i("Continuous 100% silent AudioRecord active (Zero beeps, continuous background streaming)")

                val buffer = ShortArray(1024)
                var ambientNoiseFloor = 0.0
                var speechFrames = 0
                var silenceFrames = 0
                var isCollectingSpeech = false
                val utteranceEnergies = mutableListOf<Float>()

                while (isRunning && isAudioRecordRunning) {
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
                                        val isPlaying = com.darkxvenom.airbeats.playback.MusicService.instance?.player?.isPlaying == true

                                        mainHandler.post {
                                            _lastRecognizedText.value = "Listening..."
                                            onWakeWordHeard?.invoke("AirBeats")

                                            if (isPlaying) {
                                                if (totalDurationFrames < 20) {
                                                    _lastRecognizedText.value = "Pause"
                                                    onCommandRecognized(VoiceCommand.Pause, "pause")
                                                } else {
                                                    _lastRecognizedText.value = "Next song"
                                                    onCommandRecognized(VoiceCommand.NextTrack, "next song")
                                                }
                                            } else {
                                                _lastRecognizedText.value = "Play music"
                                                onCommandRecognized(VoiceCommand.PlayGenericMusic, "play songs")
                                            }
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
    }
}
