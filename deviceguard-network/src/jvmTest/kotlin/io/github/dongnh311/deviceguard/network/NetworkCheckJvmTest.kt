package io.github.dongnh311.deviceguard.network

import kotlinx.coroutines.test.runTest
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetworkCheckJvmTest {
    private val savedProperties = mutableMapOf<String, String?>()
    private val savedProxySelector: ProxySelector? = ProxySelector.getDefault()

    @AfterTest
    fun restoreGlobals() {
        for ((key, value) in savedProperties) {
            if (value == null) System.clearProperty(key) else System.setProperty(key, value)
        }
        savedProperties.clear()
        ProxySelector.setDefault(savedProxySelector)
    }

    @Test
    fun cleanEnvironmentProducesNoProxyIndicators() =
        runTest {
            clearProperty(PROP_HTTP_HOST)
            clearProperty(PROP_HTTPS_HOST)
            clearProperty(PROP_SOCKS_HOST)
            ProxySelector.setDefault(DirectProxySelector)

            val outcome = runNetworkCheck(fakeContext())

            assertTrue(outcome.applicable)
            assertTrue(outcome.proxyIndicators.isEmpty(), "clean env: got ${outcome.proxyIndicators}")
            assertNull(outcome.reason)
        }

    @Test
    fun httpProxyPropertyEmitsFullWeightIndicator() =
        runTest {
            clearProperty(PROP_HTTPS_HOST)
            clearProperty(PROP_SOCKS_HOST)
            ProxySelector.setDefault(DirectProxySelector)
            setProperty(PROP_HTTP_HOST, "proxy.corp.example.com")

            val outcome = runNetworkCheck(fakeContext())

            val http = outcome.proxyIndicators.single { it.name.startsWith("system_proxy:http=") }
            assertEquals("system_proxy:http=proxy.corp.example.com", http.name)
            assertEquals(1.0f, http.weight)
        }

    @Test
    fun socksPropertyEmitsReducedWeightIndicator() =
        runTest {
            clearProperty(PROP_HTTP_HOST)
            clearProperty(PROP_HTTPS_HOST)
            ProxySelector.setDefault(DirectProxySelector)
            setProperty(PROP_SOCKS_HOST, "socks.internal")

            val outcome = runNetworkCheck(fakeContext())

            val socks = outcome.proxyIndicators.single { it.name.startsWith("system_proxy:socks=") }
            assertEquals("system_proxy:socks=socks.internal", socks.name)
            assertEquals(0.9f, socks.weight)
        }

    @Test
    fun customProxySelectorEmitsSelectorIndicator() =
        runTest {
            clearProperty(PROP_HTTP_HOST)
            clearProperty(PROP_HTTPS_HOST)
            clearProperty(PROP_SOCKS_HOST)
            ProxySelector.setDefault(FixedProxySelector)

            val outcome = runNetworkCheck(fakeContext())

            val selectorHit = outcome.proxyIndicators.single { it.name.startsWith("proxy_selector:") }
            assertEquals("proxy_selector:HTTP", selectorHit.name)
            assertEquals(0.8f, selectorHit.weight)
        }

    @Test
    fun blankProxyPropertiesAreIgnored() =
        runTest {
            clearProperty(PROP_HTTPS_HOST)
            clearProperty(PROP_SOCKS_HOST)
            ProxySelector.setDefault(DirectProxySelector)
            setProperty(PROP_HTTP_HOST, "   ")

            val outcome = runNetworkCheck(fakeContext())

            assertTrue(
                outcome.proxyIndicators.none { it.name.startsWith("system_proxy:http=") },
                "blank proxy host must not produce an indicator",
            )
        }

    private fun setProperty(
        key: String,
        value: String,
    ) {
        savedProperties.getOrPut(key) { System.getProperty(key) }
        System.setProperty(key, value)
    }

    private fun clearProperty(key: String) {
        savedProperties.getOrPut(key) { System.getProperty(key) }
        System.clearProperty(key)
    }

    private object DirectProxySelector : ProxySelector() {
        override fun select(uri: URI?): List<Proxy> = listOf(Proxy.NO_PROXY)

        override fun connectFailed(
            uri: URI?,
            sa: SocketAddress?,
            ioe: java.io.IOException?,
        ) = Unit
    }

    private object FixedProxySelector : ProxySelector() {
        private val proxies =
            listOf(Proxy(Proxy.Type.HTTP, java.net.InetSocketAddress.createUnresolved("proxy.example.com", 8080)))

        override fun select(uri: URI?): List<Proxy> = proxies

        override fun connectFailed(
            uri: URI?,
            sa: SocketAddress?,
            ioe: java.io.IOException?,
        ) = Unit
    }

    private companion object {
        private const val PROP_HTTP_HOST = "http.proxyHost"
        private const val PROP_HTTPS_HOST = "https.proxyHost"
        private const val PROP_SOCKS_HOST = "socksProxyHost"
    }
}
