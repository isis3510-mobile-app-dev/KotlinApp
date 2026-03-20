package com.example.petcare.data.analytics

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

/**
 * Measures approximate network speed using [NetworkCapabilities].
 *
 * Returns download/upload speed in kbps. Falls back to 0 if unavailable.
 */
object NetworkSpeedMeasurer {

    private const val TAG = "NetworkSpeedMeasurer"

    data class Speed(val downloadKbps: Int, val uploadKbps: Int)

    /**
     * Get the current link speed reported by the OS.
     * This uses [NetworkCapabilities.getLinkDownstreamBandwidthKbps] and
     * [NetworkCapabilities.getLinkUpstreamBandwidthKbps], which are
     * estimates from the platform (Wi-Fi link speed, carrier capabilities, etc).
     */
    fun measure(context: Context): Speed {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return Speed(0, 0)
            val caps = cm.getNetworkCapabilities(network) ?: return Speed(0, 0)

            Speed(
                downloadKbps = caps.linkDownstreamBandwidthKbps,
                uploadKbps = caps.linkUpstreamBandwidthKbps
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to measure network speed: ${e.message}")
            Speed(0, 0)
        }
    }
}
