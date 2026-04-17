package io.github.dongnh311.deviceguard.rootcheck

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.ThreatType

/**
 * Outcome of platform-specific root / jailbreak probing.
 *
 * Actuals return `applicable = false` on platforms where the concept doesn't apply
 * (JVM, JS) so the orchestrator emits [io.github.dongnh311.deviceguard.core.DetectionResult.NotApplicable]
 * instead of a zero-confidence result.
 */
internal data class RootCheckOutcome(
    val applicable: Boolean,
    val reason: String? = null,
    val threatType: ThreatType = ThreatType.Root,
    val indicators: List<RootIndicator> = emptyList(),
)

/**
 * A single root/jailbreak signal that fired on the host platform.
 *
 * @property name forensic tag (e.g. `su_binary:/system/bin/su`).
 * @property weight contribution to the aggregated confidence, in `0f..1f`. Strong signals
 *   such as an executable `su` binary or a successful write to `/private` sit near `1f`;
 *   weak corroborating ones (test-keys build tag, presence of `/bin/bash` on iOS) sit near
 *   `0.2f`. Weights are summed and clamped.
 */
internal data class RootIndicator(
    val name: String,
    val weight: Float,
)

/** Platform entry point. Implementations must never throw — catch and return no indicators. */
internal expect suspend fun runRootCheck(context: DeviceGuardContext): RootCheckOutcome
