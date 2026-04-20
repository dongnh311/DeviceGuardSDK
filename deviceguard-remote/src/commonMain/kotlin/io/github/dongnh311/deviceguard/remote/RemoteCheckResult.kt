package io.github.dongnh311.deviceguard.remote

import kotlinx.serialization.Serializable

/**
 * Typed payload emitted by [RemoteCheckDetector] on
 * [io.github.dongnh311.deviceguard.core.DetectionResult.Success.data].
 *
 * @property remoteControlInstalled `true` when installed-app / running-process signals
 *   clear the detector's threshold.
 * @property screenBeingCaptured `true` when the screen is currently being mirrored,
 *   recorded, or shared (iOS `UIScreen.isCaptured`, desktop active-capture probes).
 * @property installedConfidence clamped to `0f..1f`.
 * @property captureConfidence clamped to `0f..1f`.
 * @property installedIndicators forensic names of fired installation signals (no PII).
 *   Examples: `remote_pkg:com.teamviewer.teamviewer.market.mobile`, `remote_process:anydesk`.
 * @property captureIndicators forensic names of fired capture signals. Example:
 *   `screen_captured:UIScreen.isCaptured`.
 */
@Serializable
public data class RemoteCheckResult(
    public val remoteControlInstalled: Boolean,
    public val screenBeingCaptured: Boolean,
    public val installedConfidence: Float,
    public val captureConfidence: Float,
    public val installedIndicators: List<String>,
    public val captureIndicators: List<String>,
) {
    init {
        require(installedConfidence in 0f..1f) { "installedConfidence must be within 0f..1f" }
        require(captureConfidence in 0f..1f) { "captureConfidence must be within 0f..1f" }
    }
}
