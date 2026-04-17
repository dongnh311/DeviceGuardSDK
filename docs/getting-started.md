# Getting Started

## Installation

!!! warning "Pre-release"
    DeviceGuard is at `0.1.0-SNAPSHOT`. Coordinates below will be published after the
    first tagged release. Until then, consume from source via
    `includeBuild` or a composite build.

```kotlin title="build.gradle.kts"
dependencies {
    implementation("io.github.dongnh311:deviceguard-core:0.1.0")

    // Opt in to the detectors you need. Each is an independent module.
    implementation("io.github.dongnh311:deviceguard-fingerprint:0.1.0")
    implementation("io.github.dongnh311:deviceguard-rootcheck:0.1.0")
    implementation("io.github.dongnh311:deviceguard-emulator:0.1.0")
    implementation("io.github.dongnh311:deviceguard-integrity:0.1.0")
    implementation("io.github.dongnh311:deviceguard-network:0.1.0")
}
```

### Using the BoM

Align every DeviceGuard artefact to a single version via the Bill-of-Materials module:

```kotlin title="build.gradle.kts"
dependencies {
    implementation(platform("io.github.dongnh311:deviceguard-bom:0.1.0"))
    implementation("io.github.dongnh311:deviceguard-core")
    implementation("io.github.dongnh311:deviceguard-fingerprint")
    // …
}
```

## Your first `analyze()`

```kotlin
import io.github.dongnh311.deviceguard.core.DeviceGuard
import io.github.dongnh311.deviceguard.fingerprint.enableFingerprint
import io.github.dongnh311.deviceguard.rootcheck.enableRootCheck
import io.github.dongnh311.deviceguard.emulator.enableEmulatorCheck
import io.github.dongnh311.deviceguard.integrity.enableIntegrityCheck
import io.github.dongnh311.deviceguard.network.enableNetworkCheck

suspend fun snapshot(context: DeviceGuardContext): SecurityReport =
    DeviceGuard.Builder(context)
        .enableFingerprint()
        .enableRootCheck(strict = false)
        .enableEmulatorCheck()
        .enableIntegrityCheck(
            expectedSignature = BuildConfig.RELEASE_CERT_SHA256,
            trustedInstallers = setOf("com.android.vending"),
        )
        .enableNetworkCheck()
        .build()
        .analyze()
```

`DeviceGuardContext` is constructed from your host:

=== "Android"

    ```kotlin
    val context = DeviceGuardContext(androidContext)
    ```

=== "iOS / JVM / JS"

    ```kotlin
    // No platform handle needed; detectors reach platform APIs directly.
    val context = DeviceGuardContext()
    ```

## Reading the report

```kotlin
val report = snapshot(context)

when (report.riskLevel) {
    RiskLevel.SAFE, RiskLevel.LOW -> allow()
    RiskLevel.MEDIUM -> challenge()
    RiskLevel.HIGH, RiskLevel.CRITICAL -> deny()
}

telemetry.emit("device_snapshot") {
    put("risk", report.riskScore)
    put("device_id", report.fingerprint?.id)
    put("threats", report.threats.map { it.type })
    putAll(report.signals)
}
```

### What `riskScore` means

Each detector emits zero or more `DetectedThreat`s. Each threat carries a **weight**
(scale of severity) and a **confidence** (`0f..1f`). The default `WeightedSumScoring`
aggregates `sum(weight × confidence)` and clamps to `0..100`. See
[Architecture](architecture.md#risk-scoring) for the exact rules and how to plug a
custom strategy.

### Errors vs. `NotApplicable`

A detector that cannot run on the current platform returns
`DetectionResult.NotApplicable` — these are logged, never counted as errors, and never
move the score. A detector that throws or hit a real failure surfaces on
`report.errors`, again without moving the score. This lets you build cross-platform
code that gracefully degrades: the JVM / JS paths simply skip root/integrity probes.

## Next steps

- [Platform Guides](platforms/index.md) for manifest / Info.plist entries you may need.
- [Security Considerations](security-considerations.md) before you gate production
  flows on the output.
