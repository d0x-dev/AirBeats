package com.darkxvenom.airbeats.voice

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.darkxvenom.airbeats.R
import timber.log.Timber

class VoiceAssistantOverlayManager(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: View? = null
    private var isOverlayShowing = false

    private var actionIconView: ImageView? = null
    private var statusTextView: TextView? = null
    private var spokenTextView: TextView? = null
    private var glowBar: View? = null
    private var glowAnimator: ValueAnimator? = null

    private val dismissRunnable = Runnable {
        hide()
    }

    /**
     * Checks if the app has permission to draw overlays over other apps.
     */
    fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Shows the floating bottom pill overlay with the initial listening state.
     */
    fun showListening() {
        if (!canDrawOverlays()) return

        mainHandler.post {
            ensureOverlayCreated()
            actionIconView?.setImageResource(R.drawable.mic)
            actionIconView?.setColorFilter(Color.parseColor("#4285F4"))
            statusTextView?.text = "AirBeats is listening..."
            statusTextView?.setTextColor(Color.parseColor("#4285F4"))
            spokenTextView?.visibility = View.GONE
            spokenTextView?.text = ""

            scheduleAutoDismiss(6000)
        }
    }

    /**
     * Updates the transcribed text in real-time as words are spoken.
     */
    fun updateSpokenText(text: String) {
        if (!canDrawOverlays() || text.isBlank()) return

        mainHandler.post {
            ensureOverlayCreated()
            actionIconView?.setImageResource(R.drawable.graphic_eq)
            actionIconView?.setColorFilter(Color.parseColor("#4285F4"))
            statusTextView?.text = "Listening:"
            statusTextView?.setTextColor(Color.parseColor("#A0A0B0"))
            spokenTextView?.visibility = View.VISIBLE
            spokenTextView?.text = "\"$text\""

            scheduleAutoDismiss(6000)
        }
    }

    /**
     * Displays the action result with a clean vector icon.
     */
    fun showActionResult(message: String, iconResId: Int = R.drawable.music_note) {
        if (!canDrawOverlays()) return

        mainHandler.post {
            ensureOverlayCreated()
            try {
                actionIconView?.setImageResource(iconResId)
                actionIconView?.setColorFilter(Color.parseColor("#34A853"))
            } catch (_: Exception) {}

            statusTextView?.text = "AirBeats"
            statusTextView?.setTextColor(Color.parseColor("#34A853"))
            spokenTextView?.visibility = View.VISIBLE
            spokenTextView?.text = message

            scheduleAutoDismiss(3500)
        }
    }

    private fun scheduleAutoDismiss(delayMs: Long) {
        mainHandler.removeCallbacks(dismissRunnable)
        mainHandler.postDelayed(dismissRunnable, delayMs)
    }

    private fun ensureOverlayCreated() {
        if (overlayView != null && isOverlayShowing) return

        try {
            val root = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Main pill container card
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#F5161622"))
                    cornerRadii = floatArrayOf(
                        dpToPx(28f), dpToPx(28f), // top-left
                        dpToPx(28f), dpToPx(28f), // top-right
                        0f, 0f, 0f, 0f
                    )
                    setStroke(dpToPx(1f).toInt(), Color.parseColor("#304285F4"))
                }
                background = bg
                setPadding(dpToPx(18f).toInt(), dpToPx(10f).toInt(), dpToPx(18f).toInt(), dpToPx(22f).toInt())
                elevation = dpToPx(12f)
            }

            // Top Shimmer Glow Bar
            val glow = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(3.5f).toInt()
                ).apply {
                    bottomMargin = dpToPx(10f).toInt()
                }
                val glowGrad = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(
                        Color.parseColor("#4285F4"), // Blue
                        Color.parseColor("#9B51E0"), // Purple
                        Color.parseColor("#EA4335"), // Red
                        Color.parseColor("#FBBC05"), // Amber
                        Color.parseColor("#4285F4")  // Blue loop
                    )
                ).apply {
                    cornerRadius = dpToPx(2f)
                }
                background = glowGrad
            }
            glowBar = glow
            card.addView(glow)

            // Content Header Row
            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Action Vector Icon with Circular Badge
            val iconFrame = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(38f).toInt(), dpToPx(38f).toInt()).apply {
                    marginEnd = dpToPx(12f).toInt()
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#262638"))
                }
            }

            val iconView = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(22f).toInt(), dpToPx(22f).toInt()).apply {
                    gravity = Gravity.CENTER
                }
                setImageResource(R.drawable.mic)
                setColorFilter(Color.parseColor("#4285F4"))
            }
            actionIconView = iconView
            iconFrame.addView(iconView)
            headerRow.addView(iconFrame)

            // Text column (Status + Spoken/Action Text)
            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // Status label
            val statusTv = TextView(context).apply {
                text = "AirBeats is listening..."
                setTextColor(Color.parseColor("#4285F4"))
                textSize = 12f
                paint.isFakeBoldText = true
            }
            statusTextView = statusTv
            textCol.addView(statusTv)

            // Spoken transcribed / Action text
            val spokenTv = TextView(context).apply {
                setTextColor(Color.WHITE)
                textSize = 15f
                paint.isFakeBoldText = true
                visibility = View.GONE
            }
            spokenTextView = spokenTv
            textCol.addView(spokenTv)

            headerRow.addView(textCol)

            // Close button (X)
            val closeBtn = TextView(context).apply {
                text = "✕"
                setTextColor(Color.parseColor("#707080"))
                textSize = 16f
                setPadding(dpToPx(8f).toInt(), dpToPx(4f).toInt(), dpToPx(8f).toInt(), dpToPx(4f).toInt())
                setOnClickListener { hide() }
            }
            headerRow.addView(closeBtn)

            card.addView(headerRow)
            root.addView(card)

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM
                windowAnimations = android.R.style.Animation_Toast
            }

            windowManager.addView(root, layoutParams)
            overlayView = root
            isOverlayShowing = true

            startGlowAnimation()
        } catch (e: Exception) {
            Timber.e(e, "Error adding voice assistant overlay view")
        }
    }

    private fun startGlowAnimation() {
        glowAnimator?.cancel()
        glowAnimator = ValueAnimator.ofFloat(0.6f, 1.0f).apply {
            duration = 900
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                glowBar?.alpha = anim.animatedValue as Float
            }
            start()
        }
    }

    /**
     * Hides the floating bottom overlay.
     */
    fun hide() {
        mainHandler.removeCallbacks(dismissRunnable)
        mainHandler.post {
            try {
                glowAnimator?.cancel()
                glowAnimator = null

                if (overlayView != null && isOverlayShowing) {
                    windowManager.removeView(overlayView)
                    overlayView = null
                    isOverlayShowing = false
                }
            } catch (e: Exception) {
                Timber.e(e, "Error removing voice assistant overlay view")
            }
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }
}
