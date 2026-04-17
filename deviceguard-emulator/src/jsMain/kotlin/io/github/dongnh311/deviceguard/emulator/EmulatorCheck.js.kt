package io.github.dongnh311.deviceguard.emulator

import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import kotlinx.browser.window

private const val WEIGHT_WEBDRIVER = 0.9f
private const val WEIGHT_DEVTOOLS_OUTER_HEIGHT = 0.5f

/**
 * JS/Browser emulator + debugger detection.
 *
 * - **Emulator / headless automation:** `navigator.webdriver === true` is set by WebDriver
 *   (Selenium, Playwright) and most headless browsers. Weight 0.9 — not a traditional
 *   emulator signal, but a strong "not a real user" hint worth flagging alongside the
 *   emulator threat so consumers can gate on a single boolean.
 * - **Debugger:** `window.outerHeight == 0` is a common side effect of devtools docking.
 *   Weight 0.5 — noisy; combined with other signals in future work it becomes useful.
 *
 * On Node there is no `window`; `kotlinx.browser.window` throws on access. The outer
 * `runCatching` swallows that so Node callers see an empty outcome.
 */
internal actual suspend fun runEmulatorCheck(context: DeviceGuardContext): EmulatorCheckOutcome {
    val emulator = mutableListOf<EmulatorIndicator>()
    val debugger = mutableListOf<EmulatorIndicator>()

    runCatching {
        val nav = window.navigator.asDynamic()
        if (nav.webdriver == true) emulator += EmulatorIndicator("navigator.webdriver", WEIGHT_WEBDRIVER)
        if (window.outerHeight == 0 || window.outerWidth == 0) {
            debugger += EmulatorIndicator("window.outerHeight==0", WEIGHT_DEVTOOLS_OUTER_HEIGHT)
        }
    }

    return EmulatorCheckOutcome(applicable = true, emulatorIndicators = emulator, debuggerIndicators = debugger)
}
