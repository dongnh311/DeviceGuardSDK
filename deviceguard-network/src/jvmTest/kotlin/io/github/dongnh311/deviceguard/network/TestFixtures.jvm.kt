package io.github.dongnh311.deviceguard.network

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

internal actual fun fakeContext(): DeviceGuardContext = DeviceGuardContext()
