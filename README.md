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
| `deviceguard-rootcheck` | ✅ available | Root / Jailbreak detection |
| `deviceguard-emulator` | ✅ available | Emulator / Debugger detection |
| `deviceguard-integrity` | ✅ available | App tampering & hook detection |
| `deviceguard-network` | ✅ available | VPN / Proxy / Tor inspection |
| `deviceguard-bom` | ✅ available | Bill of Materials for version alignment |

## Platforms

| Platform | Core | Fingerprint | Root/Jailbreak | Emulator/Debugger | Integrity | Network |
|----------|------|-------------|----------------|-------------------|-----------|---------|
| Android (API 21+) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| iOS (13+) | ✅ | ✅ | ✅ (jailbreak) | ✅ (simulator; debugger TBD) | ✅ (Frida artefacts; re-sign TBD) | — deferred (cinterop) |
| JVM / Desktop | ✅ | ✅ | — not applicable | ✅ (JDWP) | — deferred | ✅ |
| JS / Web | ✅ | ✅ (best-effort, browser only) | — not applicable | ✅ (webdriver; best-effort) | — deferred | — deferred |

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

## Root / Jailbreak detection

`deviceguard-rootcheck` surfaces root on Android and jailbreak on iOS. Opt in via
`DeviceGuard.Builder(context).enableRootCheck(strict = true)`. The detector aggregates
per-signal weights in `[0.0, 1.0]` and trips `RootCheckResult.isRooted` at:

- `strict = false` (default): confidence ≥ 0.5 — a single strong signal wins.
- `strict = true`: confidence ≥ 0.2 — any moderate signal wins.

Signals per platform:

- **Android** — 13 known `su`/Superuser/Magisk binary paths (weight 1.0); a single
  `PackageManager.getInstalledPackages()` scan matched against 12 root-tooling package names
  (weight 0.9); `Build.TAGS` contains `test-keys` (weight 0.3). The module's manifest
  pre-declares the package list under `<queries>` so it works on Android 11+ without
  `QUERY_ALL_PACKAGES`.
- **iOS** — 14 known jailbreak artifact paths (Cydia, Sileo, Zebra, MobileSubstrate,
  `/bin/bash`, apt cache, etc.) via `NSFileManager.fileExistsAtPath` (weight 0.9); a
  sandbox-escape write probe at `/private/DeviceGuardProbe-<uuid>.txt` with `try/finally`
  cleanup (weight 1.0).
- **JVM / JS** — not applicable; the detector emits `DetectionResult.NotApplicable`.

When `isRooted` is true, the detector adds a `ThreatType.Root` (Android) or
`ThreatType.Jailbreak` (iOS) threat to `SecurityReport.threats` with the aggregated
confidence. The `indicators` list is suitable for forensic logging and contains no PII.

## Emulator / Debugger detection

`deviceguard-emulator` surfaces emulator / virtual-device signals alongside
attached-debugger signals. Opt in via
`DeviceGuard.Builder(context).enableEmulatorCheck()`. Two disjoint indicator lists feed
two independent confidences (each clamped to `[0, 1]`) and trip
`ThreatType.Emulator` / `ThreatType.DebuggerAttached` independently at a fixed `0.5`
threshold — so a debug build attached to an IDE on a real device produces a debugger
threat but not an emulator threat, and vice versa.

Signals per platform:

- **Android** — emulator: `Build.HARDWARE` ∈ {`goldfish`, `ranchu`}, `Build.MANUFACTURER`
  contains `Genymotion`, `Build.FINGERPRINT` starts with `generic` or contains `/sdk_`,
  `Build.PRODUCT` contains `sdk`/`emulator`, `/dev/qemu_pipe` or `/dev/socket/qemud`
  exists. Debugger: `android.os.Debug.isDebuggerConnected()` (weight 1.0),
  `waitingForDebugger()` (weight 0.6).
- **iOS** — emulator: `NSProcessInfo.processInfo.environment` contains
  `SIMULATOR_DEVICE_NAME` / `SIMULATOR_MODEL_IDENTIFIER` / `SIMULATOR_HOST_HOME`
  (weight 1.0 each). Debugger: `sysctl` `P_TRACED` probe deferred to a follow-up.
- **JVM** — emulator: `java.vm.name` ∈ {`Dalvik`, `ART`} (weight 0.5, weak signal for
  Android-on-PC runtimes). Debugger: `RuntimeMXBean.inputArguments` carries
  `-agentlib:jdwp`, `-Xrunjdwp`, or `-Xdebug` (weight 1.0).
- **JS** — emulator: `navigator.webdriver === true` for WebDriver/Selenium and most
  headless browsers (weight 0.9). Debugger: `window.outerHeight == 0` DevTools heuristic
  (weight 0.5). Node callers — where `window` is absent — see an empty outcome.

When a threat fires, `EmulatorCheckResult` carries separate `emulatorIndicators` and
`debuggerIndicators` lists so consumers don't need to parse prefixed strings to route
forensic logging. Unlike root detection, no `strict` knob is exposed — the primary
signals are deterministic (weight 1.0) and the threshold is a fixed `0.5`.

