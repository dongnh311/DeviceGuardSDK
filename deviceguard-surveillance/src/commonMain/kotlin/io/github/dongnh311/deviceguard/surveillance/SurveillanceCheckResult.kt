package io.github.dongnh311.deviceguard.surveillance

import kotlinx.serialization.Serializable

/**
 * Typed payload emitted by [SurveillanceCheckDetector] on
 * [io.github.dongnh311.deviceguard.core.DetectionResult.Success.data].
 *
 * One flag per threat category the detector can fire. All confidences are clamped to
 * `0f..1f`.
 *
 * @property indicators forensic names of all fired signals, grouped by category prefix.
 *   Examples: `a11y:com.evil.banking.trojan`, `overlay:com.other.app`, `ime:com.some.keyboard`.
 */
@Serializable
public data class SurveillanceCheckResult(
    public val accessibilityAbuse: Boolean,
    public val overlayPermission: Boolean,
    public val notificationListener: Boolean,
    public val deviceAdminActive: Boolean,
    public val suspiciousIme: Boolean,
    public val usageStatsGranted: Boolean,
    public val automationToolRunning: Boolean,
    public val debuggerAttachedElsewhere: Boolean,
    public val indicators: List<String>,
)
