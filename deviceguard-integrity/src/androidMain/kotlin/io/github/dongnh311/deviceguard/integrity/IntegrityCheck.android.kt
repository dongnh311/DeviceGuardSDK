@file:OptIn(ExperimentalStdlibApi::class)

package io.github.dongnh311.deviceguard.integrity

import android.content.pm.PackageManager
import android.os.Build
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import org.kotlincrypto.hash.sha2.SHA256

private const val WEIGHT_SIGNATURE_MISMATCH = 1.0f
private const val WEIGHT_DEBUG_CERTIFICATE = 0.7f
private const val WEIGHT_UNTRUSTED_INSTALLER = 0.3f
private const val WEIGHT_HOOK_PACKAGE = 1.0f

// SHA-1 of the public Android debug signing certificate — stable across all Android
// Studio and platform debug builds.
private const val ANDROID_DEBUG_CERT_SHA1 = "61ed377e85d386a8dfee6b864bd85b0bfaa5af81"

/**
 * Android integrity detection.
 *
 * Signature signals:
 * - Running app's signing certificate SHA-256 ≠ `config.expectedSignatureSha256` (weight 1.0).
 *   Skipped when no expected value is provided.
 * - Signing certificate's SHA-1 matches the public Android debug cert (weight 0.7). Useful
 *   during development to flag accidentally-shipped debug builds.
 * - Installer package name is outside `config.trustedInstallers` (weight 0.3). Skipped
 *   when the set is empty.
 *
 * Hook signals:
 * - Xposed installer, LSPosed manager, or similar instrumentation app is visible via
 *   `getInstalledPackages()` (weight 1.0).
 *
 * The module's manifest pre-declares the hook package names under `<queries>` so
 * `getInstalledPackages()` can see them on Android 11+ without `QUERY_ALL_PACKAGES`. Keep
 * `HOOK_PACKAGES` in sync with `src/androidMain/AndroidManifest.xml`.
 */
internal actual suspend fun runIntegrityCheck(
    context: DeviceGuardContext,
    config: IntegrityCheckConfig,
): IntegrityCheckOutcome {
    val pm = context.androidContext.packageManager
    val packageName = context.androidContext.packageName
    val signatureBytes = runCatching { signingCertificates(pm, packageName) }.getOrDefault(emptyList())

    val signature = mutableListOf<IntegrityIndicator>()
    val hook = mutableListOf<IntegrityIndicator>()

    val sha256Hashes = signatureBytes.map { SHA256().digest(it).toHexString() }
    val expected = config.expectedSignatureSha256

    if (expected != null && signatureBytes.isNotEmpty() && sha256Hashes.none { it.equals(expected, ignoreCase = true) }) {
        val got = sha256Hashes.firstOrNull().orEmpty()
        signature += IntegrityIndicator("signature_mismatch:got=$got", WEIGHT_SIGNATURE_MISMATCH)
    }

    if (signatureBytes.any { sha1Hex(it).equals(ANDROID_DEBUG_CERT_SHA1, ignoreCase = true) }) {
        signature += IntegrityIndicator("debug_certificate", WEIGHT_DEBUG_CERTIFICATE)
    }

    if (config.trustedInstallers.isNotEmpty()) {
        val installer = runCatching { installerPackageName(pm, packageName) }.getOrNull()
        if (installer != null && installer !in config.trustedInstallers) {
            signature += IntegrityIndicator("untrusted_installer:$installer", WEIGHT_UNTRUSTED_INSTALLER)
        }
    }

    val installed = runCatching { pm.getInstalledPackages(0).mapTo(HashSet()) { it.packageName } }.getOrDefault(emptySet())
    for (pkg in HOOK_PACKAGES) {
        if (pkg in installed) hook += IntegrityIndicator("hook_package:$pkg", WEIGHT_HOOK_PACKAGE)
    }

    return IntegrityCheckOutcome(applicable = true, signatureIndicators = signature, hookIndicators = hook)
}

private fun signingCertificates(
    pm: PackageManager,
    packageName: String,
): List<ByteArray> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val signingInfo = info.signingInfo
        val signatures = signingInfo?.apkContentsSigners ?: signingInfo?.signingCertificateHistory
        signatures?.map { it.toByteArray() }.orEmpty()
    } else {
        @Suppress("DEPRECATION")
        val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        @Suppress("DEPRECATION")
        info.signatures?.map { it.toByteArray() }.orEmpty()
    }

@Suppress("DEPRECATION")
private fun installerPackageName(
    pm: PackageManager,
    packageName: String,
): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        pm.getInstallSourceInfo(packageName).installingPackageName
    } else {
        pm.getInstallerPackageName(packageName)
    }

private fun sha1Hex(bytes: ByteArray): String {
    val digest =
        java.security.MessageDigest
            .getInstance("SHA-1")
            .digest(bytes)
    return digest.toHexString()
}

// Keep in sync with <queries> in src/androidMain/AndroidManifest.xml.
private val HOOK_PACKAGES =
    listOf(
        "de.robv.android.xposed.installer",
        "org.lsposed.manager",
        "io.va.exposed",
        "com.topjohnwu.magisk",
    )
