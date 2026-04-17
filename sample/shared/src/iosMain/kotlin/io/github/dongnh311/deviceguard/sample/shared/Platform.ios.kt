package io.github.dongnh311.deviceguard.sample.shared

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

internal actual val platformName: String = "iOS"

internal actual fun createDeviceGuardContext(): DeviceGuardContext = DeviceGuardContext()
