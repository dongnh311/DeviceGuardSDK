package io.github.dongnh311.deviceguard.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock

/**
 * Orchestrator for every detector attached via [Builder].
 *
 * Call [analyze] to run each detector concurrently, merge the outputs, apply the configured
 * [RiskScoring] strategy, and return a single [SecurityReport].
 *
 * Construct instances through [Builder]:
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
    /** Number of detectors attached to this [DeviceGuard]. Useful for diagnostics. */
    public val detectorCount: Int get() = detectors.size

    /**
     * Run every attached detector and assemble a [SecurityReport].
     *
     * Detectors run concurrently via [coroutineScope]; each is isolated by a try/catch so one
     * detector's failure doesn't cancel the others. [DetectionResult.NotApplicable] results
     * are logged and otherwise ignored — they don't count as errors.
     */
    public suspend fun analyze(): SecurityReport = coroutineScope { analyzeIn(this) }

    private suspend fun analyzeIn(scope: CoroutineScope): SecurityReport {
        logger.log(
            level = DeviceGuardLogger.LogLevel.DEBUG,
            tag = TAG,
            message = "Starting analysis with ${detectors.size} detector(s)",
        )

        val deferred =
            detectors.map { detector ->
                scope.async {
                    runDetector(detector)
                }
            }
        val results = deferred.awaitAll()

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
                    logger.log(
                        level = DeviceGuardLogger.LogLevel.VERBOSE,
                        tag = TAG,
                        message = "Detector '${result.detectorId}' not applicable: ${result.reason ?: "platform mismatch"}",
                    )
                is DetectionResult.Failed -> {
                    errors +=
                        DetectorError(
                            detectorId = result.detectorId,
                            message = result.message,
                            errorType = result.errorType,
                        )
                    logger.log(
                        level = DeviceGuardLogger.LogLevel.WARN,
                        tag = TAG,
                        message = "Detector '${result.detectorId}' failed: ${result.message}",
                    )
                }
            }
        }

        val score = scoring.score(threats)
        return SecurityReport(
            riskScore = score,
            riskLevel = RiskLevel.fromScore(score),
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
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            logger.log(
                level = DeviceGuardLogger.LogLevel.ERROR,
                tag = TAG,
                message = "Detector '${detector.id}' threw",
                error = error,
            )
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

        /** Override the clock used for [SecurityReport.analyzedAtEpochMillis]. Test-only. */
        @ExperimentalDeviceGuardApi
        public fun clock(clock: () -> Long): Builder =
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

    internal companion object {
        internal const val TAG: String = "DeviceGuard"
    }
}
