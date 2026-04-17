package io.github.dongnh311.deviceguard.core

/**
 * Sink for diagnostic messages emitted while analyzing a device.
 *
 * Implementations must be thread-safe; [DeviceGuard.analyze] may call [log] from any coroutine
 * dispatcher. Use [NoOp] when you want to silence output entirely, or [Println] during local
 * debugging. Override [isEnabled] to cheaply filter levels — the orchestrator checks it before
 * building each message.
 */
public interface DeviceGuardLogger {
    /** Returns `false` if events at [level] should be skipped entirely. */
    public fun isEnabled(level: LogLevel): Boolean = true

    /** Log a single event. [error] is non-null only for levels that carry a throwable. */
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
                override fun isEnabled(level: LogLevel): Boolean = false

                override fun log(
                    level: LogLevel,
                    tag: String,
                    message: String,
                    error: Throwable?,
                ) = Unit
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
