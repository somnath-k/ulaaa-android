package com.dotkios.ulaaa.util

/**
 * Normalizes a phone number for matching: digits only, last 10 (drops country code / formatting).
 * Both the stored profile phone and a picked contact number go through this so they compare equal.
 */
fun normalizePhone(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length > 10) digits.takeLast(10) else digits
}
