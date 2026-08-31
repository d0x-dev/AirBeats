package com.darkxvenom.airbeats.voice

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.service.voice.VoiceInteractionSession
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.playback.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber

/**
 * System VoiceInteractionSession for AirBeats.
 * Renders the Assistant bottom sheet and parses & executes voice commands.
 */
class AirBeatsVoiceInteractionSession(context: Context) : VoiceInteractionSession(context), RecognitionListener {

    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var actionExecutor: VoiceAssistantActionExecutor? = null

    private var rootLayout: LinearLayout? = null
    private var statusTextView: TextView? = null
    private var transcriptTextView: TextView? = null
    private var iconView: ImageView? = null

    override fun onCreate() {
        super.onCreate()
        actionExecutor = VoiceAssistantActionExecutor(
            context = context,
            scope = sessionScope,
            getMusicService = { MusicService.instance },
            overlayManager = null
        )
    }

    override fun onCreateContentView(): View {
        val root = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#40000000"))
            setOnClickListener { finish() }
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#1C1C24"))
                cornerRadius = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    24f,
                    context.resources.displayMetrics
                )
                setStroke(
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        1f,
                        context.resources.displayMetrics
                    ).toInt(),
                    Color.parseColor("#33FFFFFF")
                )
            }
            background = bg

            val pad = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                24f,
                context.resources.displayMetrics
            ).toInt()
            setPadding(pad, pad, pad, pad)

            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM
                setMargins(pad, 0, pad, pad * 2)
            }
            layoutParams = params
            setOnClickListener { /* prevent dismiss on card click */ }
        }

        iconView = ImageView(context).apply {
            setImageResource(R.drawable.mic)
            setColorFilter(Color.parseColor("#4285F4"))
            val size = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                40f,
                context.resources.displayMetrics
            ).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                bottomMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    12f,
                    context.resources.displayMetrics
                ).toInt()
            }
        }
        card.addView(iconView)

        statusTextView = TextView(context).apply {
            text = "AirBeats is listening..."
            setTextColor(Color.parseColor("#4285F4"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
        }
        card.addView(statusTextView)

        transcriptTextView = TextView(context).apply {
            text = ""
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }
        card.addView(transcriptTextView)

        root.addView(card)
        rootLayout = card
        return root
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Timber.i("AirBeatsVoiceInteractionSession onShow")

        statusTextView?.text = "AirBeats is listening..."
        transcriptTextView?.text = ""
        iconView?.setImageResource(R.drawable.mic)
        iconView?.setColorFilter(Color.parseColor("#4285F4"))

        startListening()
    }

    override fun onHide() {
        stopListening()
        super.onHide()
    }

    override fun onDestroy() {
        stopListening()
        sessionScope.cancel()
        super.onDestroy()
    }

    private fun startListening() {
        stopListening()
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@AirBeatsVoiceInteractionSession)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start speech recognition in VoiceInteractionSession")
            finish()
        }
    }

    private fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    // --- RecognitionListener ---

    override fun onReadyForSpeech(params: Bundle?) {}

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        statusTextView?.text = "Processing..."
    }

    override fun onError(error: Int) {
        Timber.d("VoiceInteractionSession SpeechRecognizer onError: %d", error)
        statusTextView?.text = "Didn't catch that"
        mainHandler.postDelayed({ finish() }, 1200L)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val topText = matches.first().trim()
            transcriptTextView?.text = "\"$topText\""
            statusTextView?.text = "Executing command..."

            var commandExecuted = false
            for (candidate in matches) {
                val command = VoiceCommandParser.parse(candidate.trim(), requireWakeWord = false)
                if (command !is VoiceCommand.Unknown) {
                    actionExecutor?.execute(command)
                    commandExecuted = true
                    break
                }
            }

            if (!commandExecuted && topText.isNotBlank()) {
                val directCommand = VoiceCommandParser.parse(topText, requireWakeWord = false)
                if (directCommand !is VoiceCommand.Unknown) {
                    actionExecutor?.execute(directCommand)
                } else if (topText.length >= 3) {
                    actionExecutor?.execute(VoiceCommand.PlaySong(topText))
                }
            }

            iconView?.setImageResource(R.drawable.play)
            iconView?.setColorFilter(Color.parseColor("#34A853"))
            statusTextView?.setTextColor(Color.parseColor("#34A853"))
            statusTextView?.text = "Done"

            mainHandler.postDelayed({ finish() }, 1800L)
        } else {
            finish()
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partialText = matches?.firstOrNull()?.trim()
        if (!partialText.isNullOrBlank()) {
            transcriptTextView?.text = "\"$partialText\""
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
