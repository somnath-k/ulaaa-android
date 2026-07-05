package com.dotkios.ulaaa.util

import kotlin.math.abs

private val STOP_WORDS = setOf(
    "in", "the", "of", "a", "an", "to", "for", "and", "at", "on", "with", "your",
    "day", "days", "night", "nights", "trip", "trips", "hour", "hours", "hrs", "h",
)

/**
 * Builds a keyless travel photo URL from keywords via LoremFlickr.
 *
 * Junk words and numbers are stripped so real place/category words remain, and a stable
 * [seed] is turned into a `lock` value so a given card keeps the same image across recompositions
 * instead of flickering through random photos. Cards fall back to a gradient while it loads / fails.
 */
fun destinationImageUrl(keywords: String, seed: String = keywords): String {
    val cleaned = keywords
        .lowercase()
        .split(Regex("[^a-z0-9]+"))
        .filter { it.isNotBlank() && it !in STOP_WORDS && it.none { c -> c.isDigit() } }
        .take(2)
        .joinToString(",")
        .ifBlank { "travel" }
    val lock = abs(seed.hashCode())
    return "https://loremflickr.com/600/400/$cleaned,travel?lock=$lock"
}
