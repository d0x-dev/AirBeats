package com.darkxvenom.airbeats.share

import java.net.URI

/**
 * Utility for extracting and normalizing music share URLs by stripping
 * tracking parameters and trailing punctuation.
 */
object ShareUrlSanitizer {

    private val TRACKING_QUERY_KEYS = setOf(
        "si",
        "feature",
        "utm_source",
        "utm_medium",
        "utm_campaign",
        "utm_term",
        "utm_content",
        "igshid",
        "fbclid",
        "gclid",
        "nd",
        "pt",
        "ct",
        "mt"
    )

    private val URL_REGEX = Regex("""https?://[^\s<>"]+""", RegexOption.IGNORE_CASE)

    private val TRAILING_PUNCTUATION = charArrayOf('.', ',', ';', ')', ']', '}', '!', '?', '\'', '"')

    /**
     * Extracts the first valid HTTP/HTTPS URL from a raw shared text payload
     * and strips common tracking query parameters.
     */
    fun extractAndSanitizeUrl(rawText: String?): String? {
        if (rawText.isNullOrBlank()) return null
        val match = URL_REGEX.find(rawText.trim())?.value ?: return null
        val trimmedUrl = match.trimEnd(*TRAILING_PUNCTUATION)
        return sanitizeUrl(trimmedUrl)
    }

    /**
     * Removes known analytics and tracking parameters from [url] while preserving
     * essential media identifiers such as `v`, `list`, and `t`.
     */
    fun sanitizeUrl(url: String): String {
        val cleaned = url.trim().trimEnd(*TRAILING_PUNCTUATION)
        return try {
            val uri = URI(cleaned)
            val rawQuery = uri.rawQuery ?: return cleaned
            val filteredParams = rawQuery
                .split("&")
                .filter { param ->
                    val key = param.substringBefore("=").lowercase()
                    key.isNotBlank() && key !in TRACKING_QUERY_KEYS
                }
            val newQuery = filteredParams.takeIf { it.isNotEmpty() }?.joinToString("&")
            URI(
                uri.scheme,
                uri.rawAuthority,
                uri.rawPath,
                newQuery,
                uri.rawFragment
            ).toString()
        } catch (_: Exception) {
            cleaned
        }
    }
}
