package io.github.dongnh311.deviceguard.emulator

import io.github.dongnh311.deviceguard.core.DetectedThreat
import io.github.dongnh311.deviceguard.core.DetectionResult
import io.github.dongnh311.deviceguard.core.Detector
import io.github.dongnh311.deviceguard.core.DeviceGuard
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.ThreatType

private const val DETECTOR_ID = "emulatorcheck"
private const val THRESHOLD = 0.5f

/**
 * Detector that surfaces emulator / virtual-device and attached-debugger signals.
 *
 * Two independent checks run in the same pass: emulator indicators (platform build props,
 * QEMU artefacts, simulator env vars, JDWP presence) and debugger indicators
 * (`Debug.isDebuggerConnected`, `jdwp` agent, `navigator.webdriver`, etc.). Each has its own
 * clamped-to-1 confidence and trips an independent boolean at a threshold of `0.5` — so a
 * debug build attached to an IDE on a real device produces a debugger threat but not an
 * emulator threat, and vice versa.
 *
 * Unlike [io.github.dongnh311.deviceguard.core.DeviceGuardContext]-gated root detection,
 * emulator signals are mostly deterministic (a file exists, an env var is set, a JVM
 * argument is present) at weight 1.0 — there is no meaningful "strict" variant, so the
 * detector exposes a single fixed threshold instead of a strict/lax toggle.
 */
public class EmulatorCheckDetector internal constructor(
    private val probe: suspend (DeviceGuardContext) -> EmulatorCheckOutcome,
) : Detector<EmulatorCheckResult> {
    public constructor() : this(probe = { runEmulatorCheck(it) })

    override val id: String = DETECTOR_ID

    override suspend fun detect(context: DeviceGuardContext): DetectionResult<EmulatorCheckResult> {
        val outcome = probe(context)
        if (!outcome.applicable) return DetectionResult.NotApplicable(id, outcome.reason)

        val emulatorConfidence = sumWeights(outcome.emulatorIndicators)
        val debuggerConfidence = sumWeights(outcome.debuggerIndicators)
        val isEmulator = emulatorConfidence >= THRESHOLD
        val isDebugger = debuggerConfidence >= THRESHOLD

        val emulatorIndicatorNames = outcome.emulatorIndicators.map { it.name }
        val debuggerIndicatorNames = outcome.debuggerIndicators.map { it.name }

        val threats =
            buildList {
                if (isEmulator) add(threat(ThreatType.Emulator, emulatorConfidence, emulatorIndicatorNames))
                if (isDebugger) add(threat(ThreatType.DebuggerAttached, debuggerConfidence, debuggerIndicatorNames))
            }

        return DetectionResult.Success(
            detectorId = id,
            data =
                EmulatorCheckResult(
                    isEmulator = isEmulator,
                    isDebuggerAttached = isDebugger,
                    emulatorConfidence = emulatorConfidence,
                    debuggerConfidence = debuggerConfidence,
                    emulatorIndicators = emulatorIndicatorNames,
                    debuggerIndicators = debuggerIndicatorNames,
                ),
            threats = threats,
            signals =
                mapOf(
                    "emulatorcheck.threshold" to THRESHOLD.toString(),
                    "emulatorcheck.emulator_indicator_count" to outcome.emulatorIndicators.size.toString(),
                    "emulatorcheck.debugger_indicator_count" to outcome.debuggerIndicators.size.toString(),
                ),
        )
    }

    private fun sumWeights(indicators: List<EmulatorIndicator>): Float =
        indicators.sumOf { it.weight.toDouble() }.toFloat().coerceIn(0f, 1f)

    private fun threat(
        type: ThreatType,
        confidence: Float,
        indicators: List<String>,
    ): DetectedThreat =
        DetectedThreat.of(
            threat = type,
            confidence = confidence,
            indicators = indicators,
        )
}

/** Attach an [EmulatorCheckDetector] to the builder. */
public fun DeviceGuard.Builder.enableEmulatorCheck(): DeviceGuard.Builder = addDetector(EmulatorCheckDetector())
