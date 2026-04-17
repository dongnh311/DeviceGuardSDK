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

## PR workflow — `/simplify` before `gh pr create`

Never open a PR on raw first-pass code. Every PR branch must go through a
review pass before `gh pr create`:

1. Finish the feature work and make `./gradlew build` green.
2. Run **`/simplify`** on the branch diff. The skill fans out three agents
   (code reuse, code quality, efficiency) in parallel. Read all three
   reports.
3. Apply the high-confidence findings directly. Skip false positives without
   arguing — note them and move on.
4. Commit the simplify fixes with a subject starting `simplify:` or `review:`
   (or anywhere containing the phrase `simplify review`). Example:
   `review: simplify Phase N — drop dead code, cut hot-path cost`.
5. Push the branch.
6. Run `gh pr create`.

A PreToolUse hook at `.claude/hooks/require-simplify.sh` enforces this. It
inspects the last 10 commits on the current branch (relative to `origin/main`)
for the sentinel commit and blocks `gh pr create` if none is found. The hook
leaves `gh pr view`, `gh pr list`, `gh pr comment`, `gh pr merge`, etc.
untouched — only creation is gated.

**Review rules for `/simplify`:**

- **Reuse agent** — flag hand-rolled logic that stdlib / kotlinx / project
  helpers already cover (e.g. `sumOf`/`coerceIn` instead of a manual
  accumulator, `Clock.System.now()` instead of hand-rolled epoch math).
- **Quality agent** — derivable state, parameter sprawl, copy-paste variants
  (sealed hierarchies with repeated overrides are the common culprit), leaky
  public API (`internal` candidates that slipped out as `public`, annotations
  abused as test-only escape hatches), stringly-typed code, over-validation
  in `init` blocks, redundant KDoc that restates the identifier, and tests
  that exercise nothing the siblings don't already cover.
- **Efficiency agent** — hot-path allocations, eager log-message
  interpolation when the sink is a `NoOp`, sequential work that could be
  parallel, JSON `encodeDefaults` bloat, linear scans that could saturate
  early, and cancellation-catch ordering bugs.

The DeviceGuard SDK has a hard perf budget — `analyze()` p95 < 200ms on a
mid-tier device — so the efficiency agent's findings are not optional.

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
