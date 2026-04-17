package io.github.dongnh311.deviceguard.fingerprint

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

/**
 * Platform-specific signal collection.
 *
 * Each actual returns a map of raw key/value pairs gathered from platform APIs. The map is
 * consumed by [FingerprintDetector] which normalizes the order and hashes the content to
 * produce a stable [io.github.dongnh311.deviceguard.core.DeviceFingerprint.id].
 *
 * Implementations MUST:
 * - return only non-PII signals — device class and environment shape, not personally
 *   identifying values;
 * - use stable string formats (`toString()` on numeric/enum values) so the same device
 *   emits the same bytes across runs;
 * - swallow platform errors and omit the offending key rather than throwing — a partial
 *   fingerprint is better than a failed one.
 */
internal expect fun collectFingerprintSignals(context: DeviceGuardContext): Map<String, String>
