package io.github.dongnh311.deviceguard.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock

private const val TAG = "DeviceGuard"

/**
 * Orchestrator for every detector attached via [Builder].
 *
 * Call [analyze] to run each detector concurrently, merge the outputs, apply the configured
 * [RiskScoring] strategy, and return a single [SecurityReport].
 *
 * ```kotlin
 * val guard = DeviceGuard.Builder(context)
 *     .addDetector(myDetector)
 *     .logger(DeviceGuardLogger.Println)
 *     .build()
 *
 * val report = guard.analyze()
 * ```
 */
public class DeviceGuard internal constructor(
    private val context: DeviceGuardContext,
    private val detectors: List<Detector<*>>,
    private val logger: DeviceGuardLogger,
    private val scoring: RiskScoring,
    private val clock: () -> Long,
) {
    /**
     * Run every attached detector and assemble a [SecurityReport].
     *
     * Detectors run concurrently via [coroutineScope]; each is isolated by a try/catch so one
     * detector's failure doesn't cancel the others. [DetectionResult.NotApplicable] results
     * are logged and otherwise ignored — they don't count as errors.
     */
    public suspend fun analyze(): SecurityReport =
        coroutineScope {
            logger.logLazy(DeviceGuardLogger.LogLevel.DEBUG) {
                "Starting analysis with ${detectors.size} detector(s)"
            }

            val results = detectors.map { async { runDetector(it) } }.awaitAll()

            val threats = mutableListOf<DetectedThreat>()
            val signals = linkedMapOf<String, String>()
            val errors = mutableListOf<DetectorError>()
            var fingerprint: DeviceFingerprint? = null

            for (result in results) {
                when (result) {
                    is DetectionResult.Success<*> -> {
                        threats += result.threats
                        signals.putAll(result.signals)
                        val data = result.data
                        if (fingerprint == null && data is DeviceFingerprint) {
                            fingerprint = data
                        }
                    }
                    is DetectionResult.NotApplicable ->
                        logger.logLazy(DeviceGuardLogger.LogLevel.VERBOSE) {
                            "Detector '${result.detectorId}' not applicable: ${result.reason ?: "platform mismatch"}"
                        }
                    is DetectionResult.Failed -> {
                        errors +=
                            DetectorError(
                                detectorId = result.detectorId,
                                message = result.message,
                                errorType = result.errorType,
                            )
                        logger.logLazy(DeviceGuardLogger.LogLevel.WARN) {
                            "Detector '${result.detectorId}' failed: ${result.message}"
                        }
                    }
                }
            }

            val score = scoring.score(threats)
            SecurityReport(
                riskScore = score,
                threats = threats,
                fingerprint = fingerprint,
                signals = signals,
                errors = errors,
                analyzedAtEpochMillis = clock(),
            )
        }

    private suspend fun runDetector(detector: Detector<*>): DetectionResult<*> =
        try {
            detector.detect(context)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            logger.logLazy(DeviceGuardLogger.LogLevel.ERROR, error = error) {
                "Detector '${detector.id}' threw"
            }
            DetectionResult.Failed(
                detectorId = detector.id,
                message = error.message ?: error::class.simpleName.orEmpty(),
                errorType = error::class.simpleName,
            )
        }

    /**
     * Builder for [DeviceGuard].
     *
     * Add detectors explicitly via [addDetector]. Individual detector modules
     * (`deviceguard-fingerprint`, `deviceguard-rootcheck`, …) publish extension functions such
     * as `enableFingerprint()` that wrap the underlying [addDetector] call.
     */
    public class Builder(
        private val context: DeviceGuardContext,
    ) {
        private val detectors: MutableList<Detector<*>> = mutableListOf()
        private var logger: DeviceGuardLogger = DeviceGuardLogger.NoOp
        private var scoring: RiskScoring = WeightedSumScoring
        private var clock: () -> Long = { Clock.System.now().toEpochMilliseconds() }

        /** Attach [detector] to the orchestrator. */
        public fun addDetector(detector: Detector<*>): Builder =
            apply {
                detectors += detector
            }

        /** Override the default [DeviceGuardLogger.NoOp] logger. */
        public fun logger(logger: DeviceGuardLogger): Builder =
            apply {
                this.logger = logger
            }

        /** Override the default [WeightedSumScoring] strategy. */
        public fun scoring(scoring: RiskScoring): Builder =
            apply {
                this.scoring = scoring
            }

        internal fun clock(clock: () -> Long): Builder =
            apply {
                this.clock = clock
            }

        /** Snapshot the current configuration into an immutable [DeviceGuard]. */
        public fun build(): DeviceGuard =
            DeviceGuard(
                context = context,
                detectors = detectors.toList(),
                logger = logger,
                scoring = scoring,
                clock = clock,
            )
    }
}

private inline fun DeviceGuardLogger.logLazy(
    level: DeviceGuardLogger.LogLevel,
    error: Throwable? = null,
    message: () -> String,
) {
    if (isEnabled(level)) log(level, TAG, message(), error)
}
