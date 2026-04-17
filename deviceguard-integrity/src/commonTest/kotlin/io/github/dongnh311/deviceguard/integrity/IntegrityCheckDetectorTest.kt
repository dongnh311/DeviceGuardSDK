package io.github.dongnh311.deviceguard.integrity

import io.github.dongnh311.deviceguard.core.DetectionResult
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.ThreatType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntegrityCheckDetectorTest {
    @Test
    fun signatureHitOnlyEmitsSignatureThreat() =
        runTest {
            val success =
                detect(
                    outcome(
                        signature = listOf(IntegrityIndicator("signature_mismatch:got=abc", 1.0f)),
                    ),
                ).success()
            assertTrue(success.data.signatureMismatch)
            assertTrue(success.data.signatureCheckRun)
            assertFalse(success.data.hookFrameworkDetected)
            assertEquals(1, success.threats.size)
            assertEquals(ThreatType.SignatureMismatch.id, success.threats.single().type)
        }

    @Test
    fun hookHitOnlyEmitsHookThreat() =
        runTest {
            val success =
                detect(
                    outcome(
                        hook = listOf(IntegrityIndicator("hook_package:de.robv.android.xposed.installer", 1.0f)),
                    ),
                ).success()
            assertFalse(success.data.signatureMismatch)
            assertTrue(success.data.hookFrameworkDetected)
            assertEquals(1, success.threats.size)
            assertEquals(ThreatType.HookFramework.id, success.threats.single().type)
        }

    @Test
    fun bothStreamsFireBothThreats() =
        runTest {
            val success =
                detect(
                    outcome(
                        signature = listOf(IntegrityIndicator("signature_mismatch:got=abc", 1.0f)),
                        hook = listOf(IntegrityIndicator("frida_artifact:/usr/lib/frida/frida-agent.dylib", 1.0f)),
                    ),
                ).success()
            assertTrue(success.data.signatureMismatch)
            assertTrue(success.data.hookFrameworkDetected)
            assertEquals(
                setOf(ThreatType.SignatureMismatch.id, ThreatType.HookFramework.id),
                success.threats.map { it.type }.toSet(),
            )
        }

    @Test
    fun confidenceExactlyAtThresholdTrips() =
        runTest {
            val success =
                detect(
                    outcome(
                        signature = listOf(IntegrityIndicator("x", 0.5f)),
                        hook = listOf(IntegrityIndicator("y", 0.5f)),
                    ),
                ).success()
            assertTrue(success.data.signatureMismatch, "signature at 0.5 must trip")
            assertTrue(success.data.hookFrameworkDetected, "hook at 0.5 must trip")
        }

    @Test
    fun confidenceJustBelowThresholdDoesNotTrip() =
        runTest {
            val success =
                detect(
                    outcome(
                        signature = listOf(IntegrityIndicator("x", 0.49f)),
                        hook = listOf(IntegrityIndicator("y", 0.49f)),
                    ),
                ).success()
            assertFalse(success.data.signatureMismatch)
            assertFalse(success.data.hookFrameworkDetected)
            assertTrue(success.threats.isEmpty())
        }

    @Test
    fun confidenceClampsToOneWithMultipleStrongIndicators() =
        runTest {
            val success =
                detect(
                    outcome(
                        hook =
                            listOf(
                                IntegrityIndicator("a", 1.0f),
                                IntegrityIndicator("b", 1.0f),
                            ),
                    ),
                ).success()
            assertEquals(1.0f, success.data.hookConfidence)
        }

    @Test
    fun weakSignalsBelowThresholdDoNotTrip() =
        runTest {
            val success =
                detect(
                    outcome(
                        signature =
                            listOf(
                                IntegrityIndicator("untrusted_installer:unknown", 0.3f),
                                IntegrityIndicator("debug_certificate", 0.1f),
                            ),
                    ),
                ).success()
            assertEquals(0.4f, success.data.signatureConfidence)
            assertFalse(success.data.signatureMismatch)
        }

    @Test
    fun noIndicatorsAcrossBothStreamsMeansNoThreats() =
        runTest {
            val success = detect(outcome()).success()
            assertFalse(success.data.signatureMismatch)
            assertFalse(success.data.hookFrameworkDetected)
            assertTrue(success.threats.isEmpty())
        }

    @Test
    fun signatureCheckNotRunWhenPlatformSkips() =
        runTest {
            // applicable = true but signatureCheckRun = false — simulates a platform that
            // has no signature verification path (e.g. a future JVM probe that only checks
            // hooks). Consumers must read signatureCheckRun to distinguish valid-from-
            // skipped.
            val success = detect(IntegrityCheckOutcome(applicable = true, signatureCheckRun = false)).success()
            assertFalse(success.data.signatureMismatch)
            assertFalse(success.data.signatureCheckRun)
            assertEquals("false", success.signals["integritycheck.signature_check_run"])
        }

    @Test
    fun keytoolFormattedHexIsNormalisedBeforeForwarding() =
        runTest {
            // keytool -list -v prints "AB:CD:EF:...  " (colons + trailing whitespace).
            // Stored config must strip to a plain lowercase hex string.
            var seenConfig: IntegrityCheckConfig? = null
            val detector =
                IntegrityCheckDetector(
                    expectedSignature = "  AB:cd:EF:12 \n",
                    trustedInstallers = setOf("com.android.vending"),
                    probe = { _, config ->
                        seenConfig = config
                        outcome()
                    },
                )
            detector.detect(fakeContext())
            assertEquals("abcdef12", seenConfig?.expectedSignatureSha256)
            assertEquals(setOf("com.android.vending"), seenConfig?.trustedInstallers)
        }

    @Test
    fun signalsAdvertiseConfiguration() =
        runTest {
            val withSignature =
                IntegrityCheckDetector("abc", emptySet(), probe = { _, _ -> outcome() })
                    .detect(fakeContext())
                    .success()
            assertEquals("true", withSignature.signals["integritycheck.signature_configured"])
            assertEquals("0.5", withSignature.signals["integritycheck.threshold"])
            assertEquals("true", withSignature.signals["integritycheck.signature_check_run"])

            val noSignature =
                IntegrityCheckDetector(null, emptySet(), probe = { _, _ -> outcome() })
                    .detect(fakeContext())
                    .success()
            assertEquals("false", noSignature.signals["integritycheck.signature_configured"])
        }

    @Test
    fun inapplicablePlatformPropagatesNotApplicable() =
        runTest {
            val result = detect(IntegrityCheckOutcome(applicable = false, reason = "jvm"))
            val notApplicable = result as DetectionResult.NotApplicable
            assertEquals("integritycheck", notApplicable.detectorId)
            assertEquals("jvm", notApplicable.reason)
        }

    private fun outcome(
        signature: List<IntegrityIndicator> = emptyList(),
        hook: List<IntegrityIndicator> = emptyList(),
    ): IntegrityCheckOutcome =
        IntegrityCheckOutcome(
            applicable = true,
            signatureCheckRun = true,
            signatureIndicators = signature,
            hookIndicators = hook,
        )

    private suspend fun detect(outcome: IntegrityCheckOutcome): DetectionResult<IntegrityCheckResult> =
        IntegrityCheckDetector(expectedSignature = null, trustedInstallers = emptySet(), probe = { _, _ -> outcome }).detect(fakeContext())

    private fun DetectionResult<IntegrityCheckResult>.success(): DetectionResult.Success<IntegrityCheckResult> =
        this as DetectionResult.Success
}

internal expect fun fakeContext(): DeviceGuardContext
