# DeviceGuard SDK — Claude Code Context

## Project Overview

**DeviceGuard SDK** is a Kotlin Multiplatform (KMP) library providing comprehensive device security detection across Android, iOS, JVM/Desktop, and JS/Web.

- **Language:** Kotlin 2.x (K2 compiler) with `expect`/`actual` pattern
- **Build:** Gradle 8.5+ with version catalog (`gradle/libs.versions.toml`)
- **Distribution:** Maven Central (open source, Apache 2.0)
- **Publishing plugin:** `vanniktech.maven.publish`
- **Docs:** Dokka for API reference
- **CI:** GitHub Actions (Linux + macOS runners)

The single authoritative plan lives at `~/Downloads/DEVICEGUARD_PLAN.md`. Phases are tracked there; each phase ships as its own PR.

## Module Architecture

```
deviceguard/
├── deviceguard-core/          # Public API, models, orchestrator
├── deviceguard-fingerprint/   # Device fingerprinting
├── deviceguard-rootcheck/     # Root / Jailbreak detection
├── deviceguard-emulator/      # Emulator / Debugger detection
├── deviceguard-integrity/     # App tampering detection
├── deviceguard-network/       # VPN / Proxy / Network detection
├── deviceguard-bom/           # Bill of Materials
└── sample/                    # Demo apps (Android, iOS, Desktop, Web)
```

Consumers can pull individual modules or the full bundle via `deviceguard-core`.

## Target Platforms (source sets)

- `commonMain` — shared Kotlin logic, public API, orchestration
- `androidMain` — Android-specific detectors (PackageManager, Build, etc.)
- `iosMain` — iOS-specific detectors (UIKit, sysctl, Security framework)
- `jvmMain` — Desktop JVM detectors (system properties, NetworkInterface)
- `jsMain` — Browser detectors (navigator, screen, WebRTC)
- `commonTest` / `<platform>Test` — per-set tests

## Public API Shape (target)

```kotlin
val guard = DeviceGuard.Builder(context)
    .enableFingerprint()
    .enableRootCheck(strict = true)
    .enableEmulatorCheck()
    .enableIntegrityCheck(expectedSignature = "…")
    .enableNetworkCheck()
    .build()

val report: SecurityReport = guard.analyze()
report.riskScore      // 0–100
report.fingerprint    // stable device ID
report.threats        // List<ThreatType>
report.signals        // Map<String, Any> raw data
```

Core types: `SecurityReport`, `ThreatType` (sealed), `RiskLevel` (enum), `DetectionResult<T>`, `DeviceFingerprint`, `Detector` (interface all modules implement).

## Development Workflow

1. One PR per phase (see `DEVICEGUARD_PLAN.md` §5).
2. `./gradlew build` must be green on every target before merging.
3. Public API changes require KDoc on every symbol.
4. Unstable APIs gated behind `@ExperimentalDeviceGuardApi`.
5. Semantic versioning — no breaking changes without major bump.

## Quality Gates

- Detekt: zero critical warnings
- ktlint: enforced in CI
- Coverage: ≥80% common code, ≥60% platform code
- `analyze()` p95 < 200ms on mid-tier device
- All public symbols have KDoc

## Commands

```bash
./gradlew build              # build all targets
./gradlew detekt             # static analysis
./gradlew ktlintCheck        # style check
./gradlew ktlintFormat       # auto-format
./gradlew allTests           # run all platform tests
./gradlew dokkaHtml          # generate API docs
./gradlew publishToMavenLocal
```

## Conventions

- **Package:** `io.github.dongnh311.deviceguard.*` (module-scoped subpackages)
- **Group ID:** `io.github.dongnh311`
- **Platform checks:** prefer `expect`/`actual` over runtime dispatch
- **No PII collection** in fingerprinting; document every collected signal
- **False positives:** return confidence scores, never boolean-only

## Not in scope

- SafetyNet / Play Integrity wrapping (document hooks; let consumers integrate)
- Server-side attestation verification (client library only)
- Native C/C++ code (pure Kotlin + platform APIs)
