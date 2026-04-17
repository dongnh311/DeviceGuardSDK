package io.github.dongnh311.deviceguard.network

import kotlinx.serialization.Serializable

/**
 * Typed payload emitted by [NetworkCheckDetector] on
 * [io.github.dongnh311.deviceguard.core.DetectionResult.Success.data].
 *
 * @property vpnActive `true` when the aggregated VPN signals clear the detector's threshold.
 * @property proxyActive `true` when the aggregated HTTP/SOCKS proxy signals clear it.
 * @property vpnConfidence clamped to `0f..1f`, aggregating VPN signals.
 * @property proxyConfidence clamped to `0f..1f`, aggregating proxy signals.
 * @property vpnIndicators forensic names of fired VPN signals (no PII). Examples:
 *   `vpn_transport:TRANSPORT_VPN`, `vpn_interface:utun3`.
 * @property proxyIndicators forensic names of fired proxy signals (no PII). Examples:
 *   `system_proxy:http=proxy.corp.example.com`, `proxy_selector:HTTP`.
 */
@Serializable
public data class NetworkCheckResult(
    public val vpnActive: Boolean,
    public val proxyActive: Boolean,
    public val vpnConfidence: Float,
    public val proxyConfidence: Float,
    public val vpnIndicators: List<String>,
    public val proxyIndicators: List<String>,
) {
    init {
        require(vpnConfidence in 0f..1f) { "vpnConfidence must be within 0f..1f" }
        require(proxyConfidence in 0f..1f) { "proxyConfidence must be within 0f..1f" }
    }
}
