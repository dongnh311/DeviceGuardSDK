package io.github.dongnh311.deviceguard.surveillance

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.ThreatType

/** A single surveillance / tampering signal fired on the host platform. */
internal data class SurveillanceIndicator(
    val category: SurveillanceCategory,
    val name: String,
    val weight: Float,
)

/** Category buckets — each maps to exactly one [ThreatType] the detector emits. */
internal enum class SurveillanceCategory(
    val threatType: ThreatType,
) {
    AccessibilityAbuse(ThreatType.AccessibilityAbuse),
    OverlayPermission(ThreatType.OverlayPermission),
    NotificationListener(ThreatType.NotificationListener),
    DeviceAdminActive(ThreatType.DeviceAdminActive),
    SuspiciousIme(ThreatType.SuspiciousIme),
    UsageStatsGranted(ThreatType.UsageStatsGranted),
    AutomationToolRunning(ThreatType.AutomationToolRunning),
    DebuggerAttachedElsewhere(ThreatType.DebuggerAttachedElsewhere),
}

/**
 * Outcome of platform-specific surveillance probing.
 *
 * Android contributes to every category. Desktop JVM contributes mainly to
 * `AutomationToolRunning` and `DebuggerAttachedElsewhere`. iOS/Web are
 * `applicable = false`.
 */
internal data class SurveillanceOutcome(
    val applicable: Boolean,
    val reason: String? = null,
    val indicators: List<SurveillanceIndicator> = emptyList(),
)

/** Platform entry point. Implementations must never throw — catch and return no indicators. */
internal expect suspend fun runSurveillanceCheck(context: DeviceGuardContext): SurveillanceOutcome

/** Package-name prefixes treated as first-party / system — never flagged. */
internal val SYSTEM_PACKAGE_PREFIXES: List<String> =
    listOf(
        "android",
        "com.android",
        "com.google.android",
        "com.samsung",
        "com.sec.",
        "com.miui",
        "com.xiaomi",
        "com.huawei",
        "com.oppo",
        "com.vivo",
        "com.oneplus",
    )

internal fun isSystemPackage(pkg: String): Boolean = SYSTEM_PACKAGE_PREFIXES.any { pkg == it || pkg.startsWith("$it.") }

/**
 * Desktop process names that can synthesize input or attach to other processes. Matched
 * case-insensitively against the process basename.
 */
internal val KNOWN_AUTOMATION_PROCESSES: List<String> =
    listOf(
        "autohotkey",
        "autohotkey64",
        "ahk",
        "hammerspoon",
        "keyboard maestro engine",
        "keyboardmaestroengine",
        "bettertouchtool",
        "karabiner-elements",
        "karabiner_grabber",
        "keysmith",
        "xdotool",
    )

/** Known debuggers / instrumentation tools that attach to other processes. */
internal val KNOWN_DEBUGGERS: List<String> =
    listOf(
        "gdb",
        "lldb",
        "x64dbg",
        "x32dbg",
        "ollydbg",
        "ida",
        "ida64",
        "cheatengine",
        "cheatengine-x86_64",
        "frida-server",
        "frida",
        "frida-trace",
    )
