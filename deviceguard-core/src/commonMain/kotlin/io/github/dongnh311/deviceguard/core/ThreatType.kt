package io.github.dongnh311.deviceguard.core

/**
 * Taxonomy of security threats that DeviceGuard modules can surface.
 *
 * Each threat carries a stable [id] (used for JSON output and user-defined scoring overrides)
 * and a [defaultWeight] consumed by [WeightedSumScoring]. Weights are tuned so that a single
 * high-severity threat saturates [RiskLevel.CRITICAL] while low-severity threats combine
 * additively without overshadowing an otherwise healthy device.
 *
 * The type is a sealed interface so consumers can exhaustively pattern match on built-ins while
 * still attaching [Custom] threats emitted by application code.
 */
public sealed interface ThreatType {
    /** Stable identifier, unique across the taxonomy. */
    public val id: String

    /** Default contribution to [WeightedSumScoring]; `0..100`. */
    public val defaultWeight: Int

    /** Root access detected on an Android device. */
    public data object Root : ThreatType {
        override val id: String = "root"
        override val defaultWeight: Int = 60
    }

    /** Jailbreak artifacts detected on an iOS device. */
    public data object Jailbreak : ThreatType {
        override val id: String = "jailbreak"
        override val defaultWeight: Int = 60
    }

    /** App is running in an emulator or virtual device. */
    public data object Emulator : ThreatType {
        override val id: String = "emulator"
        override val defaultWeight: Int = 25
    }

    /** An attached debugger is active. */
    public data object DebuggerAttached : ThreatType {
        override val id: String = "debugger_attached"
        override val defaultWeight: Int = 25
    }

    /** Signing certificate does not match the expected value — possible re-signing. */
    public data object SignatureMismatch : ThreatType {
        override val id: String = "signature_mismatch"
        override val defaultWeight: Int = 70
    }

    /** A dynamic instrumentation framework (Frida, Xposed, etc.) is present. */
    public data object HookFramework : ThreatType {
        override val id: String = "hook_framework"
        override val defaultWeight: Int = 55
    }

    /** Device is connected through a VPN tunnel. */
    public data object VpnActive : ThreatType {
        override val id: String = "vpn_active"
        override val defaultWeight: Int = 10
    }

    /** Device is connected through an HTTP or SOCKS proxy. */
    public data object ProxyActive : ThreatType {
        override val id: String = "proxy_active"
        override val defaultWeight: Int = 15
    }

    /** Device traffic is routed through a known Tor exit. */
    public data object TorExit : ThreatType {
        override val id: String = "tor_exit"
        override val defaultWeight: Int = 35
    }

    /**
     * Extension point for application-specific threats not covered by the built-ins.
     *
     * @param id stable identifier, should be unique across your deployment.
     * @param defaultWeight contribution to the aggregated risk score (0..100).
     */
    public data class Custom(
        override val id: String,
        override val defaultWeight: Int,
    ) : ThreatType {
        init {
            require(id.isNotBlank()) { "Custom threat id must not be blank" }
            require(defaultWeight in 0..MAX_WEIGHT) {
                "defaultWeight must be within 0..$MAX_WEIGHT"
            }
        }

        private companion object {
            const val MAX_WEIGHT = 100
        }
    }
}
