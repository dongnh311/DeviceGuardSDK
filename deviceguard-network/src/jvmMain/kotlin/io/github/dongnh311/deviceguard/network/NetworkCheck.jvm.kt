package io.github.dongnh311.deviceguard.network

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import java.net.NetworkInterface
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI

private const val WEIGHT_VPN_INTERFACE = 1.0f
private const val WEIGHT_HTTP_PROXY_PROPERTY = 1.0f
private const val WEIGHT_SOCKS_PROXY_PROPERTY = 0.9f
private const val WEIGHT_PROXY_SELECTOR = 0.8f

private val PROXY_SELECTOR_PROBE_URI = URI.create("https://example.com")

/**
 * JVM/Desktop network detection.
 *
 * VPN signals:
 * - `NetworkInterface.getNetworkInterfaces()` includes a `tun`/`utun`/`ipsec`/`ppp`/`wg`/`tap`
 *   interface (weight 1.0). Desktop JVM has no `ConnectivityManager` equivalent, so the
 *   interface enumeration is the primary probe.
 *
 * Proxy signals:
 * - `http.proxyHost` / `https.proxyHost` system properties set (weight 1.0 each).
 * - `socksProxyHost` set (weight 0.9).
 * - `ProxySelector.getDefault().select(example.com)` returns a non-DIRECT proxy (weight 0.8).
 *   Detects proxy configurations installed via tools like `-Djava.net.useSystemProxies=true`
 *   or explicit `ProxySelector.setDefault(…)`.
 */
internal actual suspend fun runNetworkCheck(context: DeviceGuardContext): NetworkCheckOutcome {
    val vpn = runCatching { vpnInterfaceIndicators() }.getOrDefault(emptyList())
    val proxy = systemProxyIndicators() + proxySelectorIndicators()

    return NetworkCheckOutcome(applicable = true, vpnIndicators = vpn, proxyIndicators = proxy)
}

private fun vpnInterfaceIndicators(): List<NetworkIndicator> {
    val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
    val hits = mutableListOf<NetworkIndicator>()
    while (interfaces.hasMoreElements()) {
        val iface = interfaces.nextElement()
        val isUp = runCatching { iface.isUp }.getOrDefault(false)
        if (!isUp) continue
        val name = iface.name ?: continue
        if (isVpnInterface(name)) hits += NetworkIndicator("vpn_interface:$name", WEIGHT_VPN_INTERFACE)
    }
    return hits
}

private fun systemProxyIndicators(): List<NetworkIndicator> {
    val hits = mutableListOf<NetworkIndicator>()
    val http = System.getProperty("http.proxyHost")
    val https = System.getProperty("https.proxyHost")
    val socks = System.getProperty("socksProxyHost")
    if (!http.isNullOrBlank()) hits += NetworkIndicator("system_proxy:http=$http", WEIGHT_HTTP_PROXY_PROPERTY)
    if (!https.isNullOrBlank()) hits += NetworkIndicator("system_proxy:https=$https", WEIGHT_HTTP_PROXY_PROPERTY)
    if (!socks.isNullOrBlank()) hits += NetworkIndicator("system_proxy:socks=$socks", WEIGHT_SOCKS_PROXY_PROPERTY)
    return hits
}

private fun proxySelectorIndicators(): List<NetworkIndicator> =
    runCatching {
        val proxies = ProxySelector.getDefault()?.select(PROXY_SELECTOR_PROBE_URI).orEmpty()
        proxies
            .filter { it.type() != Proxy.Type.DIRECT }
            .map { NetworkIndicator("proxy_selector:${it.type()}", WEIGHT_PROXY_SELECTOR) }
    }.getOrDefault(emptyList())
