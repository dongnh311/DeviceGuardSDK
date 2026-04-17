package io.github.dongnh311.deviceguard.emulator

import io.github.dongnh311.deviceguard.core.DetectionResult
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.ThreatType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmulatorCheckDetectorTest {
    @Test
    fun emulatorHitOnlyEmitsEmulatorThreat() =
        runTest {
            val success =
                detect(
                    EmulatorCheckOutcome(
                        applicable = true,
                        emulatorIndicators = listOf(EmulatorIndicator("hardware:goldfish", 1.0f)),
                    ),
                ).success()

            assertTrue(success.data.isEmulator)
            assertFalse(success.data.isDebuggerAttached)
            assertEquals(1, success.threats.size)
            assertEquals(ThreatType.Emulator.id, success.threats.single().type)
        }

    @Test
    fun debuggerHitOnlyEmitsDebuggerThreat() =
        runTest {
            val success =
                detect(
                    EmulatorCheckOutcome(
                        applicable = true,
                        debuggerIndicators = listOf(EmulatorIndicator("debug.isDebuggerConnected", 1.0f)),
                    ),
                ).success()

            assertFalse(success.data.isEmulator)
            assertTrue(success.data.isDebuggerAttached)
            assertEquals(1, success.threats.size)
            assertEquals(ThreatType.DebuggerAttached.id, success.threats.single().type)
        }

    @Test
    fun emulatorAndDebuggerBothFireBothThreats() =
        runTest {
            val success =
                detect(
                    EmulatorCheckOutcome(
                        applicable = true,
                        emulatorIndicators = listOf(EmulatorIndicator("hardware:ranchu", 1.0f)),
                        debuggerIndicators = listOf(EmulatorIndicator("jvm_arg:-agentlib:jdwp=...", 1.0f)),
                    ),
                ).success()

            assertTrue(success.data.isEmulator)
            assertTrue(success.data.isDebuggerAttached)
            assertEquals(2, success.threats.size)
            assertEquals(
                setOf(ThreatType.Emulator.id, ThreatType.DebuggerAttached.id),
                success.threats.map { it.type }.toSet(),
            )
        }

    @Test
    fun confidenceExactlyAtThresholdTrips() =
        runTest {
            val success =
                detect(
                    EmulatorCheckOutcome(
                        applicable = true,
                        emulatorIndicators = listOf(EmulatorIndicator("x", 0.5f)),
                        debuggerIndicators = listOf(EmulatorIndicator("y", 0.5f)),
                    ),
                ).success()
            assertTrue(success.data.isEmulator, "emulator at 0.5 must trip")
            assertTrue(success.data.isDebuggerAttached, "debugger at 0.5 must trip")
        }

    @Test
    fun confidenceJustBelowThresholdDoesNotTrip() =
        runTest {
            val success =
                detect(
                    EmulatorCheckOutcome(
                        applicable = true,
                        emulatorIndicators = listOf(EmulatorIndicator("x", 0.49f)),
                        debuggerIndicators = listOf(EmulatorIndicator("y", 0.49f)),
                    ),
                ).success()
            assertFalse(success.data.isEmulator)
            assertFalse(success.data.isDebuggerAttached)
            assertTrue(success.threats.isEmpty())
        }

    @Test
    fun confidenceClampsToOneWhenIndicatorsExceed() =
        runTest {
            val success =
                detect(
                    EmulatorCheckOutcome(
                        applicable = true,
                        emulatorIndicators =
                            listOf(
                                EmulatorIndicator("a", 1.0f),
                                EmulatorIndicator("b", 0.7f),
                            ),
                    ),
                ).success()
            assertEquals(1.0f, success.data.emulatorConfidence)
        }

    @Test
    fun noIndicatorsAcrossBothListsMeansNoThreats() =
        runTest {
            val success = detect(EmulatorCheckOutcome(applicable = true)).success()
            assertFalse(success.data.isEmulator)
            assertFalse(success.data.isDebuggerAttached)
            assertTrue(success.threats.isEmpty())
            assertEquals(0f, success.data.emulatorConfidence)
            assertEquals(0f, success.data.debuggerConfidence)
        }

    @Test
    fun indicatorsSplitByKindOnResult() =
        runTest {
            val success =
                detect(
                    EmulatorCheckOutcome(
                        applicable = true,
                        emulatorIndicators = listOf(EmulatorIndicator("hardware:goldfish", 1.0f)),
                        debuggerIndicators = listOf(EmulatorIndicator("debug.waitingForDebugger", 1.0f)),
                    ),
                ).success()
            assertEquals(listOf("hardware:goldfish"), success.data.emulatorIndicators)
            assertEquals(listOf("debug.waitingForDebugger"), success.data.debuggerIndicators)
        }

    @Test
    fun signalsSummariseTheProbe() =
        runTest {
            val success =
                detect(
                    EmulatorCheckOutcome(
                        applicable = true,
                        emulatorIndicators = listOf(EmulatorIndicator("a", 1.0f), EmulatorIndicator("b", 0.3f)),
                        debuggerIndicators = listOf(EmulatorIndicator("c", 1.0f)),
                    ),
                ).success()
            assertEquals("0.5", success.signals["emulatorcheck.threshold"])
            assertEquals("2", success.signals["emulatorcheck.emulator_indicator_count"])
            assertEquals("1", success.signals["emulatorcheck.debugger_indicator_count"])
        }

    @Test
    fun inapplicablePlatformPropagatesNotApplicable() =
        runTest {
            val result = detect(EmulatorCheckOutcome(applicable = false, reason = "test"))
            val notApplicable = result as DetectionResult.NotApplicable
            assertEquals("emulatorcheck", notApplicable.detectorId)
            assertEquals("test", notApplicable.reason)
        }

    private suspend fun detect(outcome: EmulatorCheckOutcome): DetectionResult<EmulatorCheckResult> =
        EmulatorCheckDetector(probe = { outcome }).detect(fakeContext())

    private fun DetectionResult<EmulatorCheckResult>.success(): DetectionResult.Success<EmulatorCheckResult> =
        this as DetectionResult.Success
}

internal expect fun fakeContext(): DeviceGuardContext
