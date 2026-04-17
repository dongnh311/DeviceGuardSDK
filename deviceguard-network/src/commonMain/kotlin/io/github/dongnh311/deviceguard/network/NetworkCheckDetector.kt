package io.github.dongnh311.deviceguard.network

import io.github.dongnh311.deviceguard.core.DetectedThreat
import io.github.dongnh311.deviceguard.core.DetectionResult
import io.github.dongnh311.deviceguard.core.Detector
import io.github.dongnh311.deviceguard.core.DeviceGuard
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.ThreatType

private const val DETECTOR_ID = "networkcheck"
private const val THRESHOLD = 0.5f

/**
 * Detector that surfaces active VPN tunnels and HTTP/SOCKS proxy routing.
 *
 * Two independent checks run in the same pass: VPN indicators (VPN capability on the active
 * network, `tun`/`utun`/`ipsec`/`wg` interface names) and proxy indicators
 * (`http.proxyHost`/`https.proxyHost` system properties, `ProxySelector.getDefault` returning
 * a non-direct proxy). Each has its own clamped-to-1 confidence and trips an independent
 * threat at a fixed `0.5` threshold.
 *
 * VPN and proxy presence aren't inherently hostile — corporate deployments, privacy-focused
 * users, and network providers all legitimately terminate on tunnels — so the emitted
 * threats carry low `ThreatType.VpnActive` / `ThreatType.ProxyActive` default weights. The
 * detector surfaces the state; the risk-scoring strategy decides how much it matters.
 *
 * JS returns [DetectionResult.NotApplicable] — reliable in-browser VPN/proxy detection
 * needs server-side correlation and is out of scope for this module.
 */
public class NetworkCheckDetector internal constructor(
    private val probe: suspend (DeviceGuardContext) -> NetworkCheckOutcome,
) : Detector<NetworkCheckResult> {
    public constructor() : this(probe = { runNetworkCheck(it) })

    override val id: String = DETECTOR_ID

    override suspend fun detect(context: DeviceGuardContext): DetectionResult<NetworkCheckResult> {
        val outcome = probe(context)
        if (!outcome.applicable) return DetectionResult.NotApplicable(id, outcome.reason)

        val vpnConfidence = sumWeights(outcome.vpnIndicators)
        val proxyConfidence = sumWeights(outcome.proxyIndicators)
        val vpnActive = vpnConfidence >= THRESHOLD
        val proxyActive = proxyConfidence >= THRESHOLD

        val vpnNames = outcome.vpnIndicators.map { it.name }
        val proxyNames = outcome.proxyIndicators.map { it.name }

        val threats =
            buildList {
                if (vpnActive) add(threat(ThreatType.VpnActive, vpnConfidence, vpnNames))
                if (proxyActive) add(threat(ThreatType.ProxyActive, proxyConfidence, proxyNames))
            }

        return DetectionResult.Success(
            detectorId = id,
            data =
                NetworkCheckResult(
                    vpnActive = vpnActive,
                    proxyActive = proxyActive,
                    vpnConfidence = vpnConfidence,
                    proxyConfidence = proxyConfidence,
                    vpnIndicators = vpnNames,
                    proxyIndicators = proxyNames,
                ),
            threats = threats,
            signals =
                mapOf(
                    "networkcheck.threshold" to THRESHOLD.toString(),
                    "networkcheck.vpn_indicator_count" to outcome.vpnIndicators.size.toString(),
                    "networkcheck.proxy_indicator_count" to outcome.proxyIndicators.size.toString(),
                ),
        )
    }

    private fun sumWeights(indicators: List<NetworkIndicator>): Float = indicators.sumOf { it.weight.toDouble() }.toFloat().coerceIn(0f, 1f)

    private fun threat(
        type: ThreatType,
        confidence: Float,
        indicators: List<String>,
    ): DetectedThreat =
        DetectedThreat.of(
            threat = type,
            confidence = confidence,
            indicators = indicators,
        )
}

/** Attach a [NetworkCheckDetector] to the builder. */
public fun DeviceGuard.Builder.enableNetworkCheck(): DeviceGuard.Builder = addDetector(NetworkCheckDetector())
