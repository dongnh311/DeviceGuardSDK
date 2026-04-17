# Security Policy

DeviceGuard SDK is a client-side device-security library. We take vulnerabilities in the
detection logic, false-negative gaps, and any issue that could compromise consumer
applications seriously — please follow the process below to report them privately.

## Reporting a vulnerability

**Do not file a public GitHub issue for security problems.**

Use one of the private channels instead:

- **GitHub Security Advisories (preferred)** — on the repo,
  *Security* tab → *Report a vulnerability*. This opens a private advisory visible only to
  the reporter and the maintainers, with a dedicated thread for coordinated disclosure.
- **Email** — <hoaidongit5@gmail.com>. Include `[DeviceGuard SDK Security]` in the subject.
  If you need to send an encrypted payload, ask for a PGP key in your first message.

When reporting, please include:

1. The affected module (`deviceguard-core`, `deviceguard-rootcheck`, …) and version / git
   SHA you tested against.
2. The platform (Android + API level, iOS version, JVM version, or browser).
3. A concrete reproduction — either a minimal code snippet, or a description of the device
   state that triggers the issue.
4. The impact you believe the issue has: bypass of a detection, information leak, crash /
   DoS, signature-verification weakness, etc.

## What we commit to

- **Acknowledgement** within 5 business days of receipt.
- **Triage and severity classification** within 10 business days, shared back with the
  reporter.
- **Fix timeline** discussed openly in the advisory thread. For high-severity issues we aim
  for a coordinated release within 30 days.
- **Credit** to the reporter in the release notes and `CHANGELOG.md`, unless the reporter
  prefers to remain anonymous.

## What's in scope

- Bypasses of any DeviceGuard detection (root / jailbreak, emulator, app integrity, VPN /
  proxy) that defeat the SDK without reasonable effort.
- Information disclosure — any fingerprint signal that leaks PII we document as
  non-collected.
- Signature / certificate comparison flaws that allow a tampered app to pass integrity
  checks.
- Logic issues in the risk-scoring or report-assembly code that could mis-classify a
  genuinely compromised device as safe.

## What's out of scope

- Theoretical bypasses that require root + Frida + hook framework + deep reverse
  engineering. A determined attacker with kernel-level control (Magisk-hide, Frida-gadget
  injection, custom LSM patches) can defeat any client-side detection. This is a known
  limitation of every client-side security SDK and not in itself a vulnerability — we
  treat it as a floor to be raised by layering server-side attestation on top of the
  client signals DeviceGuard surfaces.
- Issues in server-side attestation (Play Integrity API, SafetyNet, Apple DeviceCheck) —
  DeviceGuard does not wrap these; it is your responsibility to correlate server-side.
- False positives on niche device configurations. Open a regular GitHub issue with the
  device / OS details and we'll tune weights.
- Issues in dependencies (kotlinx-coroutines, serialization, kotlincrypto) — report
  upstream, optionally file a tracking issue here once the upstream has a fix.

## After a fix ships

A public advisory is published via the GitHub Security Advisories feature with a CVE if
warranted, and the `CHANGELOG.md` entry for the release includes a `Security` subsection
linking to the advisory.
