package io.github.dongnh311.deviceguard.surveillance

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

/**
 * Browser JS cannot enumerate installed extensions, other open tabs, or native host
 * processes. The detector reports itself as not applicable — the closest analog
 * (`navigator.webdriver`) is already covered by the emulator-check module.
 */
internal actual suspend fun runSurveillanceCheck(context: DeviceGuardContext): SurveillanceOutcome =
    SurveillanceOutcome(
        applicable = false,
        reason = "surveillance detection is not supported in browser JS — sandbox blocks enumeration",
    )
