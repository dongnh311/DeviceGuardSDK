package io.github.dongnh311.deviceguard.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Regression guard for the orchestrator against the SDK's `analyze()` p95 budget
 * (200ms on a mid-tier device, see README's performance section).
 *
 * Runs on [Dispatchers.Default] via [runBlocking] so `async` fan-out across detectors actually
 * parallelizes — the kotlinx-coroutines-test `runTest` scheduler would serialize them on one
 * thread and hide any regression that drops parallelism.
 *
 * Stub detectors `yield()` once so the dispatcher has a real handoff point per detector and
 * the benchmark exercises scheduling, not just sequential call overhead.
 */
class AnalyzePerfBenchmarkJvmTest {
    @Test
    fun analyzeP95StaysUnderBudget() {
        val detectors =
            List(6) { i ->
                object : Detector<Unit> {
                    override val id: String = "detector-$i"

                    override suspend fun detect(context: DeviceGuardContext): DetectionResult<Unit> {
                        yield()
                        return DetectionResult.Success(
                            detectorId = id,
                            data = Unit,
                            threats = listOf(DetectedThreat.of(ThreatType.VpnActive)),
                            signals = mapOf("detector.$i" to "ok"),
                        )
                    }
                }
            }
        val guard =
            DeviceGuard
                .Builder(DeviceGuardContext())
                .apply { detectors.forEach(::addDetector) }
                .clock { 0L }
                .build()

        val mark = TimeSource.Monotonic
        val samples =
            runBlocking(Dispatchers.Default) {
                repeat(WARMUP) { guard.analyze() }
                List(SAMPLES) {
                    val start = mark.markNow()
                    withContext(Dispatchers.Default) { guard.analyze() }
                    start.elapsedNow()
                }
            }

        val sorted = samples.sorted()
        val p95 = sorted[(SAMPLES * 95) / 100]

        assertTrue(
            p95 < BUDGET,
            "analyze() p95=$p95 exceeded budget=$BUDGET over $SAMPLES runs " +
                "(min=${sorted.first()}, max=${sorted.last()}, ${detectors.size} detectors)",
        )
    }

    private companion object {
        private const val WARMUP = 10
        private const val SAMPLES = 100
        private val BUDGET = 200.milliseconds
    }
}
