package io.github.dongnh311.deviceguard.network

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

/**
 * iOS network detection.
 *
 * The canonical VPN probe on iOS is `getifaddrs()` filtered for `utun*`/`ppp*`/`ipsec*`
 * interface names. `getifaddrs` is not exposed by the default Kotlin/Native posix bindings
 * — wiring it requires a custom cinterop `.def` file (or `SCNetworkInterfaceCopyAll` from
 * SystemConfiguration / `NEVPNManager` from NetworkExtension).
 *
 * Rather than ship an unreliable proxy, we return `NotApplicable` here and follow up with a
 * dedicated cinterop in a subsequent patch. The detector will then slot in without
 * touching the public API.
 */
internal actual suspend fun runNetworkCheck(context: DeviceGuardContext): NetworkCheckOutcome =
    NetworkCheckOutcome(
        applicable = false,
        reason = "iOS VPN probe requires a custom cinterop for getifaddrs — wiring deferred.",
    )
