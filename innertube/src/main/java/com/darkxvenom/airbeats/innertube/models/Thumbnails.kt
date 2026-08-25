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
            finalUrl = finalUrl.replace("hqdefault.jpg", "maxresdefault.jpg")
                .replace("mqdefault.jpg", "maxresdefault.jpg")
                .replace("sddefault.jpg", "maxresdefault.jpg")
                .replace("default.jpg", "maxresdefault.jpg")
        }
        if (finalUrl.contains("=w") && finalUrl.contains("-h")) {
            finalUrl = finalUrl.replace(Regex("=w\\d+-h\\d+"), "=w8192-h8192")
        }
        return finalUrl
    }
}

