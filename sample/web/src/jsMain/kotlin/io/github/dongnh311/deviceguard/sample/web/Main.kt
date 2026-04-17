package io.github.dongnh311.deviceguard.sample.web

import androidx.compose.ui.window.CanvasBasedWindow
import io.github.dongnh311.deviceguard.sample.shared.App
import org.jetbrains.skiko.wasm.onWasmReady

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
fun main() {
    onWasmReady {
        CanvasBasedWindow(title = "DeviceGuard Sample", canvasElementId = "ComposeTarget") {
            App()
        }
    }
}
