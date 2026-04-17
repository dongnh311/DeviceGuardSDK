# Security Considerations

DeviceGuard is a client-side signal generator. How you read those signals — the thresholds
you gate on, the way you combine them with server-side attestation, what you do when a
detector flags a legitimate user — matters more than the detection itself. This page
covers the parts of that decision that are opinionated by the SDK.

## The philosophy

1. **Confidence over booleans.** Every detector returns a confidence in `0f..1f`, not a
   boolean. A single weak indicator does not trip root detection in the default `lax`
   profile; strict mode lowers the threshold because applications in payment or account-
   recovery flows want any sniff of tampering surfaced.
2. **Signal composition.** Risk scoring is additive: `VPN active + proxy active +
   emulator` combines to MEDIUM (50) rather than any one of them tripping CRITICAL.
   Detector weights are tuned so a single high-severity threat (root, jailbreak,
   signature-mismatch) saturates to HIGH on its own.
3. **Never-throw contract.** A detector that fails returns `DetectionResult.Failed` with a
   forensic message. The orchestrator logs and continues. A cascade of detector failures
   does not affect `riskScore` — a rooted device that also has a flaky probe still scores
   correctly on the detectors that ran.
4. **No PII in fingerprints.** The fingerprint module hashes non-identifying platform
   signals and deliberately excludes IMEI, advertising id, account identifiers, email.
   The id is stable across launches but invalidates on factory reset, MAC rotation, or
   browser profile change — by design.

## False positives

Any signal on this page is worth weighting against the legitimate population that exhibits
it:

| Signal | Legitimate population |
|--------|----------------------|
| `VpnActive` | Anyone on a corporate VPN, traveller on a travel VPN, or privacy-conscious user with a paid VPN service. **Do not deny traffic on VPN alone.** |
| `ProxyActive` | Corporate HTTP proxies, internet cafes with captive portals, debug traffic inspection on developer machines (Charles, mitmproxy, Proxyman). |
| `Emulator` on Android | QA testers, developers, users of Android-on-Chromebook / Windows-Subsystem-for-Android, users of cloud-gaming phones. |
| `navigator.webdriver` on JS | Selenium / Playwright, but also accessibility tools in some enterprise environments. |
| `net.mac_hash` collisions on JVM | Uncommon, but possible on virtualised desktops where MAC is assigned by the hypervisor. |

The `signals` map on the `SecurityReport` carries raw forensic values for every detector
that ran. Log them alongside the decision so you can retroactively understand why a user
saw a challenge / denial.

## Bypass resistance

DeviceGuard is *not* bypass-proof and is not designed to be. Client-side detection has a
fundamental ceiling: a determined attacker with kernel-level control — Magisk-hide on
Android, patched Frida-gadget on iOS, custom LSM modules, or a rooted device running
Zygisk-Detach — can defeat *any* client-side SDK, including SafetyNet, Play Integrity
hardware attestation (when the TEE is compromised), and every commercial alternative.

What DeviceGuard does well:

- **Raises the cost floor.** A user with a stock-rooted device and Magisk-hide disabled
  (the common case) trips every Android signal. Attackers who only have a Magisk-rooted
  environment without hiding are flagged.
- **Catches casual tampering.** Re-signed APKs installed from a third-party store (not
  `com.android.vending`) trip the integrity check. Running inside Selenium / headless
  Chrome trips the JS emulator check.
- **Surfaces environment anomalies.** A VPN + emulator + proxy combination is
  suspicious enough to challenge even if each alone is not; the scoring strategy
  composes this for you.

What DeviceGuard does *not* do:

- Defeat Frida when the attacker is careful to launder artefact names and use ephemeral
  memory-only scripts.
- Detect root hiders that specifically target the binary-path / package-scan probes —
  Magisk hide works by making these invisible.
- Attest the OS or hardware to a server. For that you need Play Integrity
  (Android) / DeviceCheck (iOS) + server-side verification. Those live outside this SDK;
  see below.

## Recommended layering

For a security-sensitive flow (payment, account recovery, transfer of funds), the
recommended stack is:

1. **Client-side (DeviceGuard)** — get a `SecurityReport` and submit its `toJson()` to
   your backend alongside the business request.
2. **Server-side attestation** — verify a Play Integrity / DeviceCheck nonce that your
   client fetched fresh for this request. The SDK's documentation does not wrap these
   on purpose — their tokens and verification flows are Google's / Apple's territory.
3. **Network signals** — IP reputation, ASN, proxy / Tor exit lookup, anomaly detection
   on request rate and UA-per-IP. DeviceGuard's network detector is client-side; these
   are authoritative only when done server-side.
4. **Decision fusion** — combine the three streams into a single risk score on your
   backend. DeviceGuard's `riskScore` is a *component* of that decision, not the
   decision.

```
┌─────────────────┐      POST session-start      ┌──────────────────┐
│  App + SDK      │  ────────────────────────▶   │  Your backend    │
│                 │                               │                  │
│  DeviceGuard    │  (report.toJson())            │  + Play Integrity│
│  Play Integrity │  + integrity-token           ▶│  + IP reputation │
│  request        │  + server-issued nonce        │  + rate limits   │
└─────────────────┘                               │                  │
                                                  │  → risk decision │
                                                  └──────────────────┘
```

## What to log

- Full `SecurityReport.toJson()` for sessions that trip anything above `SAFE`.
- The detector ids in `errors` — chronic failures often point at a platform / permission
  regression on new OS releases.
- The `fingerprint.id` — useful for retrospectively grouping incidents by device without
  storing PII.

Avoid logging raw `signals` map entries verbatim to user-visible surfaces; they are
stable-by-design and cross-referenceable. Keep them on the security / telemetry path.

## What to show the user

When a risk gate fires, tell the user in product-friendly language what happened and what
recovery path is available. Do **not** enumerate the firing threats — the list is useful
for you, not for the user, and a detailed message is an aid to any attacker probing your
thresholds.

> "This device was flagged as possibly compromised. For your security, transfers are
> temporarily limited. Contact support with reference #A7F3B2 to restore full access."
