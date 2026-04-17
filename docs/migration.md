# Migration Guide

DeviceGuard follows [Semantic Versioning](https://semver.org/) from `v0.1.0` onwards.
Breaking changes bump the **major** version; additive changes bump **minor**; fixes bump
**patch**. This page records the migration path between releases and the mechanics the
SDK uses to stay source-compatible where possible.

## `@ExperimentalDeviceGuardApi`

Any public symbol annotated with `@ExperimentalDeviceGuardApi` is subject to change in a
*minor* release without a deprecation cycle. Using such a symbol requires an explicit
opt-in:

```kotlin
@OptIn(ExperimentalDeviceGuardApi::class)
val guard = DeviceGuard.Builder(context)
    .enableBleedingEdgeDetector()  // hypothetical experimental hook
    .build()
```

You can also enable it project-wide via the compiler flag:

```kotlin title="build.gradle.kts"
kotlin {
    compilerOptions {
        freeCompilerArgs.add(
            "-opt-in=io.github.dongnh311.deviceguard.core.ExperimentalDeviceGuardApi",
        )
    }
}
```

If a symbol loses its `@ExperimentalDeviceGuardApi` annotation in a release, treat that as
a public-API commitment; from then on it only changes on a *major* bump.

## Report schema versioning

`SecurityReport.schemaVersion` is an integer that bumps when the on-wire JSON layout
changes in a way old parsers cannot handle. The `fromJson()` decoder is configured with
`ignoreUnknownKeys = true` and `explicitNulls = false`, so *additive* changes (new
fields, optional values) do not bump `schemaVersion`.

When `schemaVersion` bumps, this page documents the before / after of the JSON layout and
the recommended handling for a rolling deploy where client and server versions differ.

| Schema | Introduced in | Change |
|--------|---------------|--------|
| 1      | 0.1.0         | Initial layout — `riskScore`, `threats`, `fingerprint?`, `signals`, `errors`, `analyzedAtEpochMillis`. |

## Unreleased → 0.1.0

First tagged release. No migration required — APIs land stable at 0.1.0.

Pre-`0.1.0` snapshot consumers: the **only** breaking change between the first snapshot
and `0.1.0` is artefact publishing. Snapshots were consumed via `includeBuild` or composite
builds; `0.1.0` is available from Maven Central. Replace your local builds with:

```kotlin
dependencies {
    implementation(platform("io.github.dongnh311:deviceguard-bom:0.1.0"))
    implementation("io.github.dongnh311:deviceguard-core")
    // etc.
}
```

Detector ids, threat ids, and the `SecurityReport` layout carry across unchanged.

## How the SDK avoids churn

- **`expect` / `actual` for platform adapters.** Adding a new platform is not a breaking
  change for anyone on existing targets; absence of a platform is signalled via
  `DetectionResult.NotApplicable` rather than a compile error.
- **`ignoreUnknownKeys` on decode.** An old client deserializing a new report will
  silently drop fields it does not recognise. Server-side, the reverse is true: an old
  server parsing a new client's payload will succeed as long as `schemaVersion` has not
  bumped.
- **Data-class copy semantics.** Adding a field with a default value to any of
  `SecurityReport`, `DetectedThreat`, `DeviceFingerprint`, `DetectorError` is source- and
  binary-compatible for consumers that use named-argument construction. Positional
  constructors are discouraged in the KDoc for this reason.
- **Threat weights live in `ThreatType`.** Changing a weight is a minor bump. Overriding
  via `DetectedThreat.of(threat, weight = …)` is fully supported for applications with
  their own risk tolerance.

## Upgrading

```bash
./gradlew dependencies | grep deviceguard
./gradlew :deviceguard-core:dependencyInsight --dependency deviceguard-core
```

After a minor or major bump, re-run your test suite and look for:

- Compile errors referencing types in `io.github.dongnh311.deviceguard.*` — either a
  deprecation or a removed experimental symbol.
- Runtime `DetectionResult.NotApplicable` entries on platforms where the old version
  returned `Success` with empty signals — this is a semantic fix, not a regression.
- Schema-version mismatches on the server — compare `schemaVersion` against the table
  above and either upgrade the server or let the old reader ignore unknown fields.

Post any surprises as a GitHub issue with the `migration` label and we'll fold them into
this page.
