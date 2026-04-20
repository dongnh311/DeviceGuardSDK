package io.github.dongnh311.deviceguard.surveillance

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.InternalDeviceGuardApi
import io.github.dongnh311.deviceguard.core.scanJvmProcessBasenames

private const val WEIGHT_AUTOMATION_TOOL = 1.0f
private const val WEIGHT_DEBUGGER_PROCESS = 1.0f

/**
 * Desktop JVM surveillance detection. Limited to process-name scanning — the desktop JVM
 * has no unified permission model equivalent to Android's `AccessibilityManager` /
 * `DevicePolicyManager`.
 *
 * Delegates to [scanJvmProcessBasenames] for process enumeration. Automation tools and
 * debuggers share one scan by passing a union of their needle sets, then partitioning
 * the hits by category afterwards.
 */
@OptIn(InternalDeviceGuardApi::class)
internal actual suspend fun runSurveillanceCheck(context: DeviceGuardContext): SurveillanceOutcome {
    val needles = KNOWN_AUTOMATION_PROCESSES.toSet() + KNOWN_DEBUGGERS.toSet()
    val hits = runCatching { scanJvmProcessBasenames(needles) }.getOrDefault(emptySet())

    val indicators =
        hits.map { basename ->
            val category =
                if (basename in KNOWN_AUTOMATION_PROCESSES) {
                    SurveillanceCategory.AutomationToolRunning
                } else {
                    SurveillanceCategory.DebuggerAttachedElsewhere
                }
            val prefix = if (category == SurveillanceCategory.AutomationToolRunning) "automation_process" else "debugger_process"
            val weight = if (category == SurveillanceCategory.AutomationToolRunning) WEIGHT_AUTOMATION_TOOL else WEIGHT_DEBUGGER_PROCESS
            SurveillanceIndicator(category, "$prefix:$basename", weight)
        }

    return SurveillanceOutcome(applicable = true, indicators = indicators)
}
