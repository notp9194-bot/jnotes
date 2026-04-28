package com.notp9194bot.jnotes.util

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Tiny helpers for discovering on-device network info.
 *
 * Used by the user app to seed its display name with the device's local
 * IPv4 address on first launch (so the admin sees something useful even
 * when no name has ever been set).
 */
object NetworkUtils {

    /**
     * Returns the first non-loopback IPv4 address found on any active
     * interface, or `"unknown-ip"` if none can be detected (e.g. the
     * device is offline or only has IPv6).
     */
    fun localIpv4(): String {
        return runCatching {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
                ?.hostAddress
        }.getOrNull() ?: "unknown-ip"
    }
}
