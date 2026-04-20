package io.github.dongnh311.deviceguard.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceGuardFlowTest {
    @Test
    fun firstEmissionIsImmediate() =
        runTest {
            val guard =
                DeviceGuard
                    .Builder(testContext())
                    .addDetector(stubDetector("s", DetectionResult.Success("s", data = Unit)))
                    .clock { 0L }
                    .build()
            val report = guard.observe(periodMs = 500L).first()
            assertEquals(0, report.riskScore)
        }

    @Test
    fun distinctSuppressesDuplicateEmissions() =
        runTest {
            val guard =
                DeviceGuard
                    .Builder(testContext())
                    .addDetector(stubDetector("s", DetectionResult.Success("s", data = Unit)))
                    .clock { 0L }
                    .build()
            val emissions = guard.observe(periodMs = 500L).take(1).toList()
            assertEquals(1, emissions.size)
        }

    @Test
    fun rejectsPeriodsBelowFloor() {
        val guard =
            DeviceGuard
                .Builder(testContext())
                .clock { 0L }
                .build()
        assertFailsWith<IllegalArgumentException> {
            guard.observe(periodMs = 100L)
        }
    }
}
