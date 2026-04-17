@file:OptIn(ExperimentalForeignApi::class)

package io.github.dongnh311.deviceguard.fingerprint

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.Foundation.systemTimeZone
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen

/**
 * iOS signals.
 *
 * `UIDevice.identifierForVendor` ties the fingerprint to the app's vendor bundle, resetting
 * when the user uninstalls every app from the same vendor — the intended privacy-friendly
 * reset knob. No IDFA, no `UIDevice.name` (carries user identity), no MAC (restricted).
 */
internal actual fun collectFingerprintSignals(context: DeviceGuardContext): Map<String, String> {
    val signals = HashMap<String, String>()

    runCatching {
        val device = UIDevice.currentDevice
        signals["os.system_name"] = device.systemName
        signals["os.system_version"] = device.systemVersion
        signals["device.model"] = device.model
        signals["device.localized_model"] = device.localizedModel
        device.identifierForVendor?.UUIDString?.let { signals["device.vendor_id"] = it }
    }

    runCatching {
        val screen = UIScreen.mainScreen
        signals["screen.scale"] = screen.scale.toString()
        signals["screen.native_scale"] = screen.nativeScale.toString()
        screen.bounds.useContents {
            signals["screen.width_pt"] = size.width.toString()
            signals["screen.height_pt"] = size.height.toString()
        }
    }

    runCatching {
        signals["locale"] = NSLocale.currentLocale.localeIdentifier
        signals["timezone"] = NSTimeZone.systemTimeZone.name
    }

    return signals
}
