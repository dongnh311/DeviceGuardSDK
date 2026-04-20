package io.github.dongnh311.deviceguard.remote

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

/**
 * JS platforms cannot enumerate installed native apps, running processes, or screen
 * capture state from inside a browser sandbox. The detector reports itself as not
 * applicable — consumers should rely on `Emulator` (via `navigator.webdriver`) and
 * server-side session correlation instead.
 */
internal actual suspend fun runRemoteCheck(context: DeviceGuardContext): RemoteCheckOutcome =
    RemoteCheckOutcome(
        applicable = false,
        reason = "remote detection is not supported in browser JS — sandbox blocks enumeration",
    )
