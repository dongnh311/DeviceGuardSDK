package io.github.dongnh311.deviceguard.core

import kotlinx.serialization.Serializable

/**
 * A single threat surfaced by a [Detector].
 *
 * @property type stable identifier, matches [ThreatType.id].
 * @property confidence detector confidence in the interval `[0f, 1f]`; `1f` is the default.
 * @property weight contribution to the risk score. Defaults to the threat's
 *   [ThreatType.defaultWeight] but detectors may override.
 * @property indicators opaque list of signals that caused the threat to fire. Useful for
 *   forensic logging; keep content free of PII.
 */
@Serializable
public data class DetectedThreat(
    public val type: String,
    public val confidence: Float = 1f,
    public val weight: Int,
    public val indicators: List<String> = emptyList(),
) {
    init {
        require(type.isNotBlank()) { "Threat type id must not be blank" }
        require(confidence in 0f..1f) { "confidence must be within 0f..1f" }
        require(weight in 0..MAX_WEIGHT) { "weight must be within 0..$MAX_WEIGHT" }
    }

    public companion object {
        private const val MAX_WEIGHT = 100

        /** Factory that pulls [weight] and [type] defaults from [threat]. */
        public fun of(
            threat: ThreatType,
            confidence: Float = 1f,
            indicators: List<String> = emptyList(),
            weight: Int = threat.defaultWeight,
        ): DetectedThreat =
            DetectedThreat(
                type = threat.id,
                confidence = confidence,
                weight = weight,
                indicators = indicators,
            )
    }
}
