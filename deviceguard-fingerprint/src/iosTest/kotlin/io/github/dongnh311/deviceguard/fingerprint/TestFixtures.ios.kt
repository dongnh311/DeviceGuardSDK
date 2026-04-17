package io.github.dongnh311.deviceguard.fingerprint

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

internal actual fun fakeContext(): DeviceGuardContext = DeviceGuardContext()
