package io.github.dongnh311.deviceguard.fingerprint

import io.github.dongnh311.deviceguard.core.DetectionResult
import io.github.dongnh311.deviceguard.core.DeviceFingerprint
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FingerprintDetectorTest {
    @Test
    fun sameSignalsProduceSameStableIdShapedAsSha256Hex() =
        runTest {
            val signals = mapOf("device.model" to "Pixel 8", "os.sdk_int" to "34")
            val first = detect(signals).fingerprint().id
            val second = detect(signals).fingerprint().id
            assertEquals(first, second)
            assertEquals(SHA256_HEX_LENGTH, first.length)
            assertTrue(first.all { it in '0'..'9' || it in 'a'..'f' })
        }

    @Test
    fun signalInsertionOrderDoesNotAffectId() =
        runTest {
            val forward = mapOf("a" to "1", "b" to "2", "c" to "3")
            val reverse = mapOf("c" to "3", "a" to "1", "b" to "2")
            assertEquals(detect(forward).fingerprint().id, detect(reverse).fingerprint().id)
        }

    @Test
    fun differentSignalsProduceDifferentIds() =
        runTest {
            assertNotEquals(
                detect(mapOf("x" to "1")).fingerprint().id,
                detect(mapOf("x" to "2")).fingerprint().id,
            )
        }

    @Test
    fun canonicalEncodingIsUnambiguousAcrossEqualsAndNewlines() =
        runTest {
            // Values containing '=' or '\n' would collide under a naive "k=v\n" canonicalization.
            // Length-prefixed encoding must produce different ids for these two inputs.
            val a = mapOf("key" to "foo=bar\nbaz")
            val b = mapOf("key" to "foo", "bar\nbaz" to "")
            assertNotEquals(detect(a).fingerprint().id, detect(b).fingerprint().id)
        }

    @Test
    fun detectorResultCarriesSignalsAndSchema() =
        runTest {
            val signals = mapOf("device.model" to "Pixel")
            val success = detect(signals)
            assertEquals("fingerprint", success.detectorId)
            assertEquals(signals, success.signals)
            assertEquals(signals, success.data.signals)
            assertEquals(1, success.data.version)
        }

    private suspend fun detect(signals: Map<String, String>): DetectionResult.Success<DeviceFingerprint> =
        FingerprintDetector { signals }.detect(fakeContext()) as DetectionResult.Success

    private fun DetectionResult<DeviceFingerprint>.fingerprint(): DeviceFingerprint = (this as DetectionResult.Success).data

    private companion object {
        private const val SHA256_HEX_LENGTH = 64
    }
}

internal expect fun fakeContext(): DeviceGuardContext
