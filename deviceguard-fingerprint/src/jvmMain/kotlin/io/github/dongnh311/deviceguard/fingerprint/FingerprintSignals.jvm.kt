@file:OptIn(ExperimentalStdlibApi::class)

package io.github.dongnh311.deviceguard.fingerprint

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import org.kotlincrypto.hash.sha2.SHA256
import java.net.NetworkInterface
import java.util.Locale
import java.util.TimeZone

/**
 * JVM/Desktop signals.
 *
 * Collects OS class and JVM shape. The MAC address of the first non-loopback, non-virtual
 * interface is hashed with SHA-256 at collection time so the raw address never appears in
 * [io.github.dongnh311.deviceguard.core.SecurityReport.signals]. Consumers see
 * `net.mac_hash` — a stable per-device token — never the bytes themselves.
 */
internal actual fun collectFingerprintSignals(context: DeviceGuardContext): Map<String, String> {
    val signals = HashMap<String, String>()

    signals["os.name"] = System.getProperty("os.name").orEmpty()
    signals["os.version"] = System.getProperty("os.version").orEmpty()
    signals["os.arch"] = System.getProperty("os.arch").orEmpty()
    signals["jvm.vendor"] = System.getProperty("java.vendor").orEmpty()
    signals["jvm.version"] = System.getProperty("java.version").orEmpty()
    signals["jvm.spec_version"] = System.getProperty("java.specification.version").orEmpty()

    signals["locale"] = Locale.getDefault().toLanguageTag()
    signals["timezone"] = TimeZone.getDefault().id

    runCatching {
        primaryMacAddress()?.let { signals["net.mac_hash"] = SHA256().digest(it).toHexString() }
    }

    return signals
}

/** Returns the MAC bytes of the first non-loopback, non-virtual, up interface, or null. */
private fun primaryMacAddress(): ByteArray? {
    val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
    while (interfaces.hasMoreElements()) {
        val iface = interfaces.nextElement()
        if (iface.isLoopback || iface.isVirtual || !iface.isUp) continue
        val bytes = runCatching { iface.hardwareAddress }.getOrNull() ?: continue
        if (bytes.isNotEmpty()) return bytes
    }
    return null
}
