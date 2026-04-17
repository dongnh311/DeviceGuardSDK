# JS / Web

!!! warning "Best-effort platform"
    The browser target ships a minimum viable set of detectors. In-browser security
    signals are fundamentally weaker than on mobile / desktop — the DOM sandbox does not
    let JavaScript inspect the filesystem, kernel, or process table. What the browser
    *can* see (navigator, screen, timing) is also what every fingerprinting library
    sees, so collision-resistance on visitor identity is worse. Treat JS signals as
    hints that bias a server-side decision, never as the decision itself.

## Target

`kotlin { js(IR) { browser(); nodejs() } }` — the module compiles for both browser
bundlers (webpack / vite / esbuild) and Node. Build output lives in `build/js/`.

## Applicability matrix

| Detector | JS behaviour | Reason |
|----------|--------------|--------|
| Fingerprint | ✅ (best-effort) | `navigator.userAgent`, `platform`, `language`, `hardwareConcurrency`, `screen.*`, `Intl.DateTimeFormat().resolvedOptions().timeZone`. |
| Root check | `NotApplicable` | No kernel-level concept in the browser sandbox. |
| Emulator | ✅ (best-effort) | `navigator.webdriver === true` (weight 0.9) for Selenium / Playwright / headless. Reported as an `EmulatorIndicator` so it flags `ThreatType.Emulator` — not a literal VM, but the same "not a real user" signal. |
| Debugger | ✅ (best-effort) | `window.outerHeight === 0 || window.outerWidth === 0` (weight 0.5) as a side effect of docked DevTools. Noisy; prefer the webdriver signal. |
| Integrity | `NotApplicable` | Subresource Integrity and script-tampering checks are the consumer's responsibility — the browser will not let the running bundle introspect its own integrity. |
| Network | `NotApplicable` | VPN / proxy detection from the browser is unreliable; correlate the request's source IP server-side. |

## On Node

The fingerprint module reaches for `kotlinx.browser.window` via `runCatching`. On Node the
access throws and every fingerprint signal is silently skipped — you get an empty map
back, which is the "I could not determine this" signal. The emulator module behaves the
same way: on Node, `window` is unavailable, so `navigator.webdriver` never fires and the
outcome is empty.

If you consume DeviceGuard from Node on purpose, consider the JVM target instead — it has
much more to say about the host process.

## Canvas / audio / font fingerprinting

Out of scope. Canvas fingerprinting is the most discriminative passive signal on the web
but also the most privacy-sensitive and the most unstable (browser fingerprinting
countermeasures specifically target it). If you need it, register a custom
`Detector<DeviceFingerprint>` that reads `CanvasRenderingContext2D`, hashes the output,
and merges it into your report. DeviceGuard core will not prescribe the approach.

## Real-world integration pattern

```kotlin
val report = DeviceGuard.Builder(DeviceGuardContext())
    .enableFingerprint()
    .enableEmulatorCheck()
    .build()
    .analyze()

fetch("/api/v1/session", {
    method: "POST",
    body: JSON.stringify(report.toJson()),
})
```

Post the `SecurityReport.toJson()` to your backend, combine it with server-side signals
(IP reputation, request-rate anomalies, TLS fingerprint, Play Integrity / Apple
DeviceCheck for mobile), and make the final decision there. The browser's contribution to
the decision should never exceed 20% of the weight in any rational risk model — enough to
flag a webdriver, not enough to deny a legitimate user with a slightly unusual
configuration.
