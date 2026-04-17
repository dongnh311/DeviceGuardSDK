package io.github.dongnh311.deviceguard.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DetectedThreatTest {
    @Test
    fun confidenceOutsideZeroOneRangeIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            DetectedThreat(type = "x", confidence = -0.1f, weight = 10)
        }
        assertFailsWith<IllegalArgumentException> {
            DetectedThreat(type = "x", confidence = 1.1f, weight = 10)
        }
    }

    @Test
    fun weightBelowZeroOrAboveMaxIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            DetectedThreat(type = "x", weight = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            DetectedThreat(type = "x", weight = 101)
        }
    }

    @Test
    fun factoryCopiesTypeIdAndDefaultWeight() {
        val threat = DetectedThreat.of(ThreatType.Root, confidence = 0.75f, indicators = listOf("su"))
        assertEquals(ThreatType.Root.id, threat.type)
        assertEquals(ThreatType.Root.defaultWeight, threat.weight)
        assertEquals(0.75f, threat.confidence)
        assertEquals(listOf("su"), threat.indicators)
    }
}
