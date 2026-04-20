# DeviceGuard SDK

[![Build](https://github.com/dongnh311/DeviceGuardSDK/actions/workflows/ci.yml/badge.svg)](https://github.com/dongnh311/DeviceGuardSDK/actions/workflows/ci.yml)
[![Docs](https://github.com/dongnh311/DeviceGuardSDK/actions/workflows/docs.yml/badge.svg)](https://dongnh311.github.io/DeviceGuardSDK/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)

> Kotlin Multiplatform SDK for comprehensive device security: fingerprinting, root/jailbreak detection, emulator detection, app integrity, network inspection, remote-control app detection, and surveillance / tampering detection — on Android, iOS, JVM/Desktop, and Web.

**📘 Full documentation:** <https://dongnh311.github.io/DeviceGuardSDK/>

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
    implementation("io.github.dongnh311:deviceguard-remote:0.1.0")
    implementation("io.github.dongnh311:deviceguard-surveillance:0.1.0")
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
    .enableRemoteCheck()
    .enableSurveillanceCheck()
    .build()

val report = guard.analyze()
println("Risk: ${report.riskScore}/100")
println("Threats: ${report.threats}")
println("Device ID: ${report.fingerprint.id}")
```

### Realtime observe() — emit on state change

`analyze()` gives a one-shot snapshot. For continuously monitoring the device, collect
`DeviceGuard.observe(periodMs)`: it polls at the configured interval and only emits when
the threat set or fingerprint actually changes (same threats, same report → no emission).

```kotlin
guard.observe(periodMs = 5_000L).collect { report ->
    updateUi(report)
}
```

Minimum period is `500 ms` — leaves headroom above the 200 ms p95 per-analyze budget.

### Detector × platform matrix — what it does and how it works

Legend: ✅ full coverage · ⚠️ partial / best-effort · — not applicable on this platform (returns `DetectionResult.NotApplicable`).

| Detector | Android | iOS | Desktop JVM | Web (JS) |
|---|---|---|---|---|
| **Fingerprint** (`enableFingerprint`) — stable non-PII device id | ✅ SHA-256 over `Build.MANUFACTURER/BRAND/MODEL/DEVICE/PRODUCT/HARDWARE`, `SDK_INT`, `SUPPORTED_ABIS`, `Settings.Secure.ANDROID_ID`, screen density/resolution, locale, timezone | ✅ SHA-256 over `UIDevice.systemName/version/model`, `identifierForVendor`, screen scale/bounds, locale, timezone | ✅ SHA-256 over `os.name/version/arch`, `java.vendor/version`, locale, timezone, SHA-256(first non-loopback MAC) | ⚠️ SHA-256 over `navigator.userAgent/platform/language/hardwareConcurrency`, screen resolution/colorDepth, timezone (best-effort — UA hardening degrades stability) |
| **RootCheck** (`enableRootCheck`) — root / jailbreak | ✅ 13 `su`/Superuser/Magisk binary paths (w 1.0) · PackageManager scan against 12 root-tooling pkgs (w 0.9) · `Build.TAGS == test-keys` (w 0.3). Tripped at conf ≥ 0.5 (strict: ≥ 0.2) | ✅ 14 jailbreak artifact paths (Cydia, Sileo, Zebra, MobileSubstrate, /bin/bash…) via `NSFileManager` (w 0.9) · sandbox-escape write probe at `/private/…` (w 1.0) | — | — |
| **EmulatorCheck** (`enableEmulatorCheck`) — virtual device + attached debugger (2 independent threats) | ✅ emulator: `Build.HARDWARE` ∈ {goldfish, ranchu}, Genymotion manufacturer, `FINGERPRINT` starts `generic`/contains `/sdk_`, `/dev/qemu_pipe`. Debugger: `Debug.isDebuggerConnected()` (w 1.0) + `waitingForDebugger()` (w 0.6) | ✅ emulator: `NSProcessInfo.environment` contains `SIMULATOR_DEVICE_NAME` / `SIMULATOR_MODEL_IDENTIFIER` / `SIMULATOR_HOST_HOME` (w 1.0). Debugger: `sysctl P_TRACED` deferred | ⚠️ emulator: `java.vm.name ∈ {Dalvik, ART}` (w 0.5 — Android-on-PC only). Debugger: `-agentlib:jdwp` / `-Xrunjdwp` / `-Xdebug` in `RuntimeMXBean.inputArguments` (w 1.0) | ⚠️ emulator: `navigator.webdriver === true` for Selenium/Playwright (w 0.9). Debugger: `window.outerHeight == 0` DevTools heuristic (w 0.5) |
| **IntegrityCheck** (`enableIntegrityCheck`) — signing cert + hook frameworks | ✅ signing cert SHA-256 vs `expectedSignature` (w 1.0) · signing-read failure (w 0.5) · Android Studio default debug cert (w 0.4) · installer outside `trustedInstallers` (w 0.3). Hook: Xposed/LSPosed/VirtualXposed/Magisk visible via one `getInstalledPackages()` + `<queries>` manifest | ⚠️ Frida artefacts (FridaGadget framework, `frida-agent` / `frida-gadget` under `/usr/lib/frida/` + rootless `/var/jb/usr/lib/frida/`, `frida-server`, Cycript — w 1.0 each) + `NSBundle.bundleIdentifier == nil` (w 0.6). Full `SecStaticCodeCheckValidity` + dyld scanning deferred | — (JAR verification different threat model) | — (browser SRI different threat model) |
| **NetworkCheck** (`enableNetworkCheck`) — VPN + proxy (2 independent threats, low default weights) | ✅ VPN: `ConnectivityManager` + `TRANSPORT_VPN` / missing `NET_CAPABILITY_NOT_VPN` (API 23+, w 1.0) · `NetworkInterface` `tun*/utun*/ipsec*/ppp*/wg*/tap*` up (w 0.8). Proxy: `http(s).proxyHost` (w 1.0) · `socksProxyHost` (w 0.9) | — (cinterop for `getifaddrs()` / `NEVPNManager` deferred) | ✅ VPN: `NetworkInterface` scan for same prefixes (w 1.0). Proxy: `http`/`https`/`socks` system properties (1.0 / 1.0 / 0.9) + `ProxySelector.getDefault().select()` non-DIRECT (w 0.8) | — (needs server-side IP correlation) |
| **RemoteCheck** (`enableRemoteCheck`) — remote-control apps + live screen capture | ✅ PackageManager scan against 16 known remote-control pkgs (AnyDesk, TeamViewer, RustDesk, Chrome Remote Desktop…) with `<queries>` manifest · AccessibilityManager running-services scan for known remote services. Fires `RemoteControlInstalled` | ⚠️ `UIScreen.mainScreen.captured` for live screen mirror/record only (fires `ScreenBeingCaptured`). Sandbox blocks app enumeration | ✅ `ProcessHandle.allProcesses()` basename scan for known remote binaries (vncserver, x11vnc, teamviewerd, rustdesk, anydesk, screensharingd on macOS). Fires `RemoteControlInstalled` | — (sandbox) |
| **SurveillanceCheck** (`enableSurveillanceCheck`) — apps that can spy on or interfere with other apps | ✅ 4 categories: AccessibilityManager enabled-services (non-system) → `AccessibilityAbuse` · `Settings.Secure.ENABLED_NOTIFICATION_LISTENERS` (non-system) → `NotificationListener` · `DevicePolicyManager.getActiveAdmins()` (non-system) → `DeviceAdminActive` · `Settings.Secure.DEFAULT_INPUT_METHOD` outside allow-list → `SuspiciousIme` | — (sandbox — jailbreak threat already implies elevated surveillance risk) | ⚠️ `ProcessHandle` basename scan against automation tools (pyautogui, appium, selenium, sikuli, autohotkey, keyclick) → `AutomationToolRunning`, and debuggers (gdb, lldb, strace, dtruss, frida-server) → `DebuggerAttachedElsewhere` | — (sandbox) |

Every detector is optional — only enabled modules run. `analyze()` fans them out in parallel on `Dispatchers.Default`; the orchestrator merges confidences into a single `SecurityReport.riskScore` (0–100) and `riskLevel` (SAFE / LOW / MEDIUM / HIGH / CRITICAL).

## Sample apps

`sample/` ships a Compose Multiplatform demo across every target. The UI lives once in
`sample/shared/` (Material 3, Compose Multiplatform 1.7.x); each platform wrapper is a
thin entry point that hosts the shared `App()` composable.

Every app shows the same screen: toggle detectors, tap **Analyze**, inspect the live
`SecurityReport` — risk score with a coloured progress bar, threat list, fingerprint id,
error list, and end-to-end timing.

| App | CI | Run locally |
|-----|----|-------------|
| `sample/shared` | linked via CI builds below | — (library) |
| `sample/android` | ✅ `assembleDebug` | `./gradlew :sample:android:installDebug` (device or emulator), or open in Android Studio |
| `sample/desktop` | ✅ `jar` | `./gradlew :sample:desktop:run` |
| `sample/web` | ✅ `jsBrowserProductionWebpack` (artefact uploaded) | `./gradlew :sample:web:jsBrowserDevelopmentRun` → <http://localhost:8080> |
| `sample/ios` | ✅ `xcodegen` + `xcodebuild` on Simulator | `cd sample/ios && xcodegen && open DeviceGuardSample.xcodeproj`; then Run in Xcode |

> **End-to-end smoke-verified on 2026-04-20** across all 4 samples with the full
> 7-detector build enabled:
> Desktop headless `./gradlew :sample:desktop:run` — report produced;
> Android (Pixel 6 API 34 emulator) — 2 threats, 16 signals, MEDIUM risk, no false positives on the new Remote/Surveillance toggles;
> iOS (iPhone 16 Simulator, iOS 18) — CRITICAL risk on simulator (expected: `Emulator` + `ScreenBeingCaptured` on shared display);
> Web (Chromium headless @ `jsBrowserProductionWebpack` output) — LOW risk, 1 threat, 13 signals, 21 ms elapsed.

### Manual QA matrix

Before the first release, each threat path must fire at least once on a real target. The
table below is the checklist for that sweep — tick boxes as devices are exercised.

| Platform | Environment | Expected primary threats | Status |
|----------|-------------|--------------------------|--------|
| Android | Stock Pixel / Samsung / Xiaomi | `SAFE` or `LOW` | ☐ |
| Android | AOSP emulator (Google APIs image) | `Root`, `Emulator` | ☐ |
| Android | Google Play emulator image | `Emulator` only | ☐ |
| Android | Rooted device + Magisk (no hide) | `Root`, `HookFramework` if Zygisk modules | ☐ |
| Android | Device on corporate VPN | `VpnActive` | ☐ |
| Android | Device with `http.proxyHost` set | `ProxyActive` | ☐ |
| Android | Re-signed APK, wrong expected signature | `SignatureMismatch` | ☐ |
| iOS | Stock iPhone | `SAFE` | ☐ |
| iOS | iOS Simulator | `Emulator` | ☐ |
| iOS | Jailbroken iPhone (palera1n / Dopamine) | `Jailbreak`, possibly `HookFramework` | ☐ |
| JVM / Desktop | Plain launch | `SAFE` | ☐ |
| JVM / Desktop | Launched under IntelliJ Debug (JDWP) | `DebuggerAttached` | ☐ |
| JVM / Desktop | `-Dhttp.proxyHost=proxy.example.com` | `ProxyActive` | ☐ |
| JVM / Desktop | Connected to VPN (utun / tun interface up) | `VpnActive` | ☐ |
| JS / Web | Chrome, normal session | `SAFE` | ☐ |
| JS / Web | Chrome with DevTools open | possibly `DebuggerAttached` | ☐ |
| JS / Web | Selenium / Playwright automation | `Emulator` (`navigator.webdriver`) | ☐ |

The v0.1.0 publish (Phase 9) is gated on every row above being confirmed on a real host.
Recording a short video or sharing the `Copy JSON report` payload per row is enough
evidence — attach to the release notes PR.

## Modules

| Module | Status | Purpose |
|--------|--------|---------|
| `deviceguard-core` | ✅ available | Public API, models, orchestrator |
| `deviceguard-fingerprint` | ✅ available | Stable cross-platform device ID |
| `deviceguard-rootcheck` | ✅ available | Root / Jailbreak detection |
| `deviceguard-emulator` | ✅ available | Emulator / Debugger detection |
| `deviceguard-integrity` | ✅ available | App tampering & hook detection |
| `deviceguard-network` | ✅ available | VPN / Proxy / Tor inspection |
| `deviceguard-remote` | ✅ available | Remote-control apps & screen-capture detection |
| `deviceguard-surveillance` | ✅ available | Accessibility / notification-listener / device-admin / IME abuse + automation-tool detection |
| `deviceguard-bom` | ✅ available | Bill of Materials for version alignment |

## Supported platforms

| Platform | Minimum | Notes |
|----------|---------|-------|
| Android | API 21 (Lollipop) | full detector coverage — see matrix above |
| iOS | 13.0 | Fingerprint / RootCheck (jailbreak) / EmulatorCheck / IntegrityCheck / RemoteCheck (screen-capture only). Network deferred (cinterop). Surveillance N/A (sandbox) |
| JVM / Desktop | JDK 17 | Fingerprint / EmulatorCheck (JDWP) / NetworkCheck / RemoteCheck / SurveillanceCheck (best-effort via `ProcessHandle`). Root / Integrity N/A |
| JS / Web | ES2020 browser | Fingerprint (best-effort) / EmulatorCheck (webdriver). Root / Integrity / Network / Remote / Surveillance all N/A — sandbox |

See the detector × platform matrix above for what each detector does on each OS, including the signal weights and how they're collected.

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
./gradlew build             # all targets
./gradlew allTests          # run all platform tests
./gradlew dokkaHtml         # generate API docs
./gradlew koverHtmlReport   # aggregated code coverage (HTML)
./gradlew koverXmlReport    # aggregated code coverage (XML, for CI ingest)
```

## Testing & coverage

Tests run at two layers:

- **Common logic** — orchestrator, scoring, serialization, threat/result validation, and every
  detector's threshold + threat-emission paths live in each module's `commonTest` source set
  and execute on JVM, JS, Android unit, and iOS simulator runners.
- **Platform adapters** — `jvmTest` source sets exercise the desktop probes directly:
  fingerprint signals (`os.*`, `jvm.*`, `net.mac_hash` shape), proxy detection via
  `System.getProperty` + `ProxySelector`, and the `NotApplicable` bypasses for rootcheck
  and integrity on desktop. Android-instrumented, iOS-device, and JS browser tests remain
  follow-up work.

Coverage reports are produced by the [Kover](https://github.com/Kotlin/kotlinx-kover) plugin
and aggregated at the root. After `./gradlew koverHtmlReport`, open
`build/reports/kover/html/index.html`.

A JVM p95 latency benchmark
(`deviceguard-core/src/jvmTest/.../AnalyzePerfBenchmarkJvmTest.kt`) guards the orchestrator's
overhead against the SDK's 200ms p95 budget on a mid-tier device. It runs synthetic stub
detectors on `Dispatchers.Default`, so regressions that drop detector parallelism or add
per-run blocking work fail the suite.

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for dev setup and the PR workflow. Participation is
governed by our [Code of Conduct](CODE_OF_CONDUCT.md). Release history is tracked in
[`CHANGELOG.md`](CHANGELOG.md).

## Security

Do **not** file a public issue for security vulnerabilities. Follow the private disclosure
process in [`SECURITY.md`](SECURITY.md).

## License

Apache License 2.0 — see [`LICENSE`](LICENSE).
