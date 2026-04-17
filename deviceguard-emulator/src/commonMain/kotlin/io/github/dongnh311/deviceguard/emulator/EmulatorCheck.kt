package io.github.dongnh311.deviceguard.emulator

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

/** A single emulator/debugger signal that fired on the host platform. */
internal data class EmulatorIndicator(
    val name: String,
    val weight: Float,
)

/**
 * Outcome of platform-specific emulator + debugger probing.
 *
 * Two disjoint indicator lists — emulator and debugger — feed two separate confidences in
 * the detector, and yield independent [io.github.dongnh311.deviceguard.core.ThreatType]
 * entries on the final report. Platforms where neither concept applies return
 * `applicable = false`; at the time of writing every target has at least one applicable
 * probe so all actuals return `applicable = true`.
 */
internal data class EmulatorCheckOutcome(
    val applicable: Boolean,
    val reason: String? = null,
    val emulatorIndicators: List<EmulatorIndicator> = emptyList(),
    val debuggerIndicators: List<EmulatorIndicator> = emptyList(),
)

/** Platform entry point. Implementations must never throw — catch and return no indicators. */
internal expect suspend fun runEmulatorCheck(context: DeviceGuardContext): EmulatorCheckOutcome
