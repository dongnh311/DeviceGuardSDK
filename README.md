# DeviceGuard SDK

[![Build](https://github.com/dongnh311/DeviceGuardSDK/actions/workflows/ci.yml/badge.svg)](https://github.com/dongnh311/DeviceGuardSDK/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)

> Kotlin Multiplatform SDK for comprehensive device security: fingerprinting, root/jailbreak detection, emulator detection, app integrity, and network inspection — on Android, iOS, JVM/Desktop, and Web.

## Status

**Pre-release.** APIs are unstable and may change before v0.1.0. See [`DEVICEGUARD_PLAN.md`](./DEVICEGUARD_PLAN.md) for the roadmap.

## Installation

> Coordinates will be published after Phase 9. Placeholder shown below.

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.dongnh311:deviceguard-core:0.1.0")
    // or pick individual modules:
    implementation("io.github.dongnh311:deviceguard-fingerprint:0.1.0")
    implementation("io.github.dongnh311:deviceguard-rootcheck:0.1.0")
    implementation("io.github.dongnh311:deviceguard-emulator:0.1.0")
    implementation("io.github.dongnh311:deviceguard-integrity:0.1.0")
    implementation("io.github.dongnh311:deviceguard-network:0.1.0")
}
```

## Quick Start

```kotlin
val guard = DeviceGuard.Builder(context)
    .enableFingerprint()
    .enableRootCheck(strict = true)
    .enableEmulatorCheck()
    .enableIntegrityCheck(expectedSignature = "…")
    .enableNetworkCheck()
    .build()

val report = guard.analyze()
println("Risk: ${report.riskScore}/100")
println("Threats: ${report.threats}")
println("Device ID: ${report.fingerprint.id}")
```

## Modules

| Module | Status | Purpose |
|--------|--------|---------|
| `deviceguard-core` | ✅ available | Public API, models, orchestrator |
| `deviceguard-fingerprint` | ✅ available | Stable cross-platform device ID |
| `deviceguard-rootcheck` | 🚧 planned | Root / Jailbreak detection |
| `deviceguard-emulator` | 🚧 planned | Emulator / Debugger detection |
| `deviceguard-integrity` | 🚧 planned | App tampering & hook detection |
| `deviceguard-network` | 🚧 planned | VPN / Proxy / Tor inspection |
| `deviceguard-bom` | ✅ available | Bill of Materials for version alignment |

## Platforms

| Platform | Core | Fingerprint |
|----------|------|-------------|
| Android (API 21+) | ✅ | ✅ |
| iOS (13+) | ✅ | ✅ |
| JVM / Desktop | ✅ | ✅ |
| JS / Web | ✅ | ✅ (best-effort, browser only) |

## Fingerprinting

The `deviceguard-fingerprint` module collects non-PII platform signals and hashes them
(SHA-256 with length-prefixed encoding, no collision risk) into a stable device id.

Signals collected per platform:

- **Android:** `Build.MANUFACTURER/BRAND/MODEL/DEVICE/PRODUCT/HARDWARE`, `SDK_INT`,
  `SUPPORTED_ABIS`, `Settings.Secure.ANDROID_ID`, screen density and resolution, locale, timezone.
- **iOS:** `UIDevice.systemName/systemVersion/model`, `identifierForVendor`, screen scale and
  bounds, locale, timezone.
- **JVM:** `os.name/version/arch`, `java.vendor/version`, locale, timezone, and a SHA-256
  digest of the first non-loopback MAC address (the raw bytes never leave the detector).
- **JS:** `navigator.userAgent/platform/language/hardwareConcurrency`, screen resolution and
  color depth, timezone and UTC offset.

No IMEI, advertising id, account name, email, or other directly identifying value is read —
the fingerprint is stable across app launches but invalidates on a factory reset or MAC change.

## Building

```bash
./gradlew build        # all targets
./gradlew allTests     # run all platform tests
./gradlew dokkaHtml    # generate API docs
```

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) (coming soon). Security issues — see [`SECURITY.md`](SECURITY.md) (coming soon).

## License

Apache License 2.0 — see [`LICENSE`](LICENSE).
