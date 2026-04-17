# Architecture

DeviceGuard is a thin orchestrator over independent `Detector` modules. Nothing in the
architecture is magical — if you understand these four primitives you can read the rest of
the source in one sitting.

## The four primitives

### `Detector<T>`

A detector is an interface with a stable `id` and a `suspend` method that returns a
`DetectionResult<T>`:

```kotlin
public interface Detector<out T> {
    public val id: String
    public suspend fun detect(context: DeviceGuardContext): DetectionResult<T>
}
```

Implementations must **never throw**. A platform-specific probe that genuinely fails
catches at the boundary and returns `DetectionResult.Failed(detectorId, message, errorType)`.
A probe that does not apply to the host platform (e.g. root detection on JVM) returns
`DetectionResult.NotApplicable(detectorId, reason)`.

### `DetectionResult<T>`

```kotlin
public sealed interface DetectionResult<out T> {
    public data class Success<T>(
        override val detectorId: String,
        public val data: T,
        public val threats: List<DetectedThreat> = emptyList(),
        public val signals: Map<String, String> = emptyMap(),
    ) : DetectionResult<T>

    public data class NotApplicable(
        override val detectorId: String,
        public val reason: String? = null,
    ) : DetectionResult<Nothing>

    public data class Failed(
        override val detectorId: String,
        public val message: String,
        public val errorType: String? = null,
    ) : DetectionResult<Nothing>
}
```

`data` is the detector-specific payload (`DeviceFingerprint`, `RootCheckResult`, etc.).
`threats` and `signals` are merged into the final `SecurityReport`.

### `ThreatType` and `DetectedThreat`

`ThreatType` is a sealed hierarchy with a stable `id` and a `defaultWeight`. Built-in
entries: `Root`, `Jailbreak`, `Emulator`, `DebuggerAttached`, `SignatureMismatch`,
`HookFramework`, `VpnActive`, `ProxyActive`, `TorExit`. A `Custom(id, defaultWeight)`
entry is the escape hatch for application-specific threats.

A `DetectedThreat` is an instance of one of those types with a concrete
`confidence ∈ 0f..1f`, a `weight ∈ 0..100` (defaulting to `ThreatType.defaultWeight`), and
an opaque `indicators: List<String>` for forensic logging.

### `DeviceGuard` orchestrator

`DeviceGuard.Builder` collects detectors and build-time overrides (logger, scoring
strategy). `analyze()`:

1. Fans every detector out via `async {}` on the caller's coroutine scope.
2. Joins with `awaitAll()`.
3. Aggregates: threats concatenate, signals merge, the first fingerprint wins, errors
   collect, `NotApplicable` is logged at `VERBOSE`.
4. Applies the scoring strategy and returns a single `SecurityReport`.

Detectors run in parallel by construction. A slow detector never blocks another. A
throwing detector never cancels another — the orchestrator wraps each `detect()` call in a
try / catch that also preserves `CancellationException`.

## Risk scoring

The default `WeightedSumScoring`:

```
score = sum over threats of round(weight × confidence)
score = score.coerceIn(0, 100)
```

Weights are tuned so a single high-severity threat (Root, Jailbreak, SignatureMismatch)
already puts the device in the `HIGH` band (≥ 60), and two+ co-occurring threats push to
`CRITICAL` (≥ 80). Low-severity threats (VpnActive at 10, ProxyActive at 15) combine
additively without overshadowing otherwise healthy devices.

To override scoring:

```kotlin
val scoring = RiskScoring { threats ->
    // your formula
}

DeviceGuard.Builder(context)
    .addDetector(myDetector)
    .scoring(scoring)
    .build()
```

`RiskLevel.fromScore(score)` maps to buckets:

| Range   | Level      |
|---------|------------|
| 0–19    | `SAFE`     |
| 20–39   | `LOW`      |
| 40–59   | `MEDIUM`   |
| 60–79   | `HIGH`     |
| 80–100  | `CRITICAL` |

## `SecurityReport`

```kotlin
@Serializable
public data class SecurityReport(
    public val riskScore: Int,
    public val threats: List<DetectedThreat>,
    public val fingerprint: DeviceFingerprint? = null,
    public val signals: Map<String, String> = emptyMap(),
    public val errors: List<DetectorError> = emptyList(),
    public val analyzedAtEpochMillis: Long,
    public val schemaVersion: Int = DeviceGuardVersion.REPORT_SCHEMA,
)
```

`riskLevel` is derived from `riskScore` and never stored / serialized. `toJson()` emits
canonical JSON (defaults encoded, unknown keys ignored on decode, explicit nulls dropped).
`fromJson()` is the inverse. `schemaVersion` bumps when the on-wire layout changes in a
way old parsers cannot handle — see [Migration Guide](migration.md).

## Module layout

```
deviceguard/
├── deviceguard-core/          # this page
├── deviceguard-fingerprint/   # Detector<DeviceFingerprint>
├── deviceguard-rootcheck/     # Detector<RootCheckResult>
├── deviceguard-emulator/      # Detector<EmulatorCheckResult>
├── deviceguard-integrity/     # Detector<IntegrityCheckResult>
├── deviceguard-network/       # Detector<NetworkCheckResult>
└── deviceguard-bom/           # version alignment
```

Every non-core module follows the same shape: a data class result type, a `Detector`
implementation, an `internal expect suspend fun run<X>Check(...)` platform entry point, and
an `enableX()` DSL extension on `DeviceGuard.Builder`.

That's the whole architecture — the modules differ only in what signals they read.
