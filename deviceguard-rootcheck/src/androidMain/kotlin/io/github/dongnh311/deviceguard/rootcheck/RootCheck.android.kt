package io.github.dongnh311.deviceguard.rootcheck

import android.content.pm.PackageManager
import android.os.Build
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.ThreatType
import java.io.File

private const val WEIGHT_SU_BINARY = 1.0f
private const val WEIGHT_ROOT_PACKAGE = 0.9f
private const val WEIGHT_BUILD_TAGS = 0.3f

/**
 * Android root detection.
 *
 * Probes three independent sources:
 * - **Binary files:** known `su`/Superuser/Magisk paths, weight 1.0 each.
 * - **Installed packages:** Magisk, SuperSU, KingRoot and similar, weight 0.9 each. One
 *   `getInstalledPackages` IPC — cheaper than twelve `getPackageInfo` round-trips and side
 *   steps per-call `SecurityException`/`NameNotFoundException` handling.
 * - **Build tags:** `test-keys` in `Build.TAGS`, weight 0.3 — weak on its own but useful
 *   for corroboration.
 *
 * Weights are tuned so that a single direct indicator trips `isRooted` at the default
 * (lax) threshold of `0.5`, while strict mode (`0.2`) also trips on test-keys alone.
 *
 * The `ROOT_PACKAGES` list is mirrored in `src/androidMain/AndroidManifest.xml` under
 * `<queries>` so PackageManager can see them on Android 11+ without `QUERY_ALL_PACKAGES`.
 * Keep the two lists in sync.
 */
internal actual suspend fun runRootCheck(context: DeviceGuardContext): RootCheckOutcome {
    val indicators = mutableListOf<RootIndicator>()

    for (path in SU_BINARY_PATHS) {
        if (File(path).exists()) indicators += RootIndicator("su_binary:$path", WEIGHT_SU_BINARY)
    }

    val pm = context.androidContext.packageManager
    val installed = installedPackageNames(pm)
    for (pkg in ROOT_PACKAGES) {
        if (pkg in installed) indicators += RootIndicator("package:$pkg", WEIGHT_ROOT_PACKAGE)
    }

    val tags = Build.TAGS.orEmpty()
    if (tags.contains("test-keys")) indicators += RootIndicator("build_tags:$tags", WEIGHT_BUILD_TAGS)

    return RootCheckOutcome(
        applicable = true,
        threatType = ThreatType.Root,
        indicators = indicators,
    )
}

private fun installedPackageNames(pm: PackageManager): Set<String> =
    runCatching {
        pm.getInstalledPackages(0).mapTo(HashSet()) { it.packageName }
    }.getOrDefault(emptySet())

private val SU_BINARY_PATHS =
    listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su",
        "/system/etc/init.d/99SuperSUDaemon",
        "/system/xbin/daemonsu",
        "/system/bin/.ext/.su",
    )

// Keep in sync with <queries> in src/androidMain/AndroidManifest.xml.
private val ROOT_PACKAGES =
    listOf(
        "com.topjohnwu.magisk",
        "com.noshufou.android.su",
        "com.noshufou.android.su.elite",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser",
        "com.yellowes.su",
        "com.kingroot.kinguser",
        "com.kingo.root",
        "com.smedialink.oneclickroot",
        "com.zhiqupk.root.global",
        "com.alephzain.framaroot",
    )
