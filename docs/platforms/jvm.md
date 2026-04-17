# JVM / Desktop

The JVM target covers two use cases:

- **JVM-backed desktop apps** — Compose Desktop, Swing, JavaFX — where the end user is
  running your binary on macOS, Windows, or Linux.
- **Server-side test harnesses** — running detector logic in CI against known signal
  fixtures, for example to verify a `@Serializable` round-trip or a golden test.

## Target compatibility

- **JVM target:** Java 17 bytecode. Older JDKs will fail at class-load time.
- **Toolchain:** the Gradle build pins `jvmTarget = JVM_17` across every module. If you
  consume DeviceGuard from a multi-release JAR of your own, make sure the `Multi-Release`
  manifest picks up the correct versioned class for Java 8 / 11 targets.

## Context handle

Desktop JVM does not need a platform handle:

```kotlin
val guard = DeviceGuard.Builder(DeviceGuardContext()).build()
```

## Applicability matrix

| Detector | JVM behaviour | Reason |
|----------|---------------|--------|
| Fingerprint | ✅ | Collects OS / JVM / locale / timezone + a SHA-256 of the first non-loopback MAC (raw bytes never leave the detector). |
| Root check | `NotApplicable` | Root / jailbreak has no equivalent on desktop JVM. |
| Emulator | ✅ | Detects `-agentlib:jdwp` / `-Xrunjdwp` / `-Xdebug` in JVM args (weight 1.0) and `java.vm.name == Dalvik / ART` (weight 0.5). |
| Integrity | `NotApplicable` | JAR signature verification and class-modification checksums ship in a later release. |
| Network | ✅ | `System.getProperty("http.proxyHost" / "https.proxyHost" / "socksProxyHost")`, custom `ProxySelector`, and `NetworkInterface` enumeration for `tun` / `utun` / `ipsec` / `ppp` / `wg` / `tap` prefixes. |

## Running only a subset

If your desktop app does not care about network inspection, omit the
`enableNetworkCheck()` call; the builder is additive.

```kotlin
val guard = DeviceGuard.Builder(DeviceGuardContext())
    .enableFingerprint()
    .enableEmulatorCheck()
    .build()
```

Detectors you do not add never run; the orchestrator has no "run everything available"
mode by design.

## What the JDWP probe catches

`ManagementFactory.getRuntimeMXBean().inputArguments` is the canonical way to read the
flags the JVM was started with. The probe looks for any of:

- `-agentlib:jdwp=...`
- `-Xrunjdwp:...`
- `-Xdebug` (legacy)

Any one of these trips `ThreatType.DebuggerAttached` at weight 1.0. Running tests in
IntelliJ's "Debug" mode or attaching from a remote JDWP client will flag the process —
this is correct.

## MAC address hashing

`FingerprintSignals.jvm.kt` enumerates `NetworkInterface.getNetworkInterfaces()` and picks
the first non-loopback, non-virtual, up interface. Its hardware address is hashed with
SHA-256 at collection time; the raw bytes are not added to the signal map. Consumers see
`net.mac_hash = <64 hex chars>` — a stable per-device token that does not leak the MAC.

On CI runners or sandboxed environments without a hardware interface, the key is simply
absent. No platform-specific fallback inserts a synthetic MAC.

## Verifying locally

```bash
./gradlew jvmTest                 # run every module's jvmTest source set
./gradlew :deviceguard-network:jvmTest  # a single module
```

The JVM test suite exercises the actual `runNetworkCheck`, `collectFingerprintSignals`,
and `runEmulatorCheck` entry points — not common-layer scaffolding. Failures are real.
