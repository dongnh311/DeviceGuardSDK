package io.github.dongnh311.deviceguard.integrity

import io.github.dongnh311.deviceguard.core.DetectedThreat
import io.github.dongnh311.deviceguard.core.DetectionResult
import io.github.dongnh311.deviceguard.core.Detector
import io.github.dongnh311.deviceguard.core.DeviceGuard
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.ThreatType

private const val DETECTOR_ID = "integritycheck"
private const val THRESHOLD = 0.5f

/**
 * Detector that surfaces app-tampering and dynamic-instrumentation signals.
 *
 * Two independent checks run in the same pass:
 *
 * - **Signature integrity.** When [expectedSignature] is provided, the Android probe
 *   compares the running app's signing certificate SHA-256 against it and emits a strong
 *   mismatch indicator on divergence. Without an expected value, the probe falls back to
 *   detecting a debug certificate or an untrusted installer. Fires
 *   `ThreatType.SignatureMismatch` when the aggregate crosses 0.5. Consumers must read
 *   [IntegrityCheckResult.signatureCheckRun] before treating
 *   `signatureMismatch == false` as "signature is valid" — on platforms that skip the
 *   signature stream the field is also `false`.
 * - **Hook framework detection.** Xposed packages on Android and Frida artefacts on iOS.
 *   Fires `ThreatType.HookFramework` when the aggregate crosses 0.5.
 *
 * JVM and JS return [DetectionResult.NotApplicable] — desktop JAR verification and browser
 * SRI are structurally different from mobile app integrity and are deferred.
 */
public class IntegrityCheckDetector internal constructor(
    expectedSignature: String?,
    private val trustedInstallers: Set<String>,
    private val probe: suspend (DeviceGuardContext, IntegrityCheckConfig) -> IntegrityCheckOutcome,
) : Detector<IntegrityCheckResult> {
    // Consumers paste `keytool -list -v` output that carries colons and whitespace — strip
    // both before storing so the runtime comparison doesn't trip on cosmetic input noise.
    private val expectedSignature: String? = expectedSignature?.normalizeSignatureHex()

    public constructor(
        expectedSignature: String? = null,
        trustedInstallers: Set<String> = emptySet(),
    ) : this(expectedSignature, trustedInstallers, probe = { ctx, cfg -> runIntegrityCheck(ctx, cfg) })

    override val id: String = DETECTOR_ID

    override suspend fun detect(context: DeviceGuardContext): DetectionResult<IntegrityCheckResult> {
        val config =
            IntegrityCheckConfig(
                expectedSignatureSha256 = expectedSignature,
                trustedInstallers = trustedInstallers,
            )
        val outcome = probe(context, config)
        if (!outcome.applicable) return DetectionResult.NotApplicable(id, outcome.reason)

        val signatureConfidence = sumWeights(outcome.signatureIndicators)
        val hookConfidence = sumWeights(outcome.hookIndicators)
        val signatureMismatch = signatureConfidence >= THRESHOLD
        val hookDetected = hookConfidence >= THRESHOLD

        val signatureNames = outcome.signatureIndicators.map { it.name }
        val hookNames = outcome.hookIndicators.map { it.name }

        val threats =
            buildList {
                if (signatureMismatch) add(threat(ThreatType.SignatureMismatch, signatureConfidence, signatureNames))
                if (hookDetected) add(threat(ThreatType.HookFramework, hookConfidence, hookNames))
            }

        return DetectionResult.Success(
            detectorId = id,
            data =
                IntegrityCheckResult(
                    signatureMismatch = signatureMismatch,
                    signatureCheckRun = outcome.signatureCheckRun,
                    hookFrameworkDetected = hookDetected,
                    signatureConfidence = signatureConfidence,
                    hookConfidence = hookConfidence,
                    signatureIndicators = signatureNames,
                    hookIndicators = hookNames,
                ),
            threats = threats,
            signals =
                mapOf(
                    "integritycheck.threshold" to THRESHOLD.toString(),
                    "integritycheck.signature_configured" to (expectedSignature != null).toString(),
                    "integritycheck.signature_check_run" to outcome.signatureCheckRun.toString(),
                    "integritycheck.signature_indicator_count" to outcome.signatureIndicators.size.toString(),
                    "integritycheck.hook_indicator_count" to outcome.hookIndicators.size.toString(),
                ),
        )
    }

    private fun sumWeights(indicators: List<IntegrityIndicator>): Float =
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

/** Normalize `keytool`-style hex: strip whitespace and `:` separators, lowercase. */
private fun String.normalizeSignatureHex(): String = replace(":", "").filterNot { it.isWhitespace() }.lowercase()

/**
 * Attach an [IntegrityCheckDetector] to the builder.
 *
 * @param expectedSignature hex SHA-256 of the release signing certificate. Accepted in any
 *   case and with `keytool`-style colon separators. When provided, the Android probe
 *   compares the running app's signature against it. When null, the probe falls back to
 *   weaker signals (debug certificate, untrusted installer).
 * @param trustedInstallers package names considered legitimate. Leave empty to skip.
 */
public fun DeviceGuard.Builder.enableIntegrityCheck(
    expectedSignature: String? = null,
    trustedInstallers: Set<String> = emptySet(),
): DeviceGuard.Builder = addDetector(IntegrityCheckDetector(expectedSignature, trustedInstallers))
