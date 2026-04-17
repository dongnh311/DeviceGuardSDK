package io.github.dongnh311.deviceguard.emulator

import android.os.Build
import android.os.Debug
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import java.io.File

private const val WEIGHT_HARDWARE_EMULATOR = 1.0f
private const val WEIGHT_MANUFACTURER_GENYMOTION = 1.0f
private const val WEIGHT_QEMU_FILE = 1.0f
private const val WEIGHT_FINGERPRINT_GENERIC = 0.7f
private const val WEIGHT_PRODUCT_SDK = 0.7f
private const val WEIGHT_DEBUGGER_CONNECTED = 1.0f
private const val WEIGHT_DEBUGGER_WAITING = 0.6f

/**
 * Android emulator + debugger detection.
 *
 * Emulator signals:
 * - `Build.HARDWARE` ∈ {`goldfish`, `ranchu`} — Android emulator kernels (weight 1.0).
 * - `Build.MANUFACTURER` contains `Genymotion` (weight 1.0).
 * - `Build.FINGERPRINT` starts with `generic` or contains `/sdk_` (weight 0.7).
 * - `Build.PRODUCT` contains `sdk` or `emulator` (weight 0.7).
 * - `/dev/qemu_pipe` or `/dev/socket/qemud` exists (weight 1.0 each).
 *
 * Debugger signals:
 * - `android.os.Debug.isDebuggerConnected()` (weight 1.0).
 * - `android.os.Debug.waitingForDebugger()` (weight 0.6).
 */
internal actual suspend fun runEmulatorCheck(context: DeviceGuardContext): EmulatorCheckOutcome {
    val emulator = mutableListOf<EmulatorIndicator>()
    val debugger = mutableListOf<EmulatorIndicator>()

    val hardware = Build.HARDWARE.orEmpty()
    if (hardware in EMULATOR_HARDWARE) emulator += EmulatorIndicator("hardware:$hardware", WEIGHT_HARDWARE_EMULATOR)

    if (Build.MANUFACTURER.orEmpty().contains("Genymotion", ignoreCase = true)) {
        emulator += EmulatorIndicator("manufacturer:${Build.MANUFACTURER}", WEIGHT_MANUFACTURER_GENYMOTION)
    }

    val fingerprint = Build.FINGERPRINT.orEmpty()
    if (fingerprint.startsWith("generic") || fingerprint.contains("/sdk_")) {
        emulator += EmulatorIndicator("fingerprint:$fingerprint", WEIGHT_FINGERPRINT_GENERIC)
    }

    val product = Build.PRODUCT.orEmpty()
    if (product.contains("sdk") || product.contains("emulator")) {
        emulator += EmulatorIndicator("product:$product", WEIGHT_PRODUCT_SDK)
    }

    for (path in QEMU_FILES) {
        if (File(path).exists()) emulator += EmulatorIndicator("qemu_file:$path", WEIGHT_QEMU_FILE)
    }

    if (Debug.isDebuggerConnected()) debugger += EmulatorIndicator("debug.isDebuggerConnected", WEIGHT_DEBUGGER_CONNECTED)
    if (Debug.waitingForDebugger()) debugger += EmulatorIndicator("debug.waitingForDebugger", WEIGHT_DEBUGGER_WAITING)

    return EmulatorCheckOutcome(applicable = true, emulatorIndicators = emulator, debuggerIndicators = debugger)
}

private val EMULATOR_HARDWARE = setOf("goldfish", "ranchu")

private val QEMU_FILES =
    listOf(
        "/dev/qemu_pipe",
        "/dev/socket/qemud",
    )
