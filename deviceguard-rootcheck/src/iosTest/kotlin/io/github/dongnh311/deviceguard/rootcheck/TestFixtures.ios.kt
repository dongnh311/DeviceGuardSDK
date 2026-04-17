package io.github.dongnh311.deviceguard.rootcheck

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

internal actual fun fakeContext(): DeviceGuardContext = DeviceGuardContext()
