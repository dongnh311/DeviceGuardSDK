package io.github.dongnh311.deviceguard.rootcheck

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

/** Root/jailbreak has no equivalent on desktop JVM. */
internal actual suspend fun runRootCheck(context: DeviceGuardContext): RootCheckOutcome =
    RootCheckOutcome(applicable = false, reason = "Root/jailbreak detection does not apply to JVM/desktop.")
