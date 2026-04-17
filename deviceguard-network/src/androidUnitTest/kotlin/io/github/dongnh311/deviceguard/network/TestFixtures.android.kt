package io.github.dongnh311.deviceguard.network

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.mockk.mockk

internal actual fun fakeContext(): DeviceGuardContext = DeviceGuardContext(mockk(relaxed = true))
