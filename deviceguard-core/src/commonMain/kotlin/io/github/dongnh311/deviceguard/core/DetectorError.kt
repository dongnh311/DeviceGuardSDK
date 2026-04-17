package io.github.dongnh311.deviceguard.core

import kotlinx.serialization.Serializable

/**
 * A failed detector invocation recorded on a [SecurityReport].
 *
 * Detectors that throw during [Detector.detect] or return [DetectionResult.Failed] contribute
 * one [DetectorError] to the final report instead of a threat. Consumers can surface the list
 * to help diagnose misconfiguration or platform-specific breakage, while the aggregated
 * [SecurityReport.riskScore] ignores failures so a detector outage doesn't skew the score.
 */
@Serializable
public data class DetectorError(
    public val detectorId: String,
    public val message: String,
    public val errorType: String? = null,
) {
    init {
        require(detectorId.isNotBlank()) { "detectorId must not be blank" }
    }
}
