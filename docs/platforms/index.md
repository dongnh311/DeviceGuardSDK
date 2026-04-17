# Platform Guides

DeviceGuard publishes for four host platforms. Each detector declares, per-platform,
whether it is **applicable**, **partially available**, or **deferred**. The matrix:

| Platform          | Core | Fingerprint | Root / Jailbreak | Emulator | Integrity | Network |
|-------------------|------|-------------|------------------|----------|-----------|---------|
| Android (API 21+) | ✅    | ✅           | ✅                | ✅        | ✅         | ✅       |
| iOS (13+)         | ✅    | ✅           | ✅ (jailbreak)    | ✅ (sim)  | ✅ (Frida) | — deferred (cinterop) |
| JVM / Desktop     | ✅    | ✅           | — not applicable | ✅ (JDWP) | — deferred | ✅      |
| JS / Web          | ✅    | ✅           | — not applicable | ✅ (best-effort) | — deferred | — deferred |

Detectors that return `NotApplicable` on a given platform are a design choice, not a bug:
root/jailbreak has no equivalent on desktop JVM, and in-browser VPN detection needs
server-side correlation to be reliable.

See each platform page for manifest / Info.plist / runtime caveats:

- [Android](android.md)
- [iOS](ios.md)
- [JVM / Desktop](jvm.md)
- [JS / Web](js.md)
