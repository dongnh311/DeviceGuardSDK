package io.github.dongnh311.deviceguard.emulator

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import java.lang.management.ManagementFactory

private const val WEIGHT_JDWP_AGENT = 1.0f
private const val WEIGHT_DALVIK_ART = 0.5f

/**
 * JVM/Desktop emulator + debugger detection.
 *
 * - **Debugger:** scan `RuntimeMXBean.inputArguments` for `-agentlib:jdwp` /
 *   `-Xrunjdwp` / `-Xdebug`. Weight 1.0 — a JDWP agent is an unambiguous debugger signal.
 * - **Emulator (Android-on-PC):** `java.vm.name` ∈ {`Dalvik`, `ART`} only when the JVM
 *   is really an Android runtime. Weight 0.5 — weak because a stock desktop JVM never
 *   reports these, but a BlueStacks-style wrapper might still look like plain HotSpot.
 */
internal actual suspend fun runEmulatorCheck(context: DeviceGuardContext): EmulatorCheckOutcome {
    val emulator = mutableListOf<EmulatorIndicator>()
    val debugger = mutableListOf<EmulatorIndicator>()

    val vmName = System.getProperty("java.vm.name").orEmpty()
    if (vmName.equals("Dalvik", ignoreCase = true) || vmName.equals("ART", ignoreCase = true)) {
        emulator += EmulatorIndicator("vm_name:$vmName", WEIGHT_DALVIK_ART)
    }

    runCatching {
        val args = ManagementFactory.getRuntimeMXBean().inputArguments
        val jdwp =
            args.firstOrNull { arg ->
                arg.contains("-agentlib:jdwp", ignoreCase = true) ||
                    arg.contains("-Xrunjdwp", ignoreCase = true) ||
                    arg.equals("-Xdebug", ignoreCase = true)
            }
        if (jdwp != null) debugger += EmulatorIndicator("jvm_arg:$jdwp", WEIGHT_JDWP_AGENT)
    }

    return EmulatorCheckOutcome(applicable = true, emulatorIndicators = emulator, debuggerIndicators = debugger)
}
