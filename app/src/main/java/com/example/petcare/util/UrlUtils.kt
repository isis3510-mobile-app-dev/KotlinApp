package com.example.petcare.util

import com.example.petcare.BuildConfig

object UrlUtils {
    fun resolveUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        
        // Remove "/api/" from the end of BASE_URL to get the root host
        val base = BuildConfig.BASE_URL.removeSuffix("/api/")
        val cleanUrl = if (url.startsWith("/")) url else "/$url"
        
        return "$base$cleanUrl"
    }
}
