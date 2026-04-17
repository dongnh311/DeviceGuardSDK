# DeviceGuard SDK

DeviceGuard SDK is a Kotlin Multiplatform library that surfaces device-security signals from
a single call site on Android, iOS, JVM / Desktop, and the browser.

```kotlin
val report = DeviceGuard.Builder(context)
    .enableFingerprint()
    .enableRootCheck(strict = true)
    .enableEmulatorCheck()
    .enableIntegrityCheck(expectedSignature = "…")
    .enableNetworkCheck()
    .build()
    .analyze()

report.riskScore       // 0..100
report.riskLevel       // SAFE | LOW | MEDIUM | HIGH | CRITICAL
report.threats         // List<DetectedThreat>
report.fingerprint     // stable device id
report.signals         // raw per-detector signals for telemetry
```

## What it covers

Each capability ships as its own module so you can pull only what you need:

| Module | Purpose |
|--------|---------|
| `deviceguard-core` | Public API, orchestrator, scoring, report |
| `deviceguard-fingerprint` | SHA-256 stable device id from non-PII signals |
| `deviceguard-rootcheck` | Root on Android, jailbreak on iOS |
| `deviceguard-emulator` | Emulator, simulator, attached-debugger probes |
| `deviceguard-integrity` | Signature verification + hook-framework detection |
| `deviceguard-network` | VPN and proxy detection |
| `deviceguard-bom` | Bill of Materials for version alignment |

## What it is not

- **Not a server-side attestation replacement.** Client-side detection is a signal, not
  proof. Combine DeviceGuard with Play Integrity API / Apple DeviceCheck / server-side
  IP correlation for anything security-critical.
- **Not a fingerprinting tracker.** The fingerprint module hashes non-PII platform signals
  and is stable across launches but invalidates on factory reset / MAC change. IMEI,
  advertising id, account identifiers are explicitly not collected.
- **Not bypass-proof.** Attackers with root + Frida + kernel control can defeat any
  client-side SDK. DeviceGuard raises the floor, not the ceiling — see
  [Security Considerations](security-considerations.md).

## Where to go next

- **[Getting Started](getting-started.md)** — installation, first `analyze()` call, what
  each `enableX()` turns on.
- **[Architecture](architecture.md)** — how detectors, the orchestrator, scoring, and the
  `SecurityReport` fit together.
- **[Platform Guides](platforms/index.md)** — per-platform setup (manifest entries, Info.plist
  keys) and what signals are available where.
- **[Security Considerations](security-considerations.md)** — false-positive tuning,
  confidence vs. strict thresholds, recommended server-side layering.
- **[Migration Guide](migration.md)** — report schema versioning and the
  `@ExperimentalDeviceGuardApi` opt-in policy.
