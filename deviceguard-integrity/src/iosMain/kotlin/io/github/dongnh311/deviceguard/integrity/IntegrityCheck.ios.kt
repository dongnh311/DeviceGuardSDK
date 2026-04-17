package io.github.dongnh311.deviceguard.integrity

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager

private const val WEIGHT_FRIDA_ARTIFACT = 1.0f
private const val WEIGHT_NO_BUNDLE_ID = 0.6f

/**
 * iOS integrity detection.
 *
 * Hook signals:
 * - Filesystem check for known Frida artefacts across traditional and rootless (Dopamine /
 *   palera1n, iOS 15+) jailbreak layouts: FridaGadget framework, `frida-agent` /
 *   `frida-gadget` dylibs under `/usr/lib/frida/` and `/var/jb/usr/lib/frida/`,
 *   `frida-server`, Cycript. A stock sandboxed app on a signed device cannot see these
 *   paths; a hit is a strong instrumentation indicator. Weight 1.0 per match.
 *
 * Signature signals:
 * - `NSBundle.mainBundle.bundleIdentifier == nil` (weight 0.6). A correctly-signed build
 *   always has an identifier; absence suggests a wrapper or an unsigned build.
 *
 * `signatureCheckRun` is always `true` on iOS — at minimum the bundle-identifier probe
 * always executes. Full code-signature verification via `SecStaticCodeCheckValidity`,
 * in-memory Frida agent scanning via `_dyld_image_count` (requires custom cinterop), and
 * re-signing detection via `embedded.mobileprovision` inspection are deferred — they need
 * more cinterop surface and benefit from validation against real re-signed builds.
 * [IntegrityCheckConfig.expectedSignatureSha256] is Android-only and ignored here.
 */
internal actual suspend fun runIntegrityCheck(
    context: DeviceGuardContext,
    config: IntegrityCheckConfig,
): IntegrityCheckOutcome {
    val hook = mutableListOf<IntegrityIndicator>()
    val signature = mutableListOf<IntegrityIndicator>()
    val fileManager = NSFileManager.defaultManager

    for (path in FRIDA_ARTIFACT_PATHS) {
        if (fileManager.fileExistsAtPath(path)) {
            hook += IntegrityIndicator("frida_artifact:$path", WEIGHT_FRIDA_ARTIFACT)
        }
    }

    if (NSBundle.mainBundle.bundleIdentifier == null) {
        signature += IntegrityIndicator("no_bundle_identifier", WEIGHT_NO_BUNDLE_ID)
    }

    return IntegrityCheckOutcome(
        applicable = true,
        signatureCheckRun = true,
        signatureIndicators = signature,
        hookIndicators = hook,
    )
}

private val FRIDA_ARTIFACT_PATHS =
    listOf(
        // Traditional jailbreak layouts.
        "/Library/Frameworks/FridaGadget.framework",
        "/usr/lib/frida/frida-agent.dylib",
        "/usr/lib/frida/frida-gadget.dylib",
        "/usr/sbin/frida-server",
        "/usr/bin/cycript",
        // Rootless jailbreak layouts (Dopamine / palera1n on iOS 15+).
        "/var/jb/usr/lib/frida/frida-agent.dylib",
        "/var/jb/usr/lib/frida/frida-gadget.dylib",
        "/var/jb/usr/sbin/frida-server",
        "/var/jb/usr/bin/cycript",
    )
