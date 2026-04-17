package io.github.dongnh311.deviceguard.core

/**
 * iOS actualization of [DeviceGuardContext].
 *
 * iOS detectors reach for `UIDevice`, `Bundle.main`, and `sysctl` directly — no state is kept
 * on the context itself. Construct with `DeviceGuardContext()`.
 */
public actual class DeviceGuardContext
