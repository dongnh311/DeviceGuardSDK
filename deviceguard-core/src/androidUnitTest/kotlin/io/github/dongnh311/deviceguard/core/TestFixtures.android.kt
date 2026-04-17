package io.github.dongnh311.deviceguard.core

import io.mockk.mockk

internal actual fun testContext(): DeviceGuardContext = DeviceGuardContext(mockk(relaxed = true))
