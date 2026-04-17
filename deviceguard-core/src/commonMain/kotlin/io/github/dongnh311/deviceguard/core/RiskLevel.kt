package io.github.dongnh311.deviceguard.core

import kotlinx.serialization.Serializable

/**
 * Bucketed interpretation of [SecurityReport.riskScore].
 *
 * Buckets are non-overlapping and cover `0..100` exhaustively so that every score maps to
 * exactly one level.
 */
@Serializable
public enum class RiskLevel {
    /** `0..19` — no meaningful threats detected. */
    SAFE,

    /** `20..39` — minor indicators. Acceptable for most use cases. */
    LOW,

    /** `40..59` — multiple indicators or one moderate indicator. Worth logging. */
    MEDIUM,

    /** `60..79` — clear evidence of tampering or high-risk environment. */
    HIGH,

    /** `80..100` — confident detection of rooted/jailbroken/tampered device. */
    CRITICAL,
    ;

    /** Returns `true` when [score] falls inside this level's bucket. */
    public fun matches(score: Int): Boolean = fromScore(score) == this

    public companion object {
        /** Map a raw score (clamped to `0..100`) to the corresponding level. */
        public fun fromScore(score: Int): RiskLevel =
            when (score.coerceIn(0, 100)) {
                in 0..19 -> SAFE
                in 20..39 -> LOW
                in 40..59 -> MEDIUM
                in 60..79 -> HIGH
                else -> CRITICAL
            }
    }
}
