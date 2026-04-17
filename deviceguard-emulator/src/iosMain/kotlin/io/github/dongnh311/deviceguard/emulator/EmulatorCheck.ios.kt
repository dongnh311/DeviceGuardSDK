package io.github.dongnh311.deviceguard.emulator

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import platform.Foundation.NSProcessInfo

private const val WEIGHT_SIMULATOR_ENV = 1.0f

/**
 * iOS emulator (simulator) + debugger detection.
 *
 * - **Simulator:** `NSProcessInfo.environment` carries `SIMULATOR_DEVICE_NAME` /
 *   `SIMULATOR_MODEL_IDENTIFIER` only when the app is running in the iOS Simulator.
 *   Weight 1.0 — the env var is a deterministic signal from the simulator runtime.
 * - **Debugger:** not yet implemented. `sysctl(KERN_PROC_PID)` with the `P_TRACED` flag
 *   is the standard probe; it requires cinterop struct handling and benefits from
 *   real-device integration tests before shipping.
 */
internal actual suspend fun runEmulatorCheck(context: DeviceGuardContext): EmulatorCheckOutcome {
    val emulator = mutableListOf<EmulatorIndicator>()
    val env = NSProcessInfo.processInfo.environment
    for (key in SIMULATOR_ENV_KEYS) {
        if (env[key] != null) emulator += EmulatorIndicator("simulator_env:$key", WEIGHT_SIMULATOR_ENV)
    }
    return EmulatorCheckOutcome(applicable = true, emulatorIndicators = emulator)
}

private val SIMULATOR_ENV_KEYS =
    listOf(
        "SIMULATOR_DEVICE_NAME",
        "SIMULATOR_MODEL_IDENTIFIER",
        "SIMULATOR_HOST_HOME",
    )
