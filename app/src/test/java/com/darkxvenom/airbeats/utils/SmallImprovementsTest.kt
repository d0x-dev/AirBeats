package com.darkxvenom.airbeats.utils

import com.darkxvenom.airbeats.extensions.runCatchingNonCancellation
import com.darkxvenom.airbeats.share.ShareUrlSanitizer
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmallImprovementsTest {

    @Test
    fun extractAndSanitizeUrl_stripsTrackingParamsAndPunctuation() {
        val rawShare = "Check out this song: https://music.youtube.com/watch?v=dQw4w9WgXcQ&si=abc123XYZ&feature=share!"
        val sanitized = ShareUrlSanitizer.extractAndSanitizeUrl(rawShare)
        assertEquals("https://music.youtube.com/watch?v=dQw4w9WgXcQ", sanitized)
    }

    @Test
    fun sanitizeUrl_removesAllParamsWhenOnlyTrackingPresent() {
        val url = "https://open.spotify.com/track/4PTG3Z6ehGkBFwjybzWkR8?si=9a8b7c6d&utm_source=copy-link"
        assertEquals("https://open.spotify.com/track/4PTG3Z6ehGkBFwjybzWkR8", ShareUrlSanitizer.sanitizeUrl(url))
    }

    @Test
    fun formatDurationMs_handlesUnsetAndStandardValues() {
        assertEquals("0:00", AudioFormatUtils.formatDurationMs(-1L))
        assertEquals("0:00", AudioFormatUtils.formatDurationMs(Long.MIN_VALUE))
        assertEquals("3:45", AudioFormatUtils.formatDurationMs(225_000L))
        assertEquals("1:02:03", AudioFormatUtils.formatDurationMs(3_723_000L))
    }

    @Test
    fun formatAudioMetadata_formatsBitrateSampleRateAndSize() {
        assertEquals("320 kbps", AudioFormatUtils.formatBitrateBps(320_000))
        assertNull(AudioFormatUtils.formatBitrateBps(0))
        assertEquals("44.1 kHz", AudioFormatUtils.formatSampleRateHz(44_100))
        assertEquals("48 kHz", AudioFormatUtils.formatSampleRateHz(48_000))
        assertEquals("5.0 MB", AudioFormatUtils.formatFileSizeBytes(5L * 1024L * 1024L))
    }

    @Test(expected = CancellationException::class)
    fun runCatchingNonCancellation_rethrowsCancellationException() {
        runCatchingNonCancellation {
            throw CancellationException("Cancelled")
        }
    }

    @Test
    fun runCatchingNonCancellation_catchesStandardExceptions() {
        val result = runCatchingNonCancellation {
            throw IllegalStateException("Expected failure")
        }
        assertTrue(result.isFailure)
    }
}
