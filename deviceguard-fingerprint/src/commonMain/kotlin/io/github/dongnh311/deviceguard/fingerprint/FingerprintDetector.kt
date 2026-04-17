@file:OptIn(ExperimentalStdlibApi::class)

package io.github.dongnh311.deviceguard.fingerprint

import io.github.dongnh311.deviceguard.core.DetectionResult
import io.github.dongnh311.deviceguard.core.Detector
import io.github.dongnh311.deviceguard.core.DeviceFingerprint
import io.github.dongnh311.deviceguard.core.DeviceGuard
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import org.kotlincrypto.hash.sha2.SHA256

/**
 * Detector that produces a stable cross-platform device identifier.
 *
 * Collects a map of platform signals via [collectFingerprintSignals], hashes it with SHA-256
 * using a length-prefixed encoding so two different maps can never collide, and returns the
 * hex digest as [DeviceFingerprint.id]. The same device produces the same id across runs as
 * long as the collected signals don't change.
 */
public class FingerprintDetector internal constructor(
    private val signalProvider: (DeviceGuardContext) -> Map<String, String>,
) : Detector<DeviceFingerprint> {
    public constructor() : this({ collectFingerprintSignals(it) })

    override val id: String = FingerprintModule.DETECTOR_ID

    override val description: String = "Collects platform signals and hashes them into a stable device id."

    override suspend fun detect(context: DeviceGuardContext): DetectionResult<DeviceFingerprint> {
        val rawSignals = signalProvider(context)
        val digest = hashSignals(rawSignals).toHexString()
        val fingerprint =
            DeviceFingerprint(
                id = digest,
                signals = rawSignals,
                version = FingerprintModule.SCHEMA_VERSION,
            )
        return DetectionResult.Success(
            detectorId = id,
            data = fingerprint,
            signals = rawSignals,
        )
    }
}

/** Attach a [FingerprintDetector] to the builder. Returns the same builder for chaining. */
public fun DeviceGuard.Builder.enableFingerprint(): DeviceGuard.Builder = addDetector(FingerprintDetector())

/**
 * Length-prefixed incremental SHA-256 over a sorted signal map.
 *
 * For each key (sorted ascending) emit `len(k)‖k‖len(v)‖v` where lengths are 4-byte
 * big-endian. This is unambiguous: two different maps can only collide if SHA-256 collides.
 * A naive `"k=v\n"` join is vulnerable to values containing `\n` or `=` — `navigator.userAgent`
 * regularly contains `=`.
 */
private fun hashSignals(signals: Map<String, String>): ByteArray {
    val digest = SHA256()
    for (key in signals.keys.sorted()) {
        val keyBytes = key.encodeToByteArray()
        val valueBytes = signals.getValue(key).encodeToByteArray()
        digest.update(lengthPrefix(keyBytes.size))
        digest.update(keyBytes)
        digest.update(lengthPrefix(valueBytes.size))
        digest.update(valueBytes)
    }
    return digest.digest()
}

private fun lengthPrefix(size: Int): ByteArray =
    byteArrayOf(
        (size ushr 24).toByte(),
        (size ushr 16).toByte(),
        (size ushr 8).toByte(),
        size.toByte(),
    )
