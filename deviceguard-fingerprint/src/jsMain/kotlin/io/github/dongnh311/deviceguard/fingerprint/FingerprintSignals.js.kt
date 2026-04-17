package io.github.dongnh311.deviceguard.fingerprint

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import kotlinx.browser.window

/**
 * JS/Browser signals.
 *
 * Reads `navigator`, `window.screen`, and timezone via `Intl.DateTimeFormat`. On Node (no
 * `window`), every signal is skipped and the map is returned empty. Canvas fingerprinting is
 * out of scope; register a custom detector alongside [FingerprintDetector] if you need it.
 */
internal actual fun collectFingerprintSignals(context: DeviceGuardContext): Map<String, String> {
    val signals = HashMap<String, String>()

    runCatching {
        val nav = window.navigator
        signals["browser.user_agent"] = nav.userAgent
        signals["browser.platform"] = nav.platform
        signals["browser.language"] = nav.language
        signals["browser.hardware_concurrency"] = nav.hardwareConcurrency.toString()
    }

    runCatching {
        val screen = window.screen
        signals["screen.width_px"] = screen.width.toString()
        signals["screen.height_px"] = screen.height.toString()
        signals["screen.color_depth"] = screen.colorDepth.toString()
        signals["screen.pixel_depth"] = screen.pixelDepth.toString()
    }

    runCatching {
        signals["timezone.offset_min"] = resolveTimezoneOffsetMinutes().toString()
        signals["timezone"] = resolveTimezoneName()
    }

    return signals
}

private fun resolveTimezoneOffsetMinutes(): Int = js("new Date().getTimezoneOffset()").unsafeCast<Int>()

private fun resolveTimezoneName(): String = js("Intl.DateTimeFormat().resolvedOptions().timeZone").toString()
