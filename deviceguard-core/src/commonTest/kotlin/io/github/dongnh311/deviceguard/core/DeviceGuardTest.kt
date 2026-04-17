package io.github.dongnh311.deviceguard.core

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalDeviceGuardApi::class)
class DeviceGuardTest {
    @Test
    fun emptyDetectorListProducesSafeReport() =
        runTest {
            val guard =
                DeviceGuard
                    .Builder(testContext())
                    .clock { FIXED_EPOCH }
                    .build()

            val report = guard.analyze()

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
            val ids = report.errors.map { it.detectorId }.toSet()
            assertEquals(setOf("throws", "returns_failed"), ids)
        }

    @Test
    fun notApplicableResultsDoNotProduceErrors() =
        runTest {
            val detector =
                stubDetector<Unit>(
                    id = "skip",
                    result =
                        DetectionResult.NotApplicable(
                            detectorId = "skip",
                            reason = "jvm",
                        ),
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
            assertEquals(0, report.riskScore)
        }

    @Test
    fun detectorCountExposesAttachedDetectors() {
        val guard =
            DeviceGuard
                .Builder(testContext())
                .addDetector(stubDetector<Unit>(id = "a", result = DetectionResult.NotApplicable("a")))
                .addDetector(stubDetector<Unit>(id = "b", result = DetectionResult.NotApplicable("b")))
                .build()
        assertEquals(2, guard.detectorCount)
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

    private companion object {
        private const val FIXED_EPOCH = 1_700_000_000_000L
    }
}
