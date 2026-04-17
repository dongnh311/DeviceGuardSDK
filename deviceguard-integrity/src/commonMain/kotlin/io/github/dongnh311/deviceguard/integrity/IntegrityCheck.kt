package io.github.dongnh311.deviceguard.integrity

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

/**
 * Caller-supplied configuration for [IntegrityCheckDetector].
 *
 * @property expectedSignatureSha256 lowercase hex SHA-256 digest of the Android signing
 *   certificate that produced this binary. When non-null, the Android probe compares the
 *   running app's signature against this value and emits a strong mismatch indicator on any
 *   divergence. When null, the Android probe falls back to weaker signals (debug
 *   certificate, untrusted installer) — useful during development when the expected hash
 *   isn't available at build time.
 * @property trustedInstallers package names considered legitimate installers. An empty set
 *   disables the installer check. Typical values: `{"com.android.vending"}` for
 *   Play-Store-only.
 */
internal data class IntegrityCheckConfig(
    val expectedSignatureSha256: String?,
    val trustedInstallers: Set<String>,
)

/** A single integrity signal that fired on the host platform. */
internal data class IntegrityIndicator(
    val name: String,
    val weight: Float,
)

/**
 * Outcome of platform-specific integrity probing.
 *
 * Two disjoint indicator lists — signature tampering vs hook framework — feed two separate
 * confidences in the detector, yielding independent `ThreatType.SignatureMismatch` and
 * `ThreatType.HookFramework` entries on the final report. [signatureCheckRun] lets the
 * platform tell the detector whether the signature stream meaningfully ran at all (so a
 * `false` result with no indicators is distinguishable from "we didn't check"). JVM and JS
 * currently return `applicable = false` — desktop JAR verification and browser SRI have
 * materially different threat models and are deferred.
 */
internal data class IntegrityCheckOutcome(
    val applicable: Boolean,
    val reason: String? = null,
    val signatureCheckRun: Boolean = false,
    val signatureIndicators: List<IntegrityIndicator> = emptyList(),
    val hookIndicators: List<IntegrityIndicator> = emptyList(),
)

/** Platform entry point. Implementations must never throw — catch and return no indicators. */
internal expect suspend fun runIntegrityCheck(
    context: DeviceGuardContext,
    config: IntegrityCheckConfig,
): IntegrityCheckOutcome
