package io.github.dongnh311.deviceguard.core

/**
 * Outcome of a single [Detector.detect] invocation.
 *
 * The generic parameter [T] is the detector-specific payload (e.g., `Boolean` for root checks,
 * [DeviceFingerprint] for fingerprinting). The orchestrator only reads [threats], [signals],
 * and the [Success.data] payload when assembling the final [SecurityReport].
 */
public sealed interface DetectionResult<out T> {
    /** Identifier of the detector that produced this result. Matches [Detector.id]. */
    public val detectorId: String

    /**
     * Detector ran successfully.
     *
     * @property data detector-specific payload.
     * @property threats threats surfaced, if any. Empty when the detector considers the device
     *   healthy.
     * @property signals raw signals collected; merged into [SecurityReport.signals].
     */
    public data class Success<T>(
        override val detectorId: String,
        public val data: T,
        public val threats: List<DetectedThreat> = emptyList(),
        public val signals: Map<String, String> = emptyMap(),
    ) : DetectionResult<T>

    /**
     * Detector does not apply on this platform (e.g., root checks on JVM).
     *
     * Does not contribute to the risk score and does not count as an error.
     */
    public data class NotApplicable(
        override val detectorId: String,
        public val reason: String? = null,
    ) : DetectionResult<Nothing>

    /** Detector threw or returned an unrecoverable error. Surfaces on [SecurityReport.errors]. */
    public data class Failed(
        override val detectorId: String,
        public val message: String,
        public val errorType: String? = null,
    ) : DetectionResult<Nothing>
}
