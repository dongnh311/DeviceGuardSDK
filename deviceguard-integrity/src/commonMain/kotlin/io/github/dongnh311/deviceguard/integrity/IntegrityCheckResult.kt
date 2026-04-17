package io.github.dongnh311.deviceguard.integrity

import kotlinx.serialization.Serializable

/**
 * Typed payload emitted by [IntegrityCheckDetector] on
 * [io.github.dongnh311.deviceguard.core.DetectionResult.Success.data].
 *
 * @property signatureMismatch `true` when the detector fired one or more signature-tamper
 *   signals strong enough to cross the threshold. Always `false` when [signatureCheckRun]
 *   is `false` — consumers that build dashboards must check [signatureCheckRun] first to
 *   distinguish "valid" from "not verified".
 * @property signatureCheckRun `true` when signature checking actually executed. `false` on
 *   platforms where the probe is not applicable or when the caller did not configure an
 *   expected signature *and* no fallback weak signal was available (for example, iOS).
 * @property hookFrameworkDetected `true` when Xposed / Frida / similar instrumentation
 *   artefacts are visible.
 * @property signatureConfidence clamped to `0f..1f`, aggregating signature-tamper signals.
 * @property hookConfidence clamped to `0f..1f`, aggregating hook-framework signals.
 * @property signatureIndicators forensic names of fired signature signals (no PII).
 * @property hookIndicators forensic names of fired hook signals (no PII).
 */
@Serializable
public data class IntegrityCheckResult(
    public val signatureMismatch: Boolean,
    public val signatureCheckRun: Boolean,
    public val hookFrameworkDetected: Boolean,
    public val signatureConfidence: Float,
    public val hookConfidence: Float,
    public val signatureIndicators: List<String>,
    public val hookIndicators: List<String>,
) {
    init {
        require(signatureConfidence in 0f..1f) { "signatureConfidence must be within 0f..1f" }
        require(hookConfidence in 0f..1f) { "hookConfidence must be within 0f..1f" }
    }
}
