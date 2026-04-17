package io.github.dongnh311.deviceguard.core

import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

class WeightedSumScoringTest {
    @Test
    fun emptyListScoresZero() {
        assertEquals(0, WeightedSumScoring.score(emptyList()))
    }

    @Test
    fun singleThreatUsesWeightAndConfidence() {
        val threat = DetectedThreat.of(ThreatType.Emulator, confidence = 1f)
        assertEquals(ThreatType.Emulator.defaultWeight, WeightedSumScoring.score(listOf(threat)))
    }

    @Test
    fun confidenceScalesWeightLinearly() {
        val threat = DetectedThreat.of(ThreatType.Emulator, confidence = 0.5f)
        val expected = (ThreatType.Emulator.defaultWeight * 0.5f).roundToInt()
        assertEquals(expected, WeightedSumScoring.score(listOf(threat)))
    }

    @Test
    fun multipleThreatsAccumulate() {
        val threats =
            listOf(
                DetectedThreat.of(ThreatType.VpnActive),
                DetectedThreat.of(ThreatType.ProxyActive),
            )
        val expected = ThreatType.VpnActive.defaultWeight + ThreatType.ProxyActive.defaultWeight
        assertEquals(expected, WeightedSumScoring.score(threats))
    }

    @Test
    fun scoreSaturatesAtOneHundred() {
        val threats =
            listOf(
                DetectedThreat.of(ThreatType.Root),
                DetectedThreat.of(ThreatType.SignatureMismatch),
                DetectedThreat.of(ThreatType.HookFramework),
            )
        assertEquals(100, WeightedSumScoring.score(threats))
    }

    @Test
    fun customThreatWeightApplies() {
        val custom = ThreatType.Custom(id = "my_custom_threat", defaultWeight = 12)
        assertEquals(12, WeightedSumScoring.score(listOf(DetectedThreat.of(custom))))
    }
}
