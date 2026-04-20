package io.github.dongnh311.deviceguard.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

private const val DEFAULT_OBSERVE_PERIOD_MS: Long = 5_000L
private const val MIN_OBSERVE_PERIOD_MS: Long = 500L

/**
 * Emit a fresh [SecurityReport] every [periodMs] milliseconds.
 *
 * The flow re-runs [DeviceGuard.analyze] on each tick and forwards the result if the set
 * of threats or the fingerprint changed since the previous emission — consumers don't see
 * updates when nothing meaningful has moved.
 *
 * Collection is cooperative: cancelling the collector stops further polling. Errors from
 * individual detectors are captured on [SecurityReport.errors]; uncaught exceptions
 * propagate through the flow.
 *
 * The first emission happens immediately on subscription (no leading [delay]). Subsequent
 * emissions are spaced by [periodMs] from the end of the previous `analyze()` — so a slow
 * analysis naturally back-pressures the next tick instead of stacking.
 *
 * @param periodMs minimum milliseconds between ticks. Minimum supported is 500 ms — the
 *   underlying detectors have a 200 ms p95 budget and tighter polling would leave no
 *   headroom for the framework's own work. Default 5 s, safe for UI consumers.
 */
public fun DeviceGuard.observe(periodMs: Long = DEFAULT_OBSERVE_PERIOD_MS): Flow<SecurityReport> {
    require(periodMs >= MIN_OBSERVE_PERIOD_MS) { "periodMs must be >= $MIN_OBSERVE_PERIOD_MS" }
    return flow {
        while (true) {
            emit(analyze())
            delay(periodMs)
        }
    }.distinctUntilChanged { old, new -> old.sameSignal(new) }
}

/**
 * Equality check used by [observe]'s `distinctUntilChanged` — two reports are "the same
 * signal" when they report the same threats and the same fingerprint. `analyzedAtEpochMillis`
 * and `signals` are intentionally excluded because they churn every tick without reflecting
 * a security state change.
 */
private fun SecurityReport.sameSignal(other: SecurityReport): Boolean =
    threats == other.threats &&
        fingerprint?.id == other.fingerprint?.id &&
        errors == other.errors
