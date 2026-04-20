package io.github.dongnh311.deviceguard.sample.desktop

import io.github.dongnh311.deviceguard.core.DeviceGuard
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.emulator.enableEmulatorCheck
import io.github.dongnh311.deviceguard.fingerprint.enableFingerprint
import io.github.dongnh311.deviceguard.integrity.enableIntegrityCheck
import io.github.dongnh311.deviceguard.network.enableNetworkCheck
import io.github.dongnh311.deviceguard.rootcheck.enableRootCheck
import kotlinx.coroutines.runBlocking
import kotlin.time.TimeSource

fun main() {
    val guard = DeviceGuard.Builder(DeviceGuardContext())
        .enableFingerprint()
        .enableRootCheck(strict = false)
        .enableEmulatorCheck()
        .enableIntegrityCheck()
        .enableNetworkCheck()
        .build()

    val mark = TimeSource.Monotonic.markNow()
    val report = runBlocking { guard.analyze() }
    val elapsed = mark.elapsedNow()

    println("=== DeviceGuard Headless Report (JVM) ===")
    println("riskScore  : ${report.riskScore}")
    println("riskLevel  : ${report.riskLevel}")
    println("elapsedMs  : ${elapsed.inWholeMilliseconds}")
    println("fingerprint: ${report.fingerprint?.id ?: "<disabled>"}")
    println("threats    : ${report.threats.size}")
    report.threats.forEach { t -> println("  - $t") }
    println("signals    : ${report.signals.size} keys")
    report.signals.forEach { (k, v) -> println("  $k = $v") }
}
