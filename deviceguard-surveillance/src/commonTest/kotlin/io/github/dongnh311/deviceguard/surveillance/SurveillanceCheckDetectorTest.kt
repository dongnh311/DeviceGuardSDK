package io.github.dongnh311.deviceguard.surveillance

import io.github.dongnh311.deviceguard.core.DetectionResult
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.ThreatType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SurveillanceCheckDetectorTest {
    @Test
    fun accessibilityHitFiresOnlyA11yThreat() =
        runTest {
            val success =
                detect(
                    outcome(
                        listOf(
                            SurveillanceIndicator(SurveillanceCategory.AccessibilityAbuse, "a11y:com.evil.app", 1.0f),
                        ),
                    ),
                ).success()
            assertTrue(success.data.accessibilityAbuse)
            assertFalse(success.data.overlayPermission)
            assertEquals(ThreatType.AccessibilityAbuse.id, success.threats.single().type)
        }

    @Test
    fun multipleCategoriesFireIndependentThreats() =
        runTest {
            val success =
                detect(
                    outcome(
                        listOf(
                            SurveillanceIndicator(SurveillanceCategory.AccessibilityAbuse, "a11y:com.banking.trojan", 1.0f),
                            SurveillanceIndicator(SurveillanceCategory.NotificationListener, "notif:com.otp.thief", 1.0f),
                            SurveillanceIndicator(SurveillanceCategory.DeviceAdminActive, "admin:com.ransom.ware", 1.0f),
                        ),
                    ),
                ).success()
            assertTrue(success.data.accessibilityAbuse)
            assertTrue(success.data.notificationListener)
            assertTrue(success.data.deviceAdminActive)
            assertEquals(
                setOf(
                    ThreatType.AccessibilityAbuse.id,
                    ThreatType.NotificationListener.id,
                    ThreatType.DeviceAdminActive.id,
                ),
                success.threats.map { it.type }.toSet(),
            )
        }

    @Test
    fun confidenceJustBelowThresholdDoesNotTrip() =
        runTest {
            val success =
                detect(
                    outcome(
                        listOf(SurveillanceIndicator(SurveillanceCategory.SuspiciousIme, "ime:weak", 0.49f)),
                    ),
                ).success()
            assertFalse(success.data.suspiciousIme)
            assertTrue(success.threats.isEmpty())
        }

    @Test
    fun confidenceExactlyAtThresholdTrips() =
        runTest {
            val success =
                detect(
                    outcome(
                        listOf(SurveillanceIndicator(SurveillanceCategory.SuspiciousIme, "ime:borderline", 0.5f)),
                    ),
                ).success()
            assertTrue(success.data.suspiciousIme)
        }

    @Test
    fun indicatorsInSameCategoryAccumulateAndClamp() =
        runTest {
            val success =
                detect(
                    outcome(
                        listOf(
                            SurveillanceIndicator(SurveillanceCategory.AccessibilityAbuse, "a11y:pkg1", 0.6f),
                            SurveillanceIndicator(SurveillanceCategory.AccessibilityAbuse, "a11y:pkg2", 0.6f),
                        ),
                    ),
                ).success()
            assertTrue(success.data.accessibilityAbuse)
            val threat = success.threats.single { it.type == ThreatType.AccessibilityAbuse.id }
            assertEquals(1.0f, threat.confidence, "confidence must clamp to 1.0")
        }

    @Test
    fun emptyIndicatorsMeansNoThreats() =
        runTest {
            val success = detect(outcome(emptyList())).success()
            assertTrue(success.threats.isEmpty())
            assertFalse(success.data.accessibilityAbuse)
            assertFalse(success.data.overlayPermission)
            assertFalse(success.data.notificationListener)
            assertFalse(success.data.deviceAdminActive)
            assertFalse(success.data.suspiciousIme)
            assertFalse(success.data.usageStatsGranted)
            assertFalse(success.data.automationToolRunning)
            assertFalse(success.data.debuggerAttachedElsewhere)
        }

    @Test
    fun inapplicablePlatformPropagatesNotApplicable() =
        runTest {
            val result = detect(SurveillanceOutcome(applicable = false, reason = "ios"))
            val notApplicable = result as DetectionResult.NotApplicable
            assertEquals("surveillancecheck", notApplicable.detectorId)
            assertEquals("ios", notApplicable.reason)
        }

    @Test
    fun signalsSummariseTheProbe() =
        runTest {
            val success =
                detect(
                    outcome(
                        listOf(
                            SurveillanceIndicator(SurveillanceCategory.AccessibilityAbuse, "a", 1.0f),
                            SurveillanceIndicator(SurveillanceCategory.NotificationListener, "b", 1.0f),
                            SurveillanceIndicator(SurveillanceCategory.DeviceAdminActive, "c", 1.0f),
                        ),
                    ),
                ).success()
            assertEquals("0.5", success.signals["surveillancecheck.threshold"])
            assertEquals("3", success.signals["surveillancecheck.indicator_count"])
        }

    @Test
    fun systemPackageWhitelistRecognisesKnownPrefixes() {
        assertTrue(isSystemPackage("android"))
        assertTrue(isSystemPackage("com.android.settings"))
        assertTrue(isSystemPackage("com.google.android.inputmethod.latin"))
        assertTrue(isSystemPackage("com.samsung.android.app.telephonyui"))
        assertFalse(isSystemPackage("com.evil.banking.trojan"))
        assertFalse(isSystemPackage("com.thirdparty.keyboard"))
    }

    private fun outcome(indicators: List<SurveillanceIndicator>): SurveillanceOutcome =
        SurveillanceOutcome(applicable = true, indicators = indicators)

    private suspend fun detect(outcome: SurveillanceOutcome): DetectionResult<SurveillanceCheckResult> =
        SurveillanceCheckDetector(probe = { outcome }).detect(fakeContext())

    private fun DetectionResult<SurveillanceCheckResult>.success(): DetectionResult.Success<SurveillanceCheckResult> =
        this as DetectionResult.Success
}

internal expect fun fakeContext(): DeviceGuardContext
