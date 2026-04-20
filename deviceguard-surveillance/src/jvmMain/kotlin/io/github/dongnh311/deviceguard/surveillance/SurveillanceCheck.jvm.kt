package io.github.dongnh311.deviceguard.surveillance

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import java.util.Locale

private const val WEIGHT_AUTOMATION_TOOL = 1.0f
private const val WEIGHT_DEBUGGER_PROCESS = 1.0f

/**
 * Desktop JVM surveillance detection. Limited to process-name scanning because the
 * desktop JVM has no unified permission model equivalent to Android.
 *
 * Signals:
 * - A known automation / macro process is running (AutoHotkey, Hammerspoon, Keyboard
 *   Maestro, BetterTouchTool, Karabiner, xdotool). Weight 1.0 per match.
 * - A known debugger / instrumentation process is running (gdb, lldb, x64dbg, Frida,
 *   Cheat Engine, IDA). Weight 1.0 per match. Cannot tell which process is being
 *   debugged from a sandboxed JVM — the presence of the tool is the signal.
 */
internal actual suspend fun runSurveillanceCheck(context: DeviceGuardContext): SurveillanceOutcome {
    val hits = runCatching { scanProcesses() }.getOrDefault(emptyList())
    return SurveillanceOutcome(applicable = true, indicators = hits)
}

private fun scanProcesses(): List<SurveillanceIndicator> {
    val hits = mutableListOf<SurveillanceIndicator>()
    ProcessHandle.allProcesses().use { stream ->
        stream.forEach { handle ->
            val command = handle.info().command().orElse(null) ?: return@forEach
            val basename =
                command
                    .substringAfterLast('/')
                    .substringAfterLast('\\')
                    .removeSuffix(".exe")
                    .lowercase(Locale.US)
            when {
                basename in KNOWN_AUTOMATION_PROCESSES ->
                    hits +=
                        SurveillanceIndicator(
                            SurveillanceCategory.AutomationToolRunning,
                            "automation_process:$basename",
                            WEIGHT_AUTOMATION_TOOL,
                        )
                basename in KNOWN_DEBUGGERS ->
                    hits +=
                        SurveillanceIndicator(
                            SurveillanceCategory.DebuggerAttachedElsewhere,
                            "debugger_process:$basename",
                            WEIGHT_DEBUGGER_PROCESS,
                        )
            }
        }
    }
    return hits
}
