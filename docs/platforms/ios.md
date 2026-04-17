# iOS

## Minimum target

`iOS 13.0` across every DeviceGuard module. Builds for `iosArm64` (device),
`iosSimulatorArm64` (Apple Silicon simulator), and `iosX64` (legacy Intel simulator).

## Info.plist entries

DeviceGuard does **not** require any privacy usage-description strings. The jailbreak
and Frida-artefact probes inspect files and process state that Apple does not gate behind
App Tracking Transparency or other consent APIs.

If you add your own signals via a custom `Detector` that reaches for something like the
IDFA, those probes need their own Info.plist entries — DeviceGuard itself stays silent
about PII-adjacent identifiers.

## Jailbreak probes

`deviceguard-rootcheck` on iOS does **not** use `UIApplication.canOpenURL` — so no
`LSApplicationQueriesSchemes` entries are needed in your `Info.plist`. The module relies
on two source-of-truth signals:

- **Filesystem presence** of known jailbreak artefacts (weight 0.9 each): Cydia / Sileo /
  Zebra bundles, `MobileSubstrate.dylib`, `/bin/bash`, `/usr/sbin/sshd`, `/etc/apt`,
  `/private/var/lib/apt`, `/private/var/lib/cydia`, SBSettings themes, and SSH-tooling
  binaries under `/usr/bin` / `/usr/libexec`. A sandboxed app on a stock device cannot see
  these paths.
- **Sandbox-escape write probe** (weight 1.0): attempts to create a uniquely-named file
  under `/private/` and immediately removes it. Success proves the app sandbox has been
  broken. Cleanup runs in a `finally` block.

## Context handle

iOS does not need a platform handle:

```kotlin
val guard = DeviceGuard.Builder(DeviceGuardContext()).build()
```

## Integrity check scope

`deviceguard-integrity` on iOS ships two stream types:

- **Hook artefacts (weight 1.0 each)** — filesystem probes for Frida and Cycript across
  both classic and rootless (Dopamine / palera1n) jailbreak layouts:
    - `/Library/Frameworks/FridaGadget.framework`
    - `/usr/lib/frida/frida-agent.dylib`, `/usr/lib/frida/frida-gadget.dylib`,
      `/usr/sbin/frida-server`, `/usr/bin/cycript`
    - The same set under `/var/jb/` for rootless layouts (iOS 15+).
- **Signature signals (weight 0.6)** — `NSBundle.mainBundle.bundleIdentifier == null`
  trips a `no_bundle_identifier` indicator. A correctly-signed build always has an
  identifier; absence suggests a wrapper or unsigned build.

Deferred: full code-signature verification via `SecStaticCodeCheckValidity`, in-memory
Frida agent scanning via `_dyld_image_count`, and re-signing detection via
`embedded.mobileprovision` inspection. Those require more cinterop surface and benefit
from validation against real re-signed builds. Until they ship, pair DeviceGuard's iOS
integrity detector with server-side receipt validation for the App Store / TestFlight
build identity. `IntegrityCheckConfig.expectedSignatureSha256` is Android-only and
ignored on iOS.

## Simulator detection

The emulator module detects the iOS Simulator via `NSProcessInfo.environment`:

- `SIMULATOR_DEVICE_NAME`
- `SIMULATOR_MODEL_IDENTIFIER`
- `SIMULATOR_HOST_HOME`

These env vars are set by the simulator runtime itself and cannot be hidden by an app. The
debugger probe (`sysctl(KERN_PROC_PID)` with the `P_TRACED` flag) is documented in the
source but deferred pending cinterop struct support.

## What the iOS network detector does

At 0.1.0 the iOS network module returns `NotApplicable`. VPN / proxy detection via
`CFNetworkCopySystemProxySettings` works from the app sandbox, but the `__SCOPED__` dict
that carries interface-type info needs careful cinterop handling to avoid crashes on
devices with unusual profile configurations. The target is to light it up in a later
minor release.

In the meantime, if VPN / proxy correlation matters for your flow, do it server-side: the
request source IP is far more reliable than any client-side probe.

## Device fingerprint

Signals collected:

- `os.system_name`, `os.system_version`
- `device.model`, `device.localized_model`
- `device.vendor_id` — `UIDevice.current.identifierForVendor?.uuidString` when available
- `screen.scale`, `screen.native_scale`, `screen.width_pt`, `screen.height_pt`
- `locale`, `timezone`

`identifierForVendor` is the stable per-install-per-vendor identifier Apple provides.
DeviceGuard hashes the full signal map into the fingerprint `id`; the raw
`identifierForVendor` is kept in `signals` for diagnostics but deliberately not surfaced
as a standalone public property. No IDFA, no `UIDevice.name`, no MAC.
