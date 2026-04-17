package io.github.dongnh311.deviceguard.fingerprint

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FingerprintSignalsJvmTest {
    @Test
    fun emitsRequiredKeys() {
        val signals = collectFingerprintSignals(fakeContext())

        for (key in REQUIRED_KEYS) {
            assertNotNull(signals[key], "missing signal: $key")
            assertTrue(signals.getValue(key).isNotEmpty(), "empty signal: $key")
        }
    }

    @Test
    fun macHashWhenPresentIsLowercaseHex64() {
        // Sandboxed CI runners may have no non-loopback hardware interface, in which case the
        // MAC hash is legitimately absent. The check guarantees shape only when present.
        val signals = collectFingerprintSignals(fakeContext())
        val macHash = signals["net.mac_hash"] ?: return
        assertEquals(64, macHash.length, "SHA-256 hex must be 64 chars")
        assertTrue(macHash.all { it in '0'..'9' || it in 'a'..'f' }, "lowercase hex only")
    }

    @Test
    fun stableAcrossRepeatedCalls() {
        val first = collectFingerprintSignals(fakeContext())
        val second = collectFingerprintSignals(fakeContext())
        assertEquals(first, second)
    }

    private companion object {
        private val REQUIRED_KEYS =
            setOf(
                "os.name",
                "os.version",
                "os.arch",
                "jvm.vendor",
                "jvm.version",
                "jvm.spec_version",
                "locale",
                "timezone",
            )
    }
}
