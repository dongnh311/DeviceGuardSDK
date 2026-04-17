package io.github.dongnh311.deviceguard.core

/**
 * Version metadata for the DeviceGuard SDK.
 *
 * Populated at build time; used by consumers for reporting and telemetry.
 */
public object DeviceGuardVersion {
    /** Semantic version of this SDK artifact. */
    public const val VERSION: String = "0.1.0-SNAPSHOT"

    /** Schema version for [SecurityReport] JSON output. */
    public const val REPORT_SCHEMA: Int = 1
}
