package io.github.dongnh311.deviceguard.integrity

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

/** Subresource Integrity / script tampering checks are deferred on the browser target. */
internal actual suspend fun runIntegrityCheck(
    context: DeviceGuardContext,
    config: IntegrityCheckConfig,
): IntegrityCheckOutcome =
    IntegrityCheckOutcome(
        applicable = false,
        reason = "Integrity check not yet implemented for browsers — use server-side validation.",
    )
