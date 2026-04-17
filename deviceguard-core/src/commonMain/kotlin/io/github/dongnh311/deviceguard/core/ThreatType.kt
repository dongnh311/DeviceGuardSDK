package io.github.dongnh311.deviceguard.core

/**
 * Taxonomy of security threats that DeviceGuard modules can surface.
 *
 * Each threat carries a stable [id] (used for JSON output and user-defined scoring overrides)
 * and a [defaultWeight] consumed by [WeightedSumScoring]. Weights are tuned so that a single
 * high-severity threat saturates [RiskLevel.CRITICAL] while low-severity threats combine
 * additively without overshadowing an otherwise healthy device.
 */
public sealed class ThreatType(
    public open val id: String,
    public open val defaultWeight: Int,
) {
    /** Root access detected on an Android device. */
    public data object Root : ThreatType(id = "root", defaultWeight = 60)

    /** Jailbreak artifacts detected on an iOS device. */
    public data object Jailbreak : ThreatType(id = "jailbreak", defaultWeight = 60)

    /** App is running in an emulator or virtual device. */
    public data object Emulator : ThreatType(id = "emulator", defaultWeight = 25)

    /** An attached debugger is active. */
    public data object DebuggerAttached : ThreatType(id = "debugger_attached", defaultWeight = 25)

    /** Signing certificate does not match the expected value — possible re-signing. */
    public data object SignatureMismatch : ThreatType(id = "signature_mismatch", defaultWeight = 70)

    /** A dynamic instrumentation framework (Frida, Xposed, etc.) is present. */
    public data object HookFramework : ThreatType(id = "hook_framework", defaultWeight = 55)

    /** Device is connected through a VPN tunnel. */
    public data object VpnActive : ThreatType(id = "vpn_active", defaultWeight = 10)

    /** Device is connected through an HTTP or SOCKS proxy. */
    public data object ProxyActive : ThreatType(id = "proxy_active", defaultWeight = 15)

    /** Device traffic is routed through a known Tor exit. */
    public data object TorExit : ThreatType(id = "tor_exit", defaultWeight = 35)

    /** Extension point for application-specific threats not covered by the built-ins. */
    public data class Custom(
        override val id: String,
        override val defaultWeight: Int,
    ) : ThreatType(id, defaultWeight)
}
