package io.github.dongnh311.deviceguard.core

/**
 * Sink for diagnostic messages emitted while analyzing a device.
 *
 * Implementations must be thread-safe; [DeviceGuard.analyze] may call [log] from any coroutine
 * dispatcher. Use [NoOp] when you want to silence output entirely, or [Println] during local
 * debugging.
 */
public interface DeviceGuardLogger {
    /**
     * Log a single event.
     *
     * @param level severity of the event.
     * @param tag short identifier for the source component.
     * @param message human-readable message.
     * @param error optional throwable associated with the event.
     */
    public fun log(
        level: LogLevel,
        tag: String,
        message: String,
        error: Throwable? = null,
    )

    /** Severity levels, ordered from most verbose to most severe. */
    public enum class LogLevel {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
    }

    public companion object {
        /** Logger that discards every event. Default for release builds. */
        public val NoOp: DeviceGuardLogger =
            object : DeviceGuardLogger {
                override fun log(
                    level: LogLevel,
                    tag: String,
                    message: String,
                    error: Throwable?,
                ) {
                    // Intentionally empty.
                }
            }

        /** Logger that routes every event through [println]. Useful for sample apps and tests. */
        public val Println: DeviceGuardLogger =
            object : DeviceGuardLogger {
                override fun log(
                    level: LogLevel,
                    tag: String,
                    message: String,
                    error: Throwable?,
                ) {
                    val suffix = error?.let { " — ${it::class.simpleName}: ${it.message}" } ?: ""
                    println("[${level.name}] $tag: $message$suffix")
                }
            }
    }
}