## App integrity

`deviceguard-integrity` surfaces app-tampering and dynamic-instrumentation signals. Opt in
via `DeviceGuard.Builder(context).enableIntegrityCheck(expectedSignature, trustedInstallers)`.
Two disjoint indicator streams feed two independent confidences at a fixed `0.5` threshold:
`ThreatType.SignatureMismatch` and `ThreatType.HookFramework`.

```kotlin
val guard = DeviceGuard.Builder(context)
    .enableIntegrityCheck(
        expectedSignature = "1A:2B:3C:…",               // keytool-format accepted
        trustedInstallers = setOf("com.android.vending"),
    )
    .build()
```

Signals per platform:

- **Android** — signing-certificate SHA-256 vs `expectedSignature` (weight 1.0); signing
  read failure (weight 0.5, fires when `PackageManager` can't read the signing info);
  Android Studio's default debug cert SHA-1 (weight 0.4, intentionally weak — swappable
  by the build system); installer outside `trustedInstallers` (weight 0.3, skipped if the
  set is empty). Hook: Xposed installer, LSPosed manager, Virtual-Exposed, or Magisk is
  visible via a single `getInstalledPackages()` call (weight 1.0). The manifest declares
  those packages under `<queries>` so detection works on Android 11+ without
  `QUERY_ALL_PACKAGES`.
- **iOS** — Frida artefacts across traditional and rootless (Dopamine / palera1n) jailbreak
  layouts: FridaGadget framework, `frida-agent` / `frida-gadget` dylibs under
  `/usr/lib/frida/` and `/var/jb/usr/lib/frida/`, `frida-server`, Cycript (weight 1.0
  each). Signature stream: `NSBundle.mainBundle.bundleIdentifier == nil` (weight 0.6).
  Full `SecStaticCodeCheckValidity`, in-memory `_dyld_image_count` scanning, and
  `embedded.mobileprovision` inspection are deferred.
- **JVM / JS** — not applicable; desktop JAR verification and browser SRI have materially
  different threat models.

`IntegrityCheckResult` exposes `signatureMismatch`, `hookFrameworkDetected`,
`signatureCheckRun`, confidences, and separate indicator lists. **Always read
`signatureCheckRun` before treating `signatureMismatch == false` as "signature is
valid"** — on platforms that skip the signature stream both fields are false, and the
detector advertises this via the `integritycheck.signature_check_run` signal. The
`expectedSignature` string is accepted in `keytool`-style `AB:CD:EF:…` hex with
whitespace; the detector normalises to lowercase packed hex before comparison.

## VPN / proxy detection

`deviceguard-network` surfaces active VPN tunnels and HTTP/SOCKS proxy routing. Opt in via
`DeviceGuard.Builder(context).enableNetworkCheck()`. Two disjoint indicator streams feed two
independent confidences at a fixed `0.5` threshold: `ThreatType.VpnActive` and
`ThreatType.ProxyActive`.

VPN / proxy presence is not inherently hostile — corporate deployments, privacy-focused
users, and ISPs all legitimately terminate on tunnels — so the emitted threats carry
intentionally low default weights. The detector surfaces the state; the risk-scoring
strategy decides how much it matters.

Signals per platform:

- **Android** — VPN: `ConnectivityManager.getActiveNetwork()` +
  `getNetworkCapabilities()` reporting `TRANSPORT_VPN` or lacking
  `NET_CAPABILITY_NOT_VPN` (weight 1.0, API 23+ only; API 21/22 falls back to the
  interface scan); `NetworkInterface` named `tun*`/`utun*`/`ipsec*`/`ppp*`/`wg*`/`tap*`
  while up (weight 0.8). Proxy: `http.proxyHost` / `https.proxyHost` (weight 1.0 each);
  `socksProxyHost` (weight 0.9). Module manifest declares `ACCESS_NETWORK_STATE`.
- **JVM / Desktop** — VPN: `NetworkInterface.getNetworkInterfaces()` scan for the same
  prefixes (weight 1.0). Proxy: `http`/`https`/`socks` system properties (1.0 / 1.0 / 0.9)
  plus `ProxySelector.getDefault().select(…)` returning a non-DIRECT proxy (weight 0.8).
- **iOS** — `NotApplicable` pending a dedicated cinterop for `getifaddrs()`. The posix
  bindings shipped by default Kotlin/Native don't expose it, and the
  `SCNetworkInterface` / `NEVPNManager` alternatives need more surface. Deferred to a
  follow-up that won't change the public API.
- **JS / Web** — `NotApplicable`. Reliable in-browser VPN/proxy detection needs
  server-side IP correlation.

`NetworkCheckResult` carries `vpnActive`, `proxyActive`, confidences, and separate
`vpnIndicators` / `proxyIndicators` lists so consumers don't parse prefixes. Interface
matching is case-insensitive (`TAP0` on Windows and `utun3` on macOS both count).

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
