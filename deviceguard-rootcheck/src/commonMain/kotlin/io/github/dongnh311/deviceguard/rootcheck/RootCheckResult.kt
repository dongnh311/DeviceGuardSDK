package io.github.dongnh311.deviceguard.rootcheck

import kotlinx.serialization.Serializable

/**
 * Typed payload emitted by [RootCheckDetector] on [io.github.dongnh311.deviceguard.core.DetectionResult.Success.data].
 *
 * @property isRooted derived from [confidence] vs the detector's threshold.
 * @property confidence clamped to `0f..1f`. Aggregates the weights of every fired indicator.
 * @property indicators opaque names of the signals that fired (`su_binary:/system/bin/su`,
 *   `package:com.topjohnwu.magisk`, `build_tag:test-keys`, `jailbreak_path:/Applications/Cydia.app`
 *   and similar). Useful for forensic logging; never contains PII.
 */
@Serializable
public data class RootCheckResult(
    public val isRooted: Boolean,
    public val confidence: Float,
    public val indicators: List<String>,
) {
    init {
        require(confidence in 0f..1f) { "confidence must be within 0f..1f" }
    }
}
