package io.github.dongnh311.deviceguard.remote

import io.github.dongnh311.deviceguard.core.DetectionResult
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.ThreatType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoteCheckDetectorTest {
    @Test
    fun installedHitOnlyEmitsInstalledThreat() =
        runTest {
            val success =
                detect(
                    outcome(installed = listOf(RemoteIndicator("remote_pkg:com.teamviewer.teamviewer.market.mobile", 1.0f))),
                ).success()
            assertTrue(success.data.remoteControlInstalled)
            assertFalse(success.data.screenBeingCaptured)
            assertEquals(ThreatType.RemoteControlInstalled.id, success.threats.single().type)
        }

    @Test
    fun captureHitOnlyEmitsCaptureThreat() =
        runTest {
            val success =
                detect(outcome(capture = listOf(RemoteIndicator("screen_captured:UIScreen.isCaptured", 1.0f)))).success()
            assertFalse(success.data.remoteControlInstalled)
            assertTrue(success.data.screenBeingCaptured)
            assertEquals(ThreatType.ScreenBeingCaptured.id, success.threats.single().type)
        }

    @Test
    fun bothAxesFireBothThreats() =
        runTest {
            val success =
                detect(
                    outcome(
                        installed = listOf(RemoteIndicator("remote_pkg:com.anydesk.anydeskandroid", 1.0f)),
                        capture = listOf(RemoteIndicator("screensharing_daemon:screensharingd", 0.8f)),
                    ),
                ).success()
            assertTrue(success.data.remoteControlInstalled)
            assertTrue(success.data.screenBeingCaptured)
            assertEquals(
                setOf(ThreatType.RemoteControlInstalled.id, ThreatType.ScreenBeingCaptured.id),
                success.threats.map { it.type }.toSet(),
            )
        }

    @Test
    fun confidenceJustBelowThresholdDoesNotTrip() =
        runTest {
            val success =
                detect(
                    outcome(
                        installed = listOf(RemoteIndicator("x", 0.49f)),
                        capture = listOf(RemoteIndicator("y", 0.49f)),
                    ),
                ).success()
            assertFalse(success.data.remoteControlInstalled)
            assertFalse(success.data.screenBeingCaptured)
            assertTrue(success.threats.isEmpty())
        }

    @Test
    fun confidenceClampsToOneWithMultipleStrongIndicators() =
        runTest {
            val success =
                detect(
                    outcome(
                        installed =
                            listOf(
                                RemoteIndicator("remote_pkg:a", 1.0f),
                                RemoteIndicator("remote_pkg:b", 1.0f),
                            ),
                    ),
                ).success()
            assertEquals(1.0f, success.data.installedConfidence)
        }

    @Test
    fun noIndicatorsMeansNoThreats() =
        runTest {
            val success = detect(outcome()).success()
            assertTrue(success.threats.isEmpty())
            assertEquals(0f, success.data.installedConfidence)
            assertEquals(0f, success.data.captureConfidence)
        }

    @Test
    fun signalsSummariseTheProbe() =
        runTest {
            val success =
                detect(
                    outcome(
                        installed = listOf(RemoteIndicator("a", 1.0f), RemoteIndicator("b", 0.3f)),
                        capture = listOf(RemoteIndicator("c", 1.0f)),
                    ),
                ).success()
            assertEquals("0.5", success.signals["remotecheck.threshold"])
            assertEquals("2", success.signals["remotecheck.installed_indicator_count"])
            assertEquals("1", success.signals["remotecheck.capture_indicator_count"])
        }

    @Test
    fun inapplicablePlatformPropagatesNotApplicable() =
        runTest {
            val result = detect(RemoteCheckOutcome(applicable = false, reason = "browser"))
            val notApplicable = result as DetectionResult.NotApplicable
            assertEquals("remotecheck", notApplicable.detectorId)
            assertEquals("browser", notApplicable.reason)
        }

    private fun outcome(
        installed: List<RemoteIndicator> = emptyList(),
        capture: List<RemoteIndicator> = emptyList(),
    ): RemoteCheckOutcome =
        RemoteCheckOutcome(
            applicable = true,
            installedIndicators = installed,
            captureIndicators = capture,
        )

    private suspend fun detect(outcome: RemoteCheckOutcome): DetectionResult<RemoteCheckResult> =
        RemoteCheckDetector(probe = { outcome }).detect(fakeContext())

    private fun DetectionResult<RemoteCheckResult>.success(): DetectionResult.Success<RemoteCheckResult> = this as DetectionResult.Success
}

internal expect fun fakeContext(): DeviceGuardContext
