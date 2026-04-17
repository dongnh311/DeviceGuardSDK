package io.github.dongnh311.deviceguard.network

import io.github.dongnh311.deviceguard.core.DetectionResult
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.ThreatType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkCheckDetectorTest {
    @Test
    fun vpnHitOnlyEmitsVpnThreat() =
        runTest {
            val success = detect(outcome(vpn = listOf(NetworkIndicator("vpn_transport:TRANSPORT_VPN", 1.0f)))).success()
            assertTrue(success.data.vpnActive)
            assertFalse(success.data.proxyActive)
            assertEquals(1, success.threats.size)
            assertEquals(ThreatType.VpnActive.id, success.threats.single().type)
        }

    @Test
    fun proxyHitOnlyEmitsProxyThreat() =
        runTest {
            val success = detect(outcome(proxy = listOf(NetworkIndicator("system_proxy:http=proxy.corp.example.com", 1.0f)))).success()
            assertFalse(success.data.vpnActive)
            assertTrue(success.data.proxyActive)
            assertEquals(ThreatType.ProxyActive.id, success.threats.single().type)
        }

    @Test
    fun bothStreamsFireBothThreats() =
        runTest {
            val success =
                detect(
                    outcome(
                        vpn = listOf(NetworkIndicator("vpn_transport:TRANSPORT_VPN", 1.0f)),
                        proxy = listOf(NetworkIndicator("system_proxy:https=corp.example.com", 1.0f)),
                    ),
                ).success()
            assertTrue(success.data.vpnActive)
            assertTrue(success.data.proxyActive)
            assertEquals(
                setOf(ThreatType.VpnActive.id, ThreatType.ProxyActive.id),
                success.threats.map { it.type }.toSet(),
            )
        }

    @Test
    fun confidenceExactlyAtThresholdTrips() =
        runTest {
            val success =
                detect(
                    outcome(
                        vpn = listOf(NetworkIndicator("x", 0.5f)),
                        proxy = listOf(NetworkIndicator("y", 0.5f)),
                    ),
                ).success()
            assertTrue(success.data.vpnActive, "vpn at 0.5 must trip")
            assertTrue(success.data.proxyActive, "proxy at 0.5 must trip")
        }

    @Test
    fun confidenceJustBelowThresholdDoesNotTrip() =
        runTest {
            val success =
                detect(
                    outcome(
                        vpn = listOf(NetworkIndicator("x", 0.49f)),
                        proxy = listOf(NetworkIndicator("y", 0.49f)),
                    ),
                ).success()
            assertFalse(success.data.vpnActive)
            assertFalse(success.data.proxyActive)
            assertTrue(success.threats.isEmpty())
        }

    @Test
    fun confidenceClampsToOneWithMultipleStrongIndicators() =
        runTest {
            val success =
                detect(
                    outcome(
                        vpn =
                            listOf(
                                NetworkIndicator("vpn_transport:TRANSPORT_VPN", 1.0f),
                                NetworkIndicator("vpn_interface:utun0", 0.8f),
                            ),
                    ),
                ).success()
            assertEquals(1.0f, success.data.vpnConfidence)
        }

    @Test
    fun noIndicatorsAcrossBothStreamsMeansNoThreats() =
        runTest {
            val success = detect(outcome()).success()
            assertFalse(success.data.vpnActive)
            assertFalse(success.data.proxyActive)
            assertTrue(success.threats.isEmpty())
            assertEquals(0f, success.data.vpnConfidence)
            assertEquals(0f, success.data.proxyConfidence)
        }

    @Test
    fun signalsSummariseTheProbe() =
        runTest {
            val success =
                detect(
                    outcome(
                        vpn = listOf(NetworkIndicator("a", 1.0f), NetworkIndicator("b", 0.3f)),
                        proxy = listOf(NetworkIndicator("c", 1.0f)),
                    ),
                ).success()
            assertEquals("0.5", success.signals["networkcheck.threshold"])
            assertEquals("2", success.signals["networkcheck.vpn_indicator_count"])
            assertEquals("1", success.signals["networkcheck.proxy_indicator_count"])
        }

    @Test
    fun inapplicablePlatformPropagatesNotApplicable() =
        runTest {
            val result = detect(NetworkCheckOutcome(applicable = false, reason = "browser"))
            val notApplicable = result as DetectionResult.NotApplicable
            assertEquals("networkcheck", notApplicable.detectorId)
            assertEquals("browser", notApplicable.reason)
        }

    @Test
    fun isVpnInterfaceMatchesKnownPrefixes() {
        assertTrue(isVpnInterface("utun0"))
        assertTrue(isVpnInterface("tun0"))
        assertTrue(isVpnInterface("ppp0"))
        assertTrue(isVpnInterface("ipsec0"))
        assertTrue(isVpnInterface("wg0"))
        assertTrue(isVpnInterface("TAP0"), "match should be case-insensitive")
        assertFalse(isVpnInterface("en0"))
        assertFalse(isVpnInterface("lo0"))
        assertFalse(isVpnInterface(""))
    }

    private fun outcome(
        vpn: List<NetworkIndicator> = emptyList(),
        proxy: List<NetworkIndicator> = emptyList(),
    ): NetworkCheckOutcome = NetworkCheckOutcome(applicable = true, vpnIndicators = vpn, proxyIndicators = proxy)

    private suspend fun detect(outcome: NetworkCheckOutcome): DetectionResult<NetworkCheckResult> =
        NetworkCheckDetector(probe = { outcome }).detect(fakeContext())

    private fun DetectionResult<NetworkCheckResult>.success(): DetectionResult.Success<NetworkCheckResult> = this as DetectionResult.Success
}

internal expect fun fakeContext(): DeviceGuardContext
