package io.github.dongnh311.deviceguard.core

/**
 * Host-platform handle that detectors need to inspect the device.
 *
 * On Android this wraps `android.content.Context`. On iOS, JVM, and JS the type is a marker —
 * detectors reach for platform APIs directly — but the expect/actual pattern keeps common call
 * sites uniform.
 */
public expect class DeviceGuardContext
