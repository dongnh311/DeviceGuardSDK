# Android

## Minimum SDK

`minSdk = 21` (Android 5.0). Every detector compiles and runs from API 21 forward.

## Manifest permissions

DeviceGuard does **not** require any runtime permissions. Each detector relies only on APIs
that are free for any app to call:

| Detector | APIs used | Permission |
|----------|-----------|------------|
| Fingerprint | `Build.*`, `Resources.getConfiguration`, `Settings.Secure.ANDROID_ID` | none |
| Root check | `PackageManager.getInstalledPackages()`, file existence on `/system`, `/sbin`, Magisk paths | none |
| Emulator | `Build.HARDWARE`, `Build.PRODUCT`, `Debug.isDebuggerConnected()` | none |
| Integrity | `PackageManager.getPackageInfo(..., GET_SIGNING_CERTIFICATES)`, `getInstallerPackageName()` | none |
| Network | `ConnectivityManager.getNetworkCapabilities`, `System.getProperty("*.proxyHost")`, `NetworkInterface.getNetworkInterfaces()` | `ACCESS_NETWORK_STATE` |

Add the one network-related permission if you enable the network detector:

```xml title="AndroidManifest.xml"
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

The `deviceguard-network` module declares it in its own manifest for you; the merge
process wires it into your APK automatically. The entry above is only required if you
opt out of manifest merging.

## Context handle

Pass your Android `Context` when constructing the DeviceGuard context:

```kotlin
val guard = DeviceGuard.Builder(DeviceGuardContext(androidContext)).build()
```

Use the **application** context when possible. The SDK never leaks the context beyond the
`analyze()` call, but handing it the Application instance avoids any accidental
Activity-scoped dependency if you hold the `DeviceGuard` as a singleton.

## `Package visibility` (Android 11+)

Root-check's package scan and integrity's installer check both call
`PackageManager.getInstalledPackages()`. On API 30+ you must either:

- Declare the `QUERY_ALL_PACKAGES` permission (requires Play Store justification — only if
  your category allows it), **or**
- Use `<queries>` elements in your manifest listing the packages you care about, **or**
- Rely solely on the existence-based probes (file paths, build tags) — the detector
  degrades gracefully when the package scan returns a filtered list.

The root-check module does **not** declare `QUERY_ALL_PACKAGES`. If you see it under-
reporting root-tooling packages on API 30+, add the permission in your app manifest.

## Play Integrity / SafetyNet

DeviceGuard does not wrap Play Integrity or SafetyNet. For high-assurance flows, combine
server-side Play Integrity attestation with DeviceGuard's client-side signals rather than
swap one for the other. DeviceGuard's value is immediate, offline-capable triage; Play
Integrity's value is Google-attested device state. They are complementary, not redundant.

## Signing certificate format

The `enableIntegrityCheck(expectedSignature = …)` value is a SHA-256 fingerprint of your
release signing certificate as a lowercase hex string. Accepts either `"ABCDEF…"` or
`keytool`'s colon-separated `"AB:CD:EF:…"` format; whitespace and case are normalized.

Get it from Play Console (Setup → App integrity) or locally:

```bash
keytool -list -v -keystore release.keystore -alias release -storepass "$PASS"
```

Copy the `SHA-256` row from the output. Store it in a `BuildConfig` field or the secrets
store — treat it as *non-secret but verify-at-build*.

## Testing on a rooted emulator

The Android Studio emulator with Google APIs is rooted by default (via `adb root`). Expect
the root-check to trip on every run; this is correct behavior. For CI and golden-path
tests, use a Google Play system image, which ships non-rooted.
