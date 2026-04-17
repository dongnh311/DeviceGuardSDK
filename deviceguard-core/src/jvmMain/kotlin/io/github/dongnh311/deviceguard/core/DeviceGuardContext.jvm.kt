package io.github.dongnh311.deviceguard.core

/**
 * JVM/Desktop actualization of [DeviceGuardContext].
 *
 * JVM detectors read `System` properties and `NetworkInterface` directly; the context holds
 * no state. Construct with `DeviceGuardContext()`.
 */
public actual class DeviceGuardContext
