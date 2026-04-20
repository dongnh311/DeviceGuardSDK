package io.github.dongnh311.deviceguard.remote

import io.github.dongnh311.deviceguard.core.DetectedThreat
import io.github.dongnh311.deviceguard.core.DetectionResult
import io.github.dongnh311.deviceguard.core.Detector
import io.github.dongnh311.deviceguard.core.DeviceGuard
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.ThreatType

private const val DETECTOR_ID = "remotecheck"
private const val THRESHOLD = 0.5f

/**
 * Detector that surfaces installed remote-control apps and active screen capture.
 *
 * Two independent checks run in the same pass:
 * - **Remote control installed** — Android: `PackageManager` scan for known package IDs
 *   (TeamViewer, AnyDesk, QuickSupport, RustDesk, Chrome RD, Splashtop, VNC clients);
 *   Desktop JVM: running-process scan (`ps -A` / `tasklist`) for known binaries.
 * - **Screen being captured** — iOS: `UIScreen.main.isCaptured`; Desktop: presence of a
 *   running `screensharingd` (macOS) / active RDP session (Windows).
 *
 * Neither signal is inherently malicious — IT helpdesk support, accessibility users, and
 * family tech support all legitimately install these. The detector surfaces state; the
 * risk-scoring strategy decides how much it matters.
 *
 * Web returns [DetectionResult.NotApplicable] — browsers cannot enumerate installed apps.
 */
public class RemoteCheckDetector internal constructor(
    private val probe: suspend (DeviceGuardContext) -> RemoteCheckOutcome,
) : Detector<RemoteCheckResult> {
    public constructor() : this(probe = { runRemoteCheck(it) })

    override val id: String = DETECTOR_ID

    override suspend fun detect(context: DeviceGuardContext): DetectionResult<RemoteCheckResult> {
        val outcome = probe(context)
        if (!outcome.applicable) return DetectionResult.NotApplicable(id, outcome.reason)

        val installedConfidence = sumWeights(outcome.installedIndicators)
        val captureConfidence = sumWeights(outcome.captureIndicators)
        val installed = installedConfidence >= THRESHOLD
        val captured = captureConfidence >= THRESHOLD

        val installedNames = outcome.installedIndicators.map { it.name }
        val captureNames = outcome.captureIndicators.map { it.name }

        val threats =
            buildList {
                if (installed) add(threat(ThreatType.RemoteControlInstalled, installedConfidence, installedNames))
                if (captured) add(threat(ThreatType.ScreenBeingCaptured, captureConfidence, captureNames))
            }

        return DetectionResult.Success(
            detectorId = id,
            data =
                RemoteCheckResult(
                    remoteControlInstalled = installed,
                    screenBeingCaptured = captured,
                    installedConfidence = installedConfidence,
                    captureConfidence = captureConfidence,
                    installedIndicators = installedNames,
                    captureIndicators = captureNames,
                ),
            threats = threats,
            signals =
                mapOf(
                    "remotecheck.threshold" to THRESHOLD.toString(),
                    "remotecheck.installed_indicator_count" to outcome.installedIndicators.size.toString(),
                    "remotecheck.capture_indicator_count" to outcome.captureIndicators.size.toString(),
                ),
        )
    }

    private fun sumWeights(indicators: List<RemoteIndicator>): Float = indicators.sumOf { it.weight.toDouble() }.toFloat().coerceIn(0f, 1f)

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

/** Attach a [RemoteCheckDetector] to the builder. */
public fun DeviceGuard.Builder.enableRemoteCheck(): DeviceGuard.Builder = addDetector(RemoteCheckDetector())
