package io.github.dongnh311.deviceguard.core

/**
 * A single security check that contributes to a [SecurityReport].
 *
 * Every module under `deviceguard-*` exposes at least one implementation. Detectors must be
 * coroutine-safe and should honor cancellation — the orchestrator runs them inside a
 * structured scope and may cancel pending work if the caller times out.
 *
 * @param T payload surfaced by [detect] on success. Typical values are `Boolean` for boolean
 *   checks, [DeviceFingerprint] for fingerprinting, or a detector-specific data class.
 */
public interface Detector<out T> {
    /** Stable identifier, used for keying signals, errors, and log messages. */
    public val id: String

    /** Short human-readable description shown in logs. */
    public val description: String
        get() = id

    /**
     * Execute the check.
     *
     * Implementations should catch recoverable platform errors and return
     * [DetectionResult.Failed]; uncaught throwables are caught by the orchestrator and
     * recorded on [SecurityReport.errors].
     */
    public suspend fun detect(context: DeviceGuardContext): DetectionResult<T>
}
