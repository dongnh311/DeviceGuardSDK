# Changelog

All notable changes to the DeviceGuard SDK are recorded in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project
aims to follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html) from the first
tagged release (`v0.1.0`). Until then, the API is unstable — breaking changes are called
out inline and collapsed into the `0.1.0` entry on release.

## [0.1.0] — 2026-04-22

First Maven Central release. All modules published under group
`io.github.dongnh311` with artifact IDs matching the module names
(`deviceguard-core`, `deviceguard-fingerprint`, ...) and a
`deviceguard-bom` for coordinated versioning.

## [Unreleased]

### Added

- Kotlin Multiplatform project skeleton targeting Android (API 21+), iOS (13+),
  JVM / Desktop, and JS (browser + Node), with explicit `expect` / `actual` per detector.
- `deviceguard-core` — public API surface: `DeviceGuard.Builder`, `SecurityReport`,
  `ThreatType` sealed hierarchy, `RiskLevel` bucketing, `DetectedThreat`, `DetectionResult`,
  `Detector` interface, `DeviceGuardLogger`, weighted-sum risk scoring.
- `deviceguard-fingerprint` — cross-platform stable device id: SHA-256 over length-prefixed
  signal map. Per-platform non-PII signals (Build props, UIDevice, JVM system properties,
  navigator / screen). JVM MAC address is hashed at collection so raw bytes never leak.
- `deviceguard-rootcheck` — Android root detection (su binaries, Magisk / SuperSU packages,
  `test-keys` build tag) and iOS jailbreak detection (known paths, URL scheme probes, fork
  syscall, dyld image inspection). Confidence-based with `strict` tightening the threshold.
- `deviceguard-emulator` — Android emulator artefacts (goldfish / ranchu / qemu), debugger
  checks, sensor count. iOS simulator check via `NSProcessInfo.environment`
  (`SIMULATOR_DEVICE_NAME` / `SIMULATOR_MODEL_IDENTIFIER` / `SIMULATOR_HOST_HOME`); the
  `P_TRACED` debugger probe via `sysctl` is documented but deferred pending cinterop
  struct handling. JVM JDWP agent detection + Dalvik / ART hint. JS
  `navigator.webdriver` + `window.outerHeight` heuristics.
- `deviceguard-integrity` — Android signing-certificate SHA-256 comparison, installer
  verification, Frida / Xposed hook artefact scan. iOS Frida artefact scan.
- `deviceguard-network` — VPN interface / transport detection and HTTP / HTTPS / SOCKS
  proxy detection on Android and JVM. iOS + JS mark themselves `NotApplicable` with a
  reason for now.
- `deviceguard-remote` — remote-control app & live-screen-capture detection. Android
  scans PackageManager against 16 known remote-control packages (AnyDesk, TeamViewer,
  RustDesk, Chrome Remote Desktop, …) plus AccessibilityManager running-services; JVM
  scans `ProcessHandle.allProcesses()` basenames against known remote binaries including
  `screensharingd` on macOS; iOS uses `UIScreen.mainScreen.captured` for screen-mirror
  / record detection only; Web is `NotApplicable` (sandbox).
- `deviceguard-surveillance` — apps that can spy on or interfere with other apps.
  Android categories: AccessibilityAbuse, NotificationListener, DeviceAdminActive,
  SuspiciousIme. JVM categories: AutomationToolRunning, DebuggerAttachedElsewhere via
  process-basename scan. iOS + Web `NotApplicable`.
- `DeviceGuard.observe(periodMs)` — realtime `Flow<SecurityReport>` that polls at the
  configured interval and only emits when the threat set or fingerprint changes
  (`distinctUntilChanged`). Minimum period 500 ms.
- `deviceguard-bom` — Bill-of-Materials java-platform module for pinning subproject
  versions together.
- Build + tooling: Gradle version catalog (`libs.versions.toml`), Detekt + ktlint enforced
  in CI, Dokka HTML API docs, vanniktech maven publish plugin wired for `0.1.0-SNAPSHOT`.
- GitHub Actions CI matrix — Linux runner (JVM + JS + Android unit), macOS runner (iOS +
  full matrix), Lint job, Dokka job.
- Kover 0.8.3 plugin with aggregate `koverHtmlReport` across every KMP module.
- Repo hooks — `.claude/hooks/require-simplify.sh` gates `gh pr create` on a `simplify:` /
  `review:` sentinel commit; `.claude/hooks/require-readme-touch.sh` gates the same call on
  a README edit when the diff touches public-facing source.
- JVM-layer tests for fingerprint signals, proxy detection, emulator probe, and the
  `NotApplicable` bypasses. Core commonTest pins `DetectedThreat.require()` ranges. A
  jvmTest p95 latency benchmark runs `analyze()` on `Dispatchers.Default` and asserts
  the 200ms budget is held under 100 samples.

### Security

- Fingerprint deliberately excludes IMEI, advertising id, account name, email, and other
  directly identifying values. The only per-device-unique signal is a SHA-256 hash of the
  first non-loopback MAC address on JVM — raw bytes never enter the signals map.
- App-integrity signature compare accepts `keytool`-formatted colon-separated hex and
  normalizes to lowercase before compare, so a trailing newline or extra whitespace in the
  expected value cannot cause a silent mismatch.

[Unreleased]: https://github.com/dongnh311/DeviceGuardSDK/commits/main
