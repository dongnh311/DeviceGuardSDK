package io.github.dongnh311.deviceguard.core

import kotlinx.serialization.Serializable

/**
 * Stable identifier derived from a set of platform signals.
 *
 * The same device should produce the same [id] across app launches as long as the underlying
 * signals don't change. See `deviceguard-fingerprint` for the collection logic and the
 * supported signal set per platform.
 *
 * @property id SHA-256 hash (hex-encoded) of the normalized signal map.
 * @property signals raw key/value pairs that fed into [id]. Useful for debugging drift between
 *   runs; callers should treat the contents as diagnostic only.
 * @property version schema version; bump when the signal selection or normalization changes
 *   in a way that invalidates previously-collected fingerprints.
 */
@Serializable
public data class DeviceFingerprint(
    public val id: String,
    public val signals: Map<String, String>,
    public val version: Int = 1,
) {
    init {
        require(id.isNotBlank()) { "Fingerprint id must not be blank" }
        require(version > 0) { "version must be positive" }
    }
}
