package io.github.dongnh311.deviceguard.remote

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import platform.UIKit.UIScreen

private const val WEIGHT_SCREEN_CAPTURED = 1.0f

/**
 * iOS remote-control detection.
 *
 * iOS sandbox blocks installed-app enumeration (`LSApplicationQueriesSchemes` requires
 * fixed URL schemes declared upfront and doesn't report "not installed" reliably). Process
 * enumeration is likewise unavailable. The only reliable signal is `UIScreen.mainScreen.isCaptured`
 * which returns `true` when the screen is being mirrored to AirPlay, recorded via Control
 * Center, or shared through ReplayKit / CallKit video.
 *
 * The `remoteControlInstalled` axis therefore always stays `false` on iOS; the detector
 * only contributes to `ScreenBeingCaptured`.
 */
internal actual suspend fun runRemoteCheck(context: DeviceGuardContext): RemoteCheckOutcome {
    val capture = mutableListOf<RemoteIndicator>()
    runCatching {
        if (UIScreen.mainScreen.captured) {
            capture += RemoteIndicator("screen_captured:UIScreen.isCaptured", WEIGHT_SCREEN_CAPTURED)
        }
    }
    return RemoteCheckOutcome(applicable = true, captureIndicators = capture)
}
