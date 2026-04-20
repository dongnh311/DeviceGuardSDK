package io.github.dongnh311.deviceguard.remote

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import java.util.Locale

private const val WEIGHT_PROCESS_RUNNING = 1.0f
private const val WEIGHT_SCREENSHARING_DAEMON = 0.8f

/**
 * Desktop JVM remote-control detection.
 *
 * Enumerates running processes via `ProcessHandle.allProcesses()` (JDK 9+). Each live
 * process's command basename is lower-cased and compared against [KNOWN_REMOTE_PROCESSES].
 *
 * On macOS, the presence of `screensharingd` is treated as a screen-capture signal too —
 * it's the system daemon that the built-in Screen Sharing app ships out to.
 */
internal actual suspend fun runRemoteCheck(context: DeviceGuardContext): RemoteCheckOutcome {
    val installed = mutableListOf<RemoteIndicator>()
    val capture = mutableListOf<RemoteIndicator>()

    val processHits = runCatching { scanProcesses() }.getOrDefault(emptyList())
    for (hit in processHits) {
        installed += RemoteIndicator("remote_process:${hit.name}", WEIGHT_PROCESS_RUNNING)
        if (hit.name == "screensharingd") {
            capture += RemoteIndicator("screensharing_daemon:${hit.name}", WEIGHT_SCREENSHARING_DAEMON)
        }
    }

    return RemoteCheckOutcome(applicable = true, installedIndicators = installed, captureIndicators = capture)
}

private data class ProcessHit(
    val name: String,
)

private fun scanProcesses(): List<ProcessHit> {
    val hits = mutableListOf<ProcessHit>()
    ProcessHandle.allProcesses().use { stream ->
        stream.forEach { handle ->
            val info = handle.info()
            val command = info.command().orElse(null) ?: return@forEach
            val basename =
                command
                    .substringAfterLast('/')
                    .substringAfterLast('\\')
                    .removeSuffix(".exe")
                    .lowercase(Locale.US)
            if (basename in KNOWN_REMOTE_PROCESSES) hits += ProcessHit(basename)
        }
    }
    return hits
}
