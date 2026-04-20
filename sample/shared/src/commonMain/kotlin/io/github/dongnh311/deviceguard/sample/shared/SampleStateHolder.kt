package io.github.dongnh311.deviceguard.sample.shared

import io.github.dongnh311.deviceguard.core.DeviceGuard
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.emulator.enableEmulatorCheck
import io.github.dongnh311.deviceguard.fingerprint.enableFingerprint
import io.github.dongnh311.deviceguard.integrity.enableIntegrityCheck
import io.github.dongnh311.deviceguard.network.enableNetworkCheck
import io.github.dongnh311.deviceguard.remote.enableRemoteCheck
import io.github.dongnh311.deviceguard.rootcheck.enableRootCheck
import io.github.dongnh311.deviceguard.surveillance.enableSurveillanceCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/**
 * Platform-agnostic state holder for the sample. Not a `ViewModel` — `androidx.lifecycle`
 * isn't common. Each wrapper creates one in a scope the platform controls (remember on
 * Desktop, Android's viewModelScope equivalent, rememberCoroutineScope on Web).
 */
internal class SampleStateHolder(
    private val context: DeviceGuardContext,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(SampleUiState())
    val state: StateFlow<SampleUiState> = _state.asStateFlow()

    fun toggleDetector(toggle: DetectorToggle) {
        _state.update { current ->
            val updated =
                if (toggle in current.enabledDetectors) {
                    current.enabledDetectors - toggle
                } else {
                    current.enabledDetectors + toggle
                }
            current.copy(enabledDetectors = updated)
        }
    }

    fun toggleStrictRoot(value: Boolean) {
        _state.update { it.copy(strictRoot = value) }
    }

    fun analyze() {
        val current = _state.value
        if (current.running) return
        _state.update { it.copy(running = true, lastError = null) }

        scope.launch {
            val mark = TimeSource.Monotonic.markNow()
            runCatching { buildGuard(current).analyze() }
                .onSuccess { report ->
                    val elapsed = mark.elapsedNow()
                    _state.update {
                        it.copy(
                            running = false,
                            lastReport = report,
                            lastDurationMs = elapsed.inWholeMilliseconds,
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            running = false,
                            lastError = "${error::class.simpleName}: ${error.message ?: "analyze failed"}",
                        )
                    }
                }
        }
    }

    private fun buildGuard(state: SampleUiState): DeviceGuard {
        val builder = DeviceGuard.Builder(context)
        for (toggle in state.enabledDetectors) {
            when (toggle) {
                DetectorToggle.Fingerprint -> builder.enableFingerprint()
                DetectorToggle.RootCheck -> builder.enableRootCheck(strict = state.strictRoot)
                DetectorToggle.EmulatorCheck -> builder.enableEmulatorCheck()
                DetectorToggle.IntegrityCheck -> builder.enableIntegrityCheck()
                DetectorToggle.NetworkCheck -> builder.enableNetworkCheck()
                DetectorToggle.RemoteCheck -> builder.enableRemoteCheck()
                DetectorToggle.SurveillanceCheck -> builder.enableSurveillanceCheck()
            }
        }
        return builder.build()
    }
}
