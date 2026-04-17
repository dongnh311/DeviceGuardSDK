package io.github.dongnh311.deviceguard.core

import kotlinx.serialization.Serializable

/**
 * Bucketed interpretation of [SecurityReport.riskScore].
 *
 * Buckets are non-overlapping and cover `0..100` exhaustively so that every score maps to
 * exactly one level. Consumers can use [fromScore] to derive a level from a raw integer and
 * [matches] to test membership.
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
    public fun matches(score: Int): Boolean = score in scoreRange(this)

    public companion object {
        /** Map a raw score (clamped to `0..100`) to the corresponding level. */
        public fun fromScore(score: Int): RiskLevel {
            val clamped = score.coerceIn(MIN_SCORE, MAX_SCORE)
            return entries.first { clamped in scoreRange(it) }
        }

        private const val MIN_SCORE = 0
        private const val MAX_SCORE = 100

        private fun scoreRange(level: RiskLevel): IntRange =
            when (level) {
                SAFE -> 0..19
                LOW -> 20..39
                MEDIUM -> 40..59
                HIGH -> 60..79
                CRITICAL -> 80..100
            }
    }
}
