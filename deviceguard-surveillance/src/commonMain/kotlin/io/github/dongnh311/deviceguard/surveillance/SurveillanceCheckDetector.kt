package io.github.dongnh311.deviceguard.surveillance

import io.github.dongnh311.deviceguard.core.DetectedThreat
import io.github.dongnh311.deviceguard.core.DetectionResult
import io.github.dongnh311.deviceguard.core.Detector
import io.github.dongnh311.deviceguard.core.DeviceGuard
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import io.github.dongnh311.deviceguard.core.ThreatType

private const val DETECTOR_ID = "surveillancecheck"
private const val THRESHOLD = 0.5f

/**
 * Detector that surfaces apps and tools capable of interfering with other apps locally.
 *
 * **Android (primary platform, 6 signal categories):**
 * - Accessibility service enabled for a non-system package — top indicator for banking
 *   trojans (Anubis, Cerberus, FluBot, BRATA pattern).
 * - Overlay permission held by a non-system package — `SYSTEM_ALERT_WINDOW` is the
 *   standard phishing-overlay vector.
 * - Notification listener enabled for a non-system package — OTP theft vector.
 * - Device admin granted to a non-system package — ransomware / screen lock hijack.
 * - Default IME is a non-system keyboard — potential keylogger.
 * - Usage-stats permission held by a non-system package — spyware foreground tracking.
 *
 * **Desktop JVM (secondary, 2 signal categories):**
 * - Known automation tool running (AutoHotkey, Hammerspoon, Keyboard Maestro,
 *   BetterTouchTool, Karabiner, xdotool).
 * - Known debugger / instrumentation tool running attached to another process
 *   (gdb, lldb, x64dbg, Frida, Cheat Engine, IDA).
 *
 * **iOS / Web:** returns [DetectionResult.NotApplicable]. iOS sandbox prevents app
 * enumeration; Web JS cannot see other browser extensions.
 *
 * Each category's indicators contribute to a clamped-to-1 confidence. When confidence
 * clears `0.5`, a threat of the matching [ThreatType] fires with the category's
 * default weight (see [ThreatType] KDoc).
 */
public class SurveillanceCheckDetector internal constructor(
    private val probe: suspend (DeviceGuardContext) -> SurveillanceOutcome,
) : Detector<SurveillanceCheckResult> {
    public constructor() : this(probe = { runSurveillanceCheck(it) })

    override val id: String = DETECTOR_ID

    override suspend fun detect(context: DeviceGuardContext): DetectionResult<SurveillanceCheckResult> {
        val outcome = probe(context)
        if (!outcome.applicable) return DetectionResult.NotApplicable(id, outcome.reason)

        val grouped: Map<SurveillanceCategory, List<SurveillanceIndicator>> = outcome.indicators.groupBy { it.category }
        val confidences: Map<SurveillanceCategory, Float> =
            SurveillanceCategory.entries.associateWith { category ->
                val list = grouped[category].orEmpty()
                list.sumOf { it.weight.toDouble() }.toFloat().coerceIn(0f, 1f)
            }

        val threats =
            buildList {
                for ((category, confidence) in confidences) {
                    if (confidence < THRESHOLD) continue
                    val type = category.threatType()
                    val indicators = (grouped[category].orEmpty()).map { it.name }
                    add(DetectedThreat.of(threat = type, confidence = confidence, indicators = indicators))
                }
            }

        return DetectionResult.Success(
            detectorId = id,
            data =
                SurveillanceCheckResult(
                    accessibilityAbuse = confidences.isTripped(SurveillanceCategory.AccessibilityAbuse),
                    overlayPermission = confidences.isTripped(SurveillanceCategory.OverlayPermission),
                    notificationListener = confidences.isTripped(SurveillanceCategory.NotificationListener),
                    deviceAdminActive = confidences.isTripped(SurveillanceCategory.DeviceAdminActive),
                    suspiciousIme = confidences.isTripped(SurveillanceCategory.SuspiciousIme),
                    usageStatsGranted = confidences.isTripped(SurveillanceCategory.UsageStatsGranted),
                    automationToolRunning = confidences.isTripped(SurveillanceCategory.AutomationToolRunning),
                    debuggerAttachedElsewhere = confidences.isTripped(SurveillanceCategory.DebuggerAttachedElsewhere),
                    indicators = outcome.indicators.map { it.name },
                ),
            threats = threats,
            signals =
                mapOf(
                    "surveillancecheck.threshold" to THRESHOLD.toString(),
                    "surveillancecheck.indicator_count" to outcome.indicators.size.toString(),
                ),
        )
    }

    private fun Map<SurveillanceCategory, Float>.isTripped(category: SurveillanceCategory): Boolean = (this[category] ?: 0f) >= THRESHOLD

    private fun SurveillanceCategory.threatType(): ThreatType =
        when (this) {
            SurveillanceCategory.AccessibilityAbuse -> ThreatType.AccessibilityAbuse
            SurveillanceCategory.OverlayPermission -> ThreatType.OverlayPermission
            SurveillanceCategory.NotificationListener -> ThreatType.NotificationListener
            SurveillanceCategory.DeviceAdminActive -> ThreatType.DeviceAdminActive
            SurveillanceCategory.SuspiciousIme -> ThreatType.SuspiciousIme
            SurveillanceCategory.UsageStatsGranted -> ThreatType.UsageStatsGranted
            SurveillanceCategory.AutomationToolRunning -> ThreatType.AutomationToolRunning
            SurveillanceCategory.DebuggerAttachedElsewhere -> ThreatType.DebuggerAttachedElsewhere
        }
}

/** Attach a [SurveillanceCheckDetector] to the builder. */
public fun DeviceGuard.Builder.enableSurveillanceCheck(): DeviceGuard.Builder = addDetector(SurveillanceCheckDetector())
