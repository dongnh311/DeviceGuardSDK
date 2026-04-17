package io.github.dongnh311.deviceguard.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import java.net.NetworkInterface

private const val WEIGHT_VPN_TRANSPORT = 1.0f
private const val WEIGHT_VPN_INTERFACE = 0.8f
private const val WEIGHT_HTTP_PROXY_PROPERTY = 1.0f
private const val WEIGHT_SOCKS_PROXY_PROPERTY = 0.9f

/**
 * Android network detection.
 *
 * VPN signals:
 * - `ConnectivityManager.getActiveNetwork()` reports `TRANSPORT_VPN` OR lacks
 *   `NET_CAPABILITY_NOT_VPN` (weight 1.0). This is the authoritative framework-level answer
 *   since API 21. Requires `ACCESS_NETWORK_STATE` — the module's manifest declares it.
 * - `NetworkInterface.getNetworkInterfaces()` contains a `tun`/`utun`/`ipsec`/`ppp`/`wg`/`tap`
 *   interface (weight 0.8). Catches VPNs that bypass the framework (rare but possible).
 *
 * Proxy signals:
 * - `http.proxyHost` / `https.proxyHost` system property set (weight 1.0 each).
 * - `socksProxyHost` set (weight 0.9). Slightly lower — less common, and a misconfigured
 *   DNS resolver is a common false-positive source.
 */
internal actual suspend fun runNetworkCheck(context: DeviceGuardContext): NetworkCheckOutcome {
    val vpn = mutableListOf<NetworkIndicator>()
    val proxy = mutableListOf<NetworkIndicator>()

    runCatching { detectVpnViaConnectivityManager(context.androidContext) }.getOrNull()?.let(vpn::add)
    vpn += runCatching { detectVpnInterfaces() }.getOrDefault(emptyList())
    proxy += systemProxyIndicators()

    return NetworkCheckOutcome(applicable = true, vpnIndicators = vpn, proxyIndicators = proxy)
}

private fun detectVpnViaConnectivityManager(context: Context): NetworkIndicator? {
    // ConnectivityManager.activeNetwork + getNetworkCapabilities arrived in API 23 (M).
    // On API 21/22 the NetworkInterface scan below is the only VPN probe we have.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
    val active = cm.activeNetwork ?: return null
    val caps = cm.getNetworkCapabilities(active) ?: return null
    val notVpn = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
    val vpnTransport = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    return when {
        vpnTransport -> NetworkIndicator("vpn_transport:TRANSPORT_VPN", WEIGHT_VPN_TRANSPORT)
        !notVpn -> NetworkIndicator("vpn_capability:!NET_CAPABILITY_NOT_VPN", WEIGHT_VPN_TRANSPORT)
        else -> null
    }
}

private fun detectVpnInterfaces(): List<NetworkIndicator> {
    val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
    val hits = mutableListOf<NetworkIndicator>()
    while (interfaces.hasMoreElements()) {
        val iface = interfaces.nextElement()
        if (!iface.isUp) continue
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
