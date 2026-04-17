package io.github.dongnh311.deviceguard.core

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceGuardTest {
    @Test
    fun emptyDetectorListProducesSafeReport() =
        runTest {
            val report =
                DeviceGuard
                    .Builder(testContext())
                    .clock { FIXED_EPOCH }
                    .build()
                    .analyze()

            assertEquals(0, report.riskScore)
            assertEquals(RiskLevel.SAFE, report.riskLevel)
            assertTrue(report.threats.isEmpty())
            assertTrue(report.signals.isEmpty())
            assertTrue(report.errors.isEmpty())
            assertNull(report.fingerprint)
            assertEquals(FIXED_EPOCH, report.analyzedAtEpochMillis)
        }

    @Test
    fun detectorThreatsAndSignalsAreAggregated() =
        runTest {
            val rootDetector =
                stubDetector(
                    id = "root",
                    result =
                        DetectionResult.Success(
                            detectorId = "root",
                            data = true,
                            threats = listOf(DetectedThreat.of(ThreatType.Root, confidence = 0.8f)),
                            signals = mapOf("su_path" to "/system/bin/su"),
                        ),
                )
            val networkDetector =
                stubDetector(
                    id = "network",
                    result =
                        DetectionResult.Success(
                            detectorId = "network",
                            data = Unit,
                            threats = listOf(DetectedThreat.of(ThreatType.VpnActive)),
                            signals = mapOf("iface" to "tun0"),
                        ),
                )

            val report =
                DeviceGuard
                    .Builder(testContext())
                    .addDetector(rootDetector)
                    .addDetector(networkDetector)
                    .clock { FIXED_EPOCH }
                    .build()
                    .analyze()

            assertEquals(2, report.threats.size)
            assertEquals(setOf("su_path", "iface"), report.signals.keys)
            assertTrue(report.riskScore >= ThreatType.VpnActive.defaultWeight)
            assertEquals(RiskLevel.fromScore(report.riskScore), report.riskLevel)
        }

    @Test
    fun fingerprintDetectorPopulatesReportFingerprint() =
        runTest {
            val fp =
                DeviceFingerprint(
                    id = "deadbeef",
                    signals = mapOf("os" to "Android"),
                )
            val detector =
                stubDetector(
                    id = "fingerprint",
                    result =
                        DetectionResult.Success(
                            detectorId = "fingerprint",
                            data = fp,
                        ),
                )

            val report =
                DeviceGuard
                    .Builder(testContext())
                    .addDetector(detector)
                    .clock { FIXED_EPOCH }
                    .build()
                    .analyze()

            assertEquals(fp, report.fingerprint)
        }

    @Test
    fun failedDetectorsAreRecordedButDontAffectScore() =
        runTest {
            val throwing =
                object : Detector<Unit> {
                    override val id: String = "throws"

                    override suspend fun detect(context: DeviceGuardContext): DetectionResult<Unit> {
                        error("boom")
                    }
                }
            val returnsFailed =
                stubDetector<Unit>(
                    id = "returns_failed",
                    result =
                        DetectionResult.Failed(
                            detectorId = "returns_failed",
                            message = "api unavailable",
                            errorType = "UnsupportedOperationException",
                        ),
                )

            val report =
                DeviceGuard
                    .Builder(testContext())
                    .addDetector(throwing)
                    .addDetector(returnsFailed)
                    .clock { FIXED_EPOCH }
                    .build()
                    .analyze()

            assertEquals(0, report.riskScore)
            assertEquals(RiskLevel.SAFE, report.riskLevel)
            assertEquals(2, report.errors.size)
            assertEquals(setOf("throws", "returns_failed"), report.errors.map { it.detectorId }.toSet())
        }

    @Test
    fun notApplicableResultsDoNotProduceErrors() =
        runTest {
            val detector =
                stubDetector<Unit>(
                    id = "skip",
                    result = DetectionResult.NotApplicable(detectorId = "skip", reason = "jvm"),
                )

            val report =
                DeviceGuard
                    .Builder(testContext())
                    .addDetector(detector)
                    .clock { FIXED_EPOCH }
                    .build()
                    .analyze()

            assertTrue(report.errors.isEmpty())
            assertTrue(report.threats.isEmpty())
        }

    @Test
    fun loggerReceivesDetectorFailures() =
        runTest {
            val events = mutableListOf<String>()
            val logger =
                object : DeviceGuardLogger {
                    override fun log(
                        level: DeviceGuardLogger.LogLevel,
                        tag: String,
                        message: String,
                        error: Throwable?,
                    ) {
                        events += "$level:$message"
                    }
                }

            DeviceGuard
                .Builder(testContext())
                .logger(logger)
                .addDetector(
                    object : Detector<Unit> {
                        override val id: String = "boom"

                        override suspend fun detect(context: DeviceGuardContext): DetectionResult<Unit> {
                            error("expected in test")
                        }
                    },
                ).clock { FIXED_EPOCH }
                .build()
                .analyze()

            assertNotNull(events.firstOrNull { it.startsWith("ERROR") }, "ERROR log expected")
            assertNotNull(events.firstOrNull { it.startsWith("WARN") }, "WARN log expected")
        }

    @Test
    fun noOpLoggerShortCircuitsViaIsEnabled() =
        runTest {
            var built = 0
            val logger =
                object : DeviceGuardLogger {
                    override fun isEnabled(level: DeviceGuardLogger.LogLevel): Boolean = false

                    override fun log(
                        level: DeviceGuardLogger.LogLevel,
                        tag: String,
                        message: String,
                        error: Throwable?,
                    ) {
                        built += 1
                    }
                }

            DeviceGuard
                .Builder(testContext())
                .logger(logger)
                .clock { FIXED_EPOCH }
                .build()
                .analyze()

            assertEquals(0, built, "disabled logger must not receive any log calls")
        }

    private companion object {
        private const val FIXED_EPOCH = 1_700_000_000_000L
    }
}
