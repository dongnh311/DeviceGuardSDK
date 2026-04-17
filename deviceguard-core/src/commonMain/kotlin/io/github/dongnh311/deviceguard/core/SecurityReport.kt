package io.github.dongnh311.deviceguard.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Aggregated result of a DeviceGuard [DeviceGuard.analyze] run.
 *
 * `SecurityReport` is the primary consumer-facing payload: it collapses every detector's
 * contribution into a single risk score, a bucketed [riskLevel], a flat list of [threats]
 * suitable for display, and the raw [signals] useful for telemetry.
 *
 * @property riskScore aggregated risk in `0..100`. Produced by the [RiskScoring] strategy.
 * @property threats threats surfaced by detectors, in the order they were produced.
 * @property fingerprint stable device identifier, if the fingerprint detector ran.
 * @property signals flat map of detector-emitted raw signals, keyed by signal name.
 * @property errors detector invocations that failed; excluded from [riskScore] computation.
 * @property analyzedAtEpochMillis wall-clock time of the analysis, epoch milliseconds.
 * @property schemaVersion schema version of this payload. Bumped when the JSON layout
 *   changes in a way that old parsers cannot handle.
 */
@Serializable
public data class SecurityReport(
    public val riskScore: Int,
    public val threats: List<DetectedThreat>,
    public val fingerprint: DeviceFingerprint? = null,
    public val signals: Map<String, String> = emptyMap(),
    public val errors: List<DetectorError> = emptyList(),
    public val analyzedAtEpochMillis: Long,
    public val schemaVersion: Int = DeviceGuardVersion.REPORT_SCHEMA,
) {
    /** Bucketed interpretation of [riskScore]. Always derived — never stored or serialized. */
    public val riskLevel: RiskLevel get() = RiskLevel.fromScore(riskScore)

    /** Serialize to canonical JSON. */
    public fun toJson(pretty: Boolean = false): String = (if (pretty) PrettyJson else DefaultJson).encodeToString(serializer(), this)

    public companion object {
        private val DefaultJson =
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                explicitNulls = false
            }

        private val PrettyJson =
            Json(from = DefaultJson) {
                prettyPrint = true
            }

        /** Parse a report previously produced by [toJson]. */
        public fun fromJson(json: String): SecurityReport = DefaultJson.decodeFromString(serializer(), json)
    }
}
