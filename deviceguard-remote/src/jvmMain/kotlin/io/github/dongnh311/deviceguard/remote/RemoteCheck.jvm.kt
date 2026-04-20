package io.github.dongnh311.deviceguard.remote

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.InternalDeviceGuardApi
import io.github.dongnh311.deviceguard.core.scanJvmProcessBasenames

private const val WEIGHT_PROCESS_RUNNING = 1.0f
private const val WEIGHT_SCREENSHARING_DAEMON = 0.8f
private const val SCREENSHARING_DAEMON = "screensharingd"

/**
 * Desktop JVM remote-control detection.
 *
 * Delegates process enumeration to [scanJvmProcessBasenames] — a shared helper that
 * walks `ProcessHandle.allProcesses()` once against a caller-provided name set. Two
 * axes share the lookup: every match contributes to `installedIndicators`; the macOS
 * `screensharingd` system daemon additionally contributes to `captureIndicators` so
 * the detector can fire `ScreenBeingCaptured` when the built-in Screen Sharing is live.
 */
@OptIn(InternalDeviceGuardApi::class)
internal actual suspend fun runRemoteCheck(context: DeviceGuardContext): RemoteCheckOutcome {
    val hits = runCatching { scanJvmProcessBasenames(KNOWN_REMOTE_PROCESSES.toSet()) }.getOrDefault(emptySet())

    val installed = hits.map { RemoteIndicator("remote_process:$it", WEIGHT_PROCESS_RUNNING) }
    val capture =
        if (SCREENSHARING_DAEMON in hits) {
            listOf(RemoteIndicator("screensharing_daemon:$SCREENSHARING_DAEMON", WEIGHT_SCREENSHARING_DAEMON))
        } else {
            emptyList()
        }

    return RemoteCheckOutcome(applicable = true, installedIndicators = installed, captureIndicators = capture)
}
