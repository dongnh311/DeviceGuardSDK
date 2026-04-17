package io.github.dongnh311.deviceguard.core

import kotlin.test.Test
import kotlin.test.assertEquals

class RiskLevelTest {
    @Test
    fun zeroMapsToSafe() {
        assertEquals(RiskLevel.SAFE, RiskLevel.fromScore(0))
    }

    @Test
    fun bucketBoundaries() {
        assertEquals(RiskLevel.SAFE, RiskLevel.fromScore(19))
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(20))
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(39))
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(40))
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(59))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(60))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(79))
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(80))
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(100))
    }

    @Test
    fun outOfRangeScoresAreClamped() {
        assertEquals(RiskLevel.SAFE, RiskLevel.fromScore(-50))
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(9_999))
    }

    @Test
    fun matchesAgreesWithFromScore() {
        val scores = listOf(0, 20, 40, 60, 80, 100)
        for (score in scores) {
            val level = RiskLevel.fromScore(score)
            val matching = RiskLevel.entries.filter { it.matches(score) }
            assertEquals(listOf(level), matching)
        }
    }
}
