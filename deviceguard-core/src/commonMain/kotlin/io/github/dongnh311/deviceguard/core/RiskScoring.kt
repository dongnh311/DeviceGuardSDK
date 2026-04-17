package io.github.dongnh311.deviceguard.core

import kotlin.math.roundToInt

/**
 * Strategy for collapsing a list of [DetectedThreat]s into a single `0..100` risk score.
 *
 * Implementations should be pure and total — the same input must produce the same output and
 * the result must always fall in `0..100`. Use [WeightedSumScoring] unless you need to weight
 * threats differently per application.
 */
public fun interface RiskScoring {
    /** Compute the aggregated score for [threats]. */
    public fun score(threats: List<DetectedThreat>): Int
}

/**
 * Default scoring: each threat contributes `round(weight * confidence)` points; the sum is
 * clamped to `0..100`. This saturates quickly once a high-severity threat fires, while
 * remaining additive for low-severity signals.
 */
public object WeightedSumScoring : RiskScoring {
    override fun score(threats: List<DetectedThreat>): Int =
        threats.sumOf { (it.weight * it.confidence).roundToInt() }.coerceIn(0, MAX_WEIGHT)
}
