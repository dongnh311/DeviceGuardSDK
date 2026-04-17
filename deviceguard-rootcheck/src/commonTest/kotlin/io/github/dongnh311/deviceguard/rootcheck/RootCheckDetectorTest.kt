package io.github.dongnh311.deviceguard.rootcheck

import io.github.dongnh311.deviceguard.core.DetectionResult
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.ThreatType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RootCheckDetectorTest {
    @Test
    fun singleStrongIndicatorTripsLaxAndStrict() =
        runTest {
            val outcome = outcome(RootIndicator("su_binary:/system/bin/su", 1.0f))
            val lax = detect(outcome, strict = false).success()
            val strict = detect(outcome, strict = true).success()
            assertTrue(lax.data.isRooted)
            assertTrue(strict.data.isRooted)
            assertEquals(1.0f, lax.data.confidence)
        }

    @Test
    fun weakTestKeysIndicatorTripsOnlyInStrictMode() =
        runTest {
            val outcome = outcome(RootIndicator("build_tags:test-keys", 0.3f))
            assertFalse(detect(outcome, strict = false).success().data.isRooted, "0.3 < 0.5 lax threshold")
            assertTrue(detect(outcome, strict = true).success().data.isRooted, "0.3 ≥ 0.2 strict threshold")
        }

    @Test
    fun confidenceExactlyAtThresholdIsConsideredRooted() =
        runTest {
            val lax = outcome(RootIndicator("x", 0.5f))
            val strict = outcome(RootIndicator("x", 0.2f))
            assertTrue(detect(lax, strict = false).success().data.isRooted, "0.5 ≥ 0.5 must trip lax")
            assertTrue(detect(strict, strict = true).success().data.isRooted, "0.2 ≥ 0.2 must trip strict")
        }

    @Test
    fun confidenceJustBelowThresholdIsNotRooted() =
        runTest {
            val lax = outcome(RootIndicator("x", 0.49f))
            val strict = outcome(RootIndicator("x", 0.19f))
            assertFalse(detect(lax, strict = false).success().data.isRooted)
            assertFalse(detect(strict, strict = true).success().data.isRooted)
        }

    @Test
    fun noIndicatorsMeansNotRootedAndNoThreat() =
        runTest {
            val result = detect(outcome(), strict = false).success()
            assertFalse(result.data.isRooted)
            assertEquals(0f, result.data.confidence)
            assertTrue(result.threats.isEmpty())
        }

    @Test
    fun confidenceClampsToOneEvenWithMultipleFullWeightIndicators() =
        runTest {
            val outcome =
                outcome(
                    RootIndicator("su_binary:/system/bin/su", 1.0f),
                    RootIndicator("su_binary:/sbin/su", 1.0f),
                    RootIndicator("package:com.topjohnwu.magisk", 0.9f),
                )
            assertEquals(1.0f, detect(outcome, strict = false).success().data.confidence)
        }

    @Test
    fun rootedResultEmitsThreatWithOutcomesThreatType() =
        runTest {
            val outcome =
                outcome(
                    threat = ThreatType.Jailbreak,
                    indicators = arrayOf(RootIndicator("jailbreak_path:/Applications/Cydia.app", 0.9f)),
                )
            val threat = detect(outcome, strict = false).success().threats.single()
            assertEquals(ThreatType.Jailbreak.id, threat.type)
            assertEquals(0.9f, threat.confidence)
            assertEquals(listOf("jailbreak_path:/Applications/Cydia.app"), threat.indicators)
        }

    @Test
    fun signalsReportStrictAndThresholdOrthogonalToResultData() =
        runTest {
            val resultLax = detect(outcome(RootIndicator("x", 1.0f)), strict = false).success()
            assertEquals("root", resultLax.signals["rootcheck.threat_type"])
            assertEquals("false", resultLax.signals["rootcheck.strict"])
            assertEquals("0.5", resultLax.signals["rootcheck.threshold"])
            assertEquals("1", resultLax.signals["rootcheck.indicator_count"])

            val resultStrict = detect(outcome(RootIndicator("x", 1.0f)), strict = true).success()
            assertEquals("true", resultStrict.signals["rootcheck.strict"])
            assertEquals("0.2", resultStrict.signals["rootcheck.threshold"])
        }

    @Test
    fun inapplicablePlatformPropagatesNotApplicable() =
        runTest {
            val outcome = RootCheckOutcome(applicable = false, reason = "jvm")
            val notApplicable = detect(outcome, strict = false) as DetectionResult.NotApplicable
            assertEquals("rootcheck", notApplicable.detectorId)
            assertEquals("jvm", notApplicable.reason)
        }

    private fun outcome(
        vararg indicators: RootIndicator,
        threat: ThreatType = ThreatType.Root,
    ): RootCheckOutcome = RootCheckOutcome(applicable = true, threatType = threat, indicators = indicators.toList())

    private suspend fun detect(
        outcome: RootCheckOutcome,
        strict: Boolean,
    ): DetectionResult<RootCheckResult> = RootCheckDetector(strict = strict, probe = { outcome }).detect(fakeContext())

    private fun DetectionResult<RootCheckResult>.success(): DetectionResult.Success<RootCheckResult> = this as DetectionResult.Success
}

internal expect fun fakeContext(): DeviceGuardContext
