package io.github.dongnh311.deviceguard.sample.shared

import androidx.compose.ui.window.ComposeUIViewController
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import platform.UIKit.UIViewController

internal actual val platformName: String = "iOS"

internal actual fun createDeviceGuardContext(): DeviceGuardContext = DeviceGuardContext()

/**
 * iOS entry point. The Xcode host project imports the `SampleShared` framework and calls this
 * factory from a SwiftUI `UIViewControllerRepresentable` to embed the Compose UI.
 */
public fun MainViewController(): UIViewController = ComposeUIViewController { App() }
