package io.github.dongnh311.deviceguard.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SecurityReportSerializationTest {
    @Test
    fun roundTripPreservesFields() {
        val report =
            SecurityReport(
                riskScore = 65,
                riskLevel = RiskLevel.HIGH,
                threats =
                    listOf(
                        DetectedThreat.of(
                            threat = ThreatType.Root,
                            confidence = 0.9f,
                            indicators = listOf("su_present", "build_tags_test_keys"),
                        ),
                    ),
                fingerprint =
                    DeviceFingerprint(
                        id = "abc123",
                        signals = mapOf("manufacturer" to "Pixel"),
                    ),
                signals = mapOf("sdk" to "34"),
                errors =
                    listOf(
                        DetectorError(
                            detectorId = "network",
                            message = "denied",
                            errorType = "SecurityException",
                        ),
                    ),
                analyzedAtEpochMillis = 1_700_000_000_000L,
            )

        val json = report.toJson()
        val parsed = SecurityReport.fromJson(json)

        assertEquals(report, parsed)
    }

    @Test
    fun prettyJsonIsMultilineAndContainsFields() {
        val report =
            SecurityReport(
                riskScore = 0,
                riskLevel = RiskLevel.SAFE,
                threats = emptyList(),
                analyzedAtEpochMillis = 0L,
            )
        val pretty = report.toJson(pretty = true)
        assertTrue(pretty.contains("\n"), "pretty JSON should be multiline")
        assertTrue(pretty.contains("\"riskScore\""), "pretty JSON should contain field names")
    }
}
