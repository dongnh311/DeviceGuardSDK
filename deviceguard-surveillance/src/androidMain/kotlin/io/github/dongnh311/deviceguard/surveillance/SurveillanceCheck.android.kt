package io.github.dongnh311.deviceguard.surveillance

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import io.github.dongnh311.deviceguard.core.DeviceGuardContext

private const val WEIGHT_A11Y_SERVICE = 1.0f
private const val WEIGHT_NOTIFICATION_LISTENER = 1.0f
private const val WEIGHT_DEVICE_ADMIN = 1.0f
private const val WEIGHT_SUSPICIOUS_IME = 1.0f

/**
 * Android surveillance detection.
 *
 * Signals:
 * - Enabled accessibility services → each non-system package that ships one.
 *   Weight 1.0 per package. Read via `AccessibilityManager.getEnabledAccessibilityServiceList`.
 * - Enabled notification listeners → `NotificationManagerCompat.getEnabledListenerPackages`
 *   + filter non-system. Weight 1.0 per package.
 * - Active device admins → `DevicePolicyManager.getActiveAdmins` + filter non-system.
 *   Weight 1.0 per admin.
 * - Default input method → `Settings.Secure.DEFAULT_INPUT_METHOD`; flag when the
 *   package is non-system. Weight 1.0 if suspicious.
 *
 * `OverlayPermission` and `UsageStatsGranted` are intentionally skipped on Android —
 * Android provides no API to enumerate holders of `SYSTEM_ALERT_WINDOW` or
 * `PACKAGE_USAGE_STATS` from a non-privileged app. Consumers that need those can
 * subclass the detector.
 */
internal actual suspend fun runSurveillanceCheck(context: DeviceGuardContext): SurveillanceOutcome {
    val indicators = mutableListOf<SurveillanceIndicator>()

    val ctx = context.androidContext
    indicators += runCatching { accessibilityIndicators(ctx) }.getOrDefault(emptyList())
    indicators += runCatching { notificationListenerIndicators(ctx) }.getOrDefault(emptyList())
    indicators += runCatching { deviceAdminIndicators(ctx) }.getOrDefault(emptyList())
    indicators += runCatching { suspiciousImeIndicator(ctx) }.getOrNull()?.let(::listOf).orEmpty()

    return SurveillanceOutcome(applicable = true, indicators = indicators)
}

private fun accessibilityIndicators(context: Context): List<SurveillanceIndicator> {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return emptyList()
    val enabled = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    val hits = mutableListOf<SurveillanceIndicator>()
    for (service in enabled) {
        val pkg = service.resolveInfo?.serviceInfo?.packageName ?: continue
        if (!isSystemPackage(pkg)) {
            hits += SurveillanceIndicator(SurveillanceCategory.AccessibilityAbuse, "a11y:$pkg", WEIGHT_A11Y_SERVICE)
        }
    }
    return hits
}

private fun notificationListenerIndicators(context: Context): List<SurveillanceIndicator> {
    // Settings.Secure.ENABLED_NOTIFICATION_LISTENERS is a `:`-separated list of
    // "packageName/ServiceClass" entries. Stable across all Android versions (API 19+).
    val raw = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return emptyList()
    if (raw.isBlank()) return emptyList()
    return raw
        .split(':')
        .mapNotNull { it.substringBefore('/').trim().takeIf(String::isNotEmpty) }
        .distinct()
        .filter { !isSystemPackage(it) }
        .map { SurveillanceIndicator(SurveillanceCategory.NotificationListener, "notif_listener:$it", WEIGHT_NOTIFICATION_LISTENER) }
}

private fun deviceAdminIndicators(context: Context): List<SurveillanceIndicator> {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return emptyList()
    val admins = dpm.activeAdmins.orEmpty()
    return admins
        .map { it.packageName }
        .filter { !isSystemPackage(it) }
        .map { SurveillanceIndicator(SurveillanceCategory.DeviceAdminActive, "device_admin:$it", WEIGHT_DEVICE_ADMIN) }
}

private fun suspiciousImeIndicator(context: Context): SurveillanceIndicator? {
    val defaultIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: return null
    // DEFAULT_INPUT_METHOD format is "packageName/.ServiceClass" — extract package only.
    val pkg = defaultIme.substringBefore('/').trim()
    if (pkg.isEmpty() || isSystemPackage(pkg)) return null
    return SurveillanceIndicator(SurveillanceCategory.SuspiciousIme, "ime:$pkg", WEIGHT_SUSPICIOUS_IME)
}
