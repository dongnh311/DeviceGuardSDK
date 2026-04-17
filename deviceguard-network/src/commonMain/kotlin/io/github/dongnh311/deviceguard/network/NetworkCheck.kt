package io.github.dongnh311.deviceguard.network

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

/** A single VPN or proxy signal that fired on the host platform. */
internal data class NetworkIndicator(
    val name: String,
    val weight: Float,
)

/**
 * Outcome of platform-specific VPN + proxy probing.
 *
 * Two disjoint indicator lists feed two separate confidences in the detector and yield
 * independent `ThreatType.VpnActive` / `ThreatType.ProxyActive` entries on the final
 * report. JS currently returns `applicable = false` — reliable in-browser VPN detection
 * needs server-side correlation.
 */
internal data class NetworkCheckOutcome(
    val applicable: Boolean,
    val reason: String? = null,
    val vpnIndicators: List<NetworkIndicator> = emptyList(),
    val proxyIndicators: List<NetworkIndicator> = emptyList(),
)

/** Platform entry point. Implementations must never throw — catch and return no indicators. */
internal expect suspend fun runNetworkCheck(context: DeviceGuardContext): NetworkCheckOutcome

/** Network-interface name prefixes that indicate a VPN/tunnel is active. */
internal val VPN_INTERFACE_PREFIXES: List<String> =
    listOf(
        "tun",
        "utun",
        "ppp",
        "ipsec",
        "tap",
        "wg", // WireGuard
    )

/** Returns `true` when [name] starts with any known VPN-interface prefix (case-insensitive). */
internal fun isVpnInterface(name: String): Boolean {
    val lower = name.lowercase()
    return VPN_INTERFACE_PREFIXES.any { lower.startsWith(it) }
}
