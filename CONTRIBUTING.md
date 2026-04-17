# Contributing to DeviceGuard SDK

Thanks for considering a contribution. This guide covers local setup, the PR workflow, and
the conventions the repo enforces through hooks.

## Prerequisites

- **JDK 17** (Temurin or any distribution) — required for the JVM target, the Gradle
  daemon, and Android compilation.
- **Android SDK** with platform `android-34` and build-tools `34.x`. Point
  `$ANDROID_HOME` or create `local.properties` with `sdk.dir=/path/to/sdk`.
- **Xcode 15+** on macOS — required only for iOS targets (`iosArm64`, `iosX64`,
  `iosSimulatorArm64`). Linux contributors can skip iOS and still build / test everything
  else; the CI macOS runner covers the iOS matrix.
- **Node 20+** — the JS target pulls Node automatically via Gradle, but having a local
  Node installed speeds the first build.

Clone, then:

```bash
./gradlew build              # Linux-compatible targets (JVM + JS + Android unit)
./gradlew iosSimulatorArm64Test   # macOS only
```

## Day-to-day commands

```bash
./gradlew detekt             # static analysis
./gradlew ktlintCheck        # style
./gradlew ktlintFormat       # auto-format
./gradlew allTests           # every platform test
./gradlew dokkaHtml          # API reference
./gradlew koverHtmlReport    # coverage (build/reports/kover/html/)
./gradlew publishToMavenLocal
```

## PR workflow

Each phase of the SDK roadmap ships as its own PR. A PR is ready to open once:

1. The feature work is complete and every target builds — `./gradlew build` green on
   whatever platforms you have locally; CI covers the rest.
2. A `/simplify`-style review pass has been done on the branch diff — read the reuse,
   quality, and efficiency angles, apply the high-confidence findings, skip false positives.
3. `README.md` reflects any change to public-facing surface (module table, install snippet,
   platform status, quick-start API, feature list). The PR-gate hook blocks creation when
   source files in the publishable set change but the README is untouched.
4. The commit that applies the review findings carries a sentinel — a subject starting with
   `simplify:` or `review:`, or containing the phrase `simplify review`. The PR-gate hook
   inspects the last 10 commits on the branch for this.
5. Append a test plan and scope callouts to the PR body so the reviewer knows what ran and
   what was deliberately deferred.

Example commit subject:

```
review: Phase N /simplify pass

Fixes applied:
- <finding 1>
- <finding 2>
```

Or, folded into the feature commit directly:

```
Phase N: <short feature summary> — review: simplify <notes>
```

Both satisfy the hook.

## Code conventions

- `explicitApi()` is on for every module — public symbols need explicit `public` and a
  KDoc that carries the *why* more than the *what*. Identifiers carry the *what*.
- Unstable or evolving APIs belong behind the `@ExperimentalDeviceGuardApi` opt-in.
- Prefer `expect` / `actual` over runtime platform dispatch. Platform adapters should keep
  the shared `commonMain` surface small and focused.
- Detectors never throw — catch at the boundary and return `DetectionResult.Failed` with a
  forensic message, or `DetectionResult.NotApplicable` if the platform simply has no
  equivalent probe.
- No PII in fingerprint signals. Document every collected field in the module's section of
  `README.md`.
- Tests prefer the existing `testContext()` / `fakeContext()` fixtures per module — each
  sub-module has an `expect`/`actual` fixture pair already.

## Running a subset

- Single module:
  `./gradlew :deviceguard-network:build`
- JVM only across every module:
  `./gradlew jvmTest`
- iOS simulator across every module:
  `./gradlew iosSimulatorArm64Test`

## Reporting issues

Functional bugs → GitHub Issues.
Security vulnerabilities → see [SECURITY.md](SECURITY.md); do not file a public issue.

## Code of Conduct

Participation in this project is covered by the [Contributor Covenant](CODE_OF_CONDUCT.md).
By contributing you agree to abide by its terms.
