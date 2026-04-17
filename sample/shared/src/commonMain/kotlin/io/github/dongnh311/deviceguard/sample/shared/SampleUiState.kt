package io.github.dongnh311.deviceguard.sample.shared

import io.github.dongnh311.deviceguard.core.SecurityReport

internal data class SampleUiState(
    val enabledDetectors: Set<DetectorToggle> = DetectorToggle.entries.toSet(),
    val strictRoot: Boolean = false,
    val running: Boolean = false,
    val lastReport: SecurityReport? = null,
    val lastError: String? = null,
    val lastDurationMs: Long? = null,
)

internal enum class DetectorToggle(
    val label: String,
) {
    Fingerprint("Fingerprint"),
    RootCheck("Root / Jailbreak"),
    EmulatorCheck("Emulator / Debugger"),
    IntegrityCheck("Integrity"),
    NetworkCheck("Network / VPN / Proxy"),
}
