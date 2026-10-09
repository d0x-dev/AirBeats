package com.darkxvenom.airbeats.playback

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal data class EqualPowerGains(
    val outgoing: Float,
    val incoming: Float,
)

internal fun needsCorrectiveCrossfadeSeek(
    primaryPositionMs: Long,
    secondaryPositionMs: Long,
    maximumDriftMs: Long,
): Boolean {
    require(maximumDriftMs >= 0L)
    return abs(primaryPositionMs - secondaryPositionMs) > maximumDriftMs
}

internal fun hasPlaybackPositionAdvanced(
    positionAfterSeekMs: Long,
    currentPositionMs: Long,
): Boolean = currentPositionMs > positionAfterSeekMs

internal fun equalPowerGains(progress: Float): EqualPowerGains {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val radians = clampedProgress.toDouble() * (PI / 2.0)
    return EqualPowerGains(
        outgoing = cos(radians).toFloat(),
        incoming = sin(radians).toFloat(),
    )
}
