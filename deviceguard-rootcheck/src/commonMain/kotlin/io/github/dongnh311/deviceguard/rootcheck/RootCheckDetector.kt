package io.github.dongnh311.deviceguard.rootcheck

import io.github.dongnh311.deviceguard.core.DetectedThreat
import io.github.dongnh311.deviceguard.core.DetectionResult
import io.github.dongnh311.deviceguard.core.Detector
import io.github.dongnh311.deviceguard.core.DeviceGuard
import io.github.dongnh311.deviceguard.core.DeviceGuardContext

private const val DETECTOR_ID = "rootcheck"
private const val LAX_THRESHOLD = 0.5f
private const val STRICT_THRESHOLD = 0.2f

/**
 * Detector that surfaces root (Android) or jailbreak (iOS) on the host device.
 *
 * Each platform probe contributes a list of [RootIndicator]s with per-signal weights. The
 * detector sums the weights, clamps to `0f..1f`, and compares the aggregate against a
 * threshold that depends on [strict]:
 *
 * - `strict = false` (default): `isRooted = confidence ≥ 0.5` — a single strong signal
 *   (weight 1.0, e.g. a sandbox-escape write) is enough; isolated weak signals are not.
 * - `strict = true`: `isRooted = confidence ≥ 0.2` — any single moderate signal trips.
 *
 * JVM and JS return [DetectionResult.NotApplicable].
 */
public class RootCheckDetector internal constructor(
    private val strict: Boolean,
    private val probe: suspend (DeviceGuardContext) -> RootCheckOutcome,
) : Detector<RootCheckResult> {
    public constructor(strict: Boolean = false) : this(strict, probe = { runRootCheck(it) })

    override val id: String = DETECTOR_ID

    override suspend fun detect(context: DeviceGuardContext): DetectionResult<RootCheckResult> {
        val outcome = probe(context)
        if (!outcome.applicable) return DetectionResult.NotApplicable(id, outcome.reason)

        val confidence =
            outcome.indicators
                .sumOf { it.weight.toDouble() }
                .toFloat()
                .coerceIn(0f, 1f)
        val threshold = if (strict) STRICT_THRESHOLD else LAX_THRESHOLD
        val isRooted = confidence >= threshold
        val indicatorNames = outcome.indicators.map { it.name }

        val threats =
            if (isRooted) {
                listOf(
                    DetectedThreat.of(
                        threat = outcome.threatType,
                        confidence = confidence,
                        indicators = indicatorNames,
                    ),
                )
            } else {
                emptyList()
            }

        return DetectionResult.Success(
            detectorId = id,
            data = RootCheckResult(isRooted = isRooted, confidence = confidence, indicators = indicatorNames),
            threats = threats,
            signals =
                mapOf(
                    "rootcheck.threat_type" to outcome.threatType.id,
                    "rootcheck.strict" to strict.toString(),
                    "rootcheck.threshold" to threshold.toString(),
                    "rootcheck.indicator_count" to outcome.indicators.size.toString(),
                ),
        )
    }
}

/** Attach a [RootCheckDetector] to the builder. Pass `strict = true` to lower the threshold. */
public fun DeviceGuard.Builder.enableRootCheck(strict: Boolean = false): DeviceGuard.Builder = addDetector(RootCheckDetector(strict))
