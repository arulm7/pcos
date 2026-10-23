package com.simats.pcos

import android.net.Uri

object ImageHelper {
    /**
     * Normalizes an image path or URL received from the backend API.
     * Handles:
     * - null, blank, "null", "undefined"
     * - absolute URLs (http, https, content, file)
     * - relative paths (e.g., "uploads/xyz.jpg", "/uploads/xyz.jpg")
     * - URLs with spaces or special characters
     */
    fun normalizeImageUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        val trimmed = rawUrl.trim()
        if (trimmed.equals("null", ignoreCase = true) ||
            trimmed.equals("undefined", ignoreCase = true) ||
            trimmed == "0"
        ) {
            return null
        }

        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("content://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true)
        ) {
            return sanitizeUrl(trimmed)
        }

        // Relative path: prepend base URL
        val baseUrl = IpV4Connection.getBaseUrl()
        val cleanBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = trimmed.removePrefix("/")
        return sanitizeUrl(cleanBase + cleanPath)
    }

    private fun sanitizeUrl(url: String): String {
        return try {
            // Replace spaces with %20 for valid HTTP request URI if not already encoded
            url.replace(" ", "%20")
        } catch (_: Exception) {
            url
        }
    }
}
