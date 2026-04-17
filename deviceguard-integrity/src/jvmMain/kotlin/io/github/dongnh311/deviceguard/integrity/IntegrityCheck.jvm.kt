package io.github.dongnh311.deviceguard.integrity

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

/** JAR signature verification and class-modification checksums are deferred on desktop JVM. */
internal actual suspend fun runIntegrityCheck(
    context: DeviceGuardContext,
    config: IntegrityCheckConfig,
): IntegrityCheckOutcome =
    IntegrityCheckOutcome(
        applicable = false,
        reason = "Integrity check not yet implemented for desktop JVM — rely on mobile targets.",
    )
