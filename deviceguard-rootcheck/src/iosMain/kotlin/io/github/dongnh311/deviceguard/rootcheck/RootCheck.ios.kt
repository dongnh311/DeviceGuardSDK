@file:OptIn(ExperimentalForeignApi::class)

package io.github.dongnh311.deviceguard.rootcheck

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.ThreatType
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSUUID

private const val WEIGHT_JAILBREAK_PATH = 0.9f
private const val WEIGHT_SANDBOX_ESCAPE = 1.0f

/**
 * iOS jailbreak detection.
 *
 * Probes two independent sources:
 * - **Jailbreak path existence:** Cydia, Sileo, apt cache, `/bin/bash`, `/usr/sbin/sshd` and
 *   similar artifacts that sandboxed apps cannot see on a stock device. Weight 0.9 each —
 *   multiple hits saturate to `1.0` via the detector's clamp.
 * - **Sandbox escape write probe:** creates an empty file under `/private/` with a random
 *   name and removes it. A sandboxed app cannot write outside its container; success proves
 *   the sandbox is broken. Weight 1.0. Cleanup runs in a `finally` block so a throw
 *   between create and remove can't leak probe files onto the device.
 *
 * No `fork()` probe in this phase — reliable child-process cleanup on iOS is fragile and
 * the path/write signals already saturate confidence.
 */
internal actual suspend fun runRootCheck(context: DeviceGuardContext): RootCheckOutcome {
    val indicators = mutableListOf<RootIndicator>()
    val fileManager = NSFileManager.defaultManager

    for (path in JAILBREAK_PATHS) {
        if (fileManager.fileExistsAtPath(path)) {
            indicators += RootIndicator("jailbreak_path:$path", WEIGHT_JAILBREAK_PATH)
        }
    }

    val probePath = "/private/DeviceGuardProbe-${NSUUID().UUIDString}.txt"
    val wrote =
        runCatching {
            fileManager.createFileAtPath(probePath, contents = null, attributes = null)
        }.getOrDefault(false)
    try {
        if (wrote) indicators += RootIndicator("sandbox_escape:$probePath", WEIGHT_SANDBOX_ESCAPE)
    } finally {
        if (wrote) runCatching { fileManager.removeItemAtPath(probePath, null) }
    }

    return RootCheckOutcome(
        applicable = true,
        threatType = ThreatType.Jailbreak,
        indicators = indicators,
    )
}

private val JAILBREAK_PATHS =
    listOf(
        "/Applications/Cydia.app",
        "/Applications/Sileo.app",
        "/Applications/Zebra.app",
        "/Library/MobileSubstrate/MobileSubstrate.dylib",
        "/bin/bash",
        "/usr/sbin/sshd",
        "/etc/apt",
        "/private/var/lib/apt",
        "/private/var/lib/cydia",
        "/private/var/mobile/Library/SBSettings/Themes",
        "/private/var/stash",
        "/usr/bin/sshd",
        "/usr/libexec/sftp-server",
        "/usr/libexec/ssh-keysign",
    )
