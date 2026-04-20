package io.github.dongnh311.deviceguard.surveillance

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

/**
 * iOS app sandbox blocks enumeration of installed apps, running processes, accessibility
 * service holders, and extension states. The detector reports itself as not applicable —
 * consumers should rely on the `Jailbreak` threat (a jailbroken device materially raises
 * the surveillance risk because tweaks bypass the sandbox).
 */
internal actual suspend fun runSurveillanceCheck(context: DeviceGuardContext): SurveillanceOutcome =
    SurveillanceOutcome(
        applicable = false,
        reason = "surveillance detection is not supported on iOS — sandbox blocks cross-app enumeration",
    )
