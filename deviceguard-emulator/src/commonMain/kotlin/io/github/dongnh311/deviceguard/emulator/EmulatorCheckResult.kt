package io.github.dongnh311.deviceguard.emulator

import kotlinx.serialization.Serializable

/**
 * Typed payload emitted by [EmulatorCheckDetector] on
 * [io.github.dongnh311.deviceguard.core.DetectionResult.Success.data].
 *
 * @property isEmulator derived from [emulatorConfidence] against the detector's threshold.
 * @property isDebuggerAttached derived from [debuggerConfidence] against the detector's threshold.
 * @property emulatorConfidence clamped to `0f..1f`, aggregating all fired emulator signals.
 * @property debuggerConfidence clamped to `0f..1f`, aggregating all fired debugger signals.
 * @property emulatorIndicators forensic names of the fired emulator signals (no PII).
 * @property debuggerIndicators forensic names of the fired debugger signals (no PII).
 */
@Serializable
public data class EmulatorCheckResult(
    public val isEmulator: Boolean,
    public val isDebuggerAttached: Boolean,
    public val emulatorConfidence: Float,
    public val debuggerConfidence: Float,
    public val emulatorIndicators: List<String>,
    public val debuggerIndicators: List<String>,
) {
    init {
        require(emulatorConfidence in 0f..1f) { "emulatorConfidence must be within 0f..1f" }
        require(debuggerConfidence in 0f..1f) { "debuggerConfidence must be within 0f..1f" }
    }
}
