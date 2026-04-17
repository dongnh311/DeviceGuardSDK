package io.github.dongnh311.deviceguard.core

/**
 * JS/Browser actualization of [DeviceGuardContext].
 *
 * Browser detectors read `window.navigator` and `window.screen` directly; the context holds
 * no state. Construct with `DeviceGuardContext()`.
 */
public actual class DeviceGuardContext
