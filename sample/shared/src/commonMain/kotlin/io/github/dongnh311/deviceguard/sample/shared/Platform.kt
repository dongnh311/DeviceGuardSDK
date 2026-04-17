package io.github.dongnh311.deviceguard.sample.shared

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

/** Short human-readable platform label shown in the UI header. */
internal expect val platformName: String

/**
 * Per-platform factory for the SDK context. Android needs a real Context; everything else
 * instantiates a marker. The App composable receives an already-constructed context so the
 * wrapper modules (android, desktop, web, ios) don't duplicate the detector wiring.
 */
internal expect fun createDeviceGuardContext(): DeviceGuardContext
