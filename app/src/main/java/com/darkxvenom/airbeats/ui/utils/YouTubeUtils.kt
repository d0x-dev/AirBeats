package com.darkxvenom.airbeats.ui.utils

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    val w = width ?: height ?: 544
    val h = height ?: width ?: 544

    if (this.contains("googleusercontent.com") || this.contains("ggpht.com")) {
        if (this.contains(Regex("=w\\d+-h\\d+"))) {
            return this.replace(Regex("=w\\d+-h\\d+.*"), "=w$w-h$h-l90-rj")
        } else if (this.contains(Regex("=s\\d+"))) {
            return this.replace(Regex("=s\\d+.*"), "=s$w")
        }
    }

    if (this.contains("ytimg.com")) {
        if (this.endsWith("/default.jpg") || this.endsWith("/hqdefault.jpg") || this.endsWith("/mqdefault.jpg") || this.endsWith("/sddefault.jpg")) {
            return this.substringBeforeLast("/") + "/maxresdefault.jpg"
        }
    }

    return this
}

fun String.highQualityThumbnail(): String =
    resize(8192, 8192)

