# DeviceGuard Sample — iOS

A minimal SwiftUI host for the Compose Multiplatform `App()` defined in `sample/shared`.

## Generate the Xcode project

Install [`xcodegen`](https://github.com/yonaskolb/XcodeGen) once:

```bash
brew install xcodegen
```

Then:

```bash
cd sample/ios
xcodegen
open DeviceGuardSample.xcodeproj
```

The `project.yml` generator declares a pre-build script that runs
`./gradlew :sample:shared:embedAndSignAppleFrameworkForXcode` before every Xcode
build, so the `SampleShared.framework` is always up-to-date relative to the Kotlin
source in `sample/shared/`.

## Command-line build

```bash
xcodebuild \
  -project DeviceGuardSample.xcodeproj \
  -scheme DeviceGuardSample \
  -sdk iphonesimulator \
  -destination "platform=iOS Simulator,name=iPhone 15" \
  build
```

## Running on a device

Select your provisioning profile and development team in Xcode (Signing & Capabilities) —
the generated `project.yml` leaves team blank on purpose so the repo stays
personal-certificate-free. Deploy to any iOS 13+ device from Xcode.
