package io.github.dongnh311.deviceguard.rootcheck

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

/** Root/jailbreak has no equivalent in the browser. */
internal actual suspend fun runRootCheck(context: DeviceGuardContext): RootCheckOutcome =
    RootCheckOutcome(applicable = false, reason = "Root/jailbreak detection does not apply to browsers.")
