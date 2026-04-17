package io.github.dongnh311.deviceguard.core

/**
 * Marks a DeviceGuard API as experimental: signatures and behavior may change between minor
 * releases without deprecation notice.
 *
 * Opt in at the usage site with `@OptIn(ExperimentalDeviceGuardApi::class)` or at a module
 * boundary with `-opt-in=io.github.dongnh311.deviceguard.core.ExperimentalDeviceGuardApi`.
 */
@RequiresOptIn(
    message = "This DeviceGuard API is experimental and may change without notice.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.CONSTRUCTOR,
)
public annotation class ExperimentalDeviceGuardApi
