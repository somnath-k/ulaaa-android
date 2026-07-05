package com.dotkios.ulaaa.util

/**
 * Builds a keyless travel photo URL from keywords via LoremFlickr.
 * Returns a relevant-ish real image; cards fall back to a gradient while it loads or if it fails.
 */
fun destinationImageUrl(keywords: String): String {
    val cleaned = keywords
        .lowercase()
        .filter { it.isLetterOrDigit() || it.isWhitespace() }
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(",")
        .ifBlank { "travel" }
    return "https://loremflickr.com/600/400/$cleaned,travel"
}
