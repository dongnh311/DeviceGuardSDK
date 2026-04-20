package io.github.dongnh311.deviceguard.remote

import android.content.Context
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityManager
import io.github.dongnh311.deviceguard.core.DeviceGuardContext

private const val WEIGHT_PACKAGE_INSTALLED = 1.0f
private const val WEIGHT_ACCESSIBILITY_REMOTE = 0.9f

/**
 * Android remote-control detection.
 *
 * Signals:
 * - `PackageManager.getPackageInfo()` succeeds for a known remote-app package ID
 *   (weight 1.0). Requires the `<queries>` manifest block declared in this module's
 *   AndroidManifest.xml — on Android 11+ other packages are invisible by default.
 * - An enabled accessibility service has a package ID on the known-remote list
 *   (weight 0.9). Remote-control apps lean heavily on a11y to inject taps and read
 *   screen content.
 *
 * Screen-capture detection on Android (MediaProjection token ownership) requires an
 * active foreground service and cross-process inspection that sandboxed apps cannot
 * perform — skipped here, covered by iOS in Phase 10a.
 */
internal actual suspend fun runRemoteCheck(context: DeviceGuardContext): RemoteCheckOutcome {
    val installed = mutableListOf<RemoteIndicator>()

    installed += runCatching { scanInstalledPackages(context.androidContext) }.getOrDefault(emptyList())
    installed += runCatching { scanAccessibilityServices(context.androidContext) }.getOrDefault(emptyList())

    return RemoteCheckOutcome(applicable = true, installedIndicators = installed)
}

private fun scanInstalledPackages(context: Context): List<RemoteIndicator> {
    val pm = context.packageManager
    val hits = mutableListOf<RemoteIndicator>()
    for (pkg in KNOWN_REMOTE_PACKAGES) {
        val present =
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        if (present) hits += RemoteIndicator("remote_pkg:$pkg", WEIGHT_PACKAGE_INSTALLED)
    }
    return hits
}

private fun scanAccessibilityServices(context: Context): List<RemoteIndicator> {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return emptyList()
    val enabled = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    val hits = mutableListOf<RemoteIndicator>()
    for (service in enabled) {
        val pkg = service.resolveInfo?.serviceInfo?.packageName ?: continue
        if (pkg in KNOWN_REMOTE_PACKAGES) {
            hits += RemoteIndicator("remote_a11y:$pkg", WEIGHT_ACCESSIBILITY_REMOTE)
        }
    }
    return hits
}
