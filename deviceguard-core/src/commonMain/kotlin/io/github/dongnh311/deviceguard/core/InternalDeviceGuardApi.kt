package io.github.dongnh311.deviceguard.core

/**
 * Marks a DeviceGuard symbol as shared across `deviceguard-*` modules but not part of the
 * consumer-facing public API. Binary-compatible across patch releases; removable across
 * minor releases. Opt in only inside the SDK's own modules; library consumers should not.
 */
@RequiresOptIn(
    message = "This DeviceGuard symbol is internal to the SDK's own modules and is not a supported consumer API.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
)
public annotation class InternalDeviceGuardApi
