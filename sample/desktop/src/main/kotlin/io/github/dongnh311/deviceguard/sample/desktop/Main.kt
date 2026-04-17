package io.github.dongnh311.deviceguard.sample.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import io.github.dongnh311.deviceguard.sample.shared.App

fun main() {
    singleWindowApplication(
        title = "DeviceGuard Sample",
        state = WindowState(size = DpSize(WINDOW_WIDTH.dp, WINDOW_HEIGHT.dp)),
    ) {
        App()
    }
}

private const val WINDOW_WIDTH = 720
private const val WINDOW_HEIGHT = 900
