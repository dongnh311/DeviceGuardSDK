package io.github.dongnh311.deviceguard.network

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

/** Reliable in-browser VPN/proxy detection needs server-side correlation. */
internal actual suspend fun runNetworkCheck(context: DeviceGuardContext): NetworkCheckOutcome =
    NetworkCheckOutcome(
        applicable = false,
        reason = "VPN/proxy detection from the browser is unreliable — correlate the request's source IP server-side.",
    )
