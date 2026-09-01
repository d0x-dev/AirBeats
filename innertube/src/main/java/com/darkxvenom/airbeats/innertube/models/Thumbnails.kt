/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.darkxvenom.airbeats.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Thumbnails(
    val thumbnails: List<Thumbnail>,
)

@Serializable
data class Thumbnail(
    val url: String,
    val width: Int?,
    val height: Int?,
) {
    val normalizedUrl: String get() {
        var finalUrl = if (url.startsWith("//")) "https:$url" else url
        if (finalUrl.contains("i.ytimg.com")) {
            if (finalUrl.endsWith("/default.jpg") || finalUrl.endsWith("/mqdefault.jpg")) {
                finalUrl = finalUrl.substringBeforeLast("/") + "/hqdefault.jpg"
            }
        }
        if (finalUrl.contains("=w") && finalUrl.contains("-h")) {
            finalUrl = finalUrl.replace(Regex("=w\\d+-h\\d+.*"), "=w544-h544-l90-rj")
        } else if (finalUrl.contains(Regex("=s\\d+"))) {
            finalUrl = finalUrl.replace(Regex("=s\\d+.*"), "=s544-l90-rj")
        }
        return finalUrl
    }
}

