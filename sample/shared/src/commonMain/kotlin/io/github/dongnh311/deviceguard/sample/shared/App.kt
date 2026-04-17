package io.github.dongnh311.deviceguard.sample.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dongnh311.deviceguard.core.RiskLevel
import io.github.dongnh311.deviceguard.core.SecurityReport

/**
 * Shared Compose entry point for every platform wrapper (android / desktop / web / ios).
 * Wrappers pass the platform-specific [io.github.dongnh311.deviceguard.core.DeviceGuardContext]
 * via [createDeviceGuardContext]; everything else is common code.
 */
@Composable
public fun App(useDarkTheme: Boolean = isSystemInDarkTheme()) {
    val colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colorScheme) {
        val context = remember { createDeviceGuardContext() }
        val scope = rememberCoroutineScope()
        val holder = remember(context) { SampleStateHolder(context, scope) }
        val state by holder.state.collectAsState()

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeaderCard()
            DetectorTogglesCard(
                enabled = state.enabledDetectors,
                strictRoot = state.strictRoot,
                onToggle = holder::toggleDetector,
                onStrictChange = holder::toggleStrictRoot,
                enabledInputs = !state.running,
            )
            AnalyzeButton(running = state.running, onClick = holder::analyze)

            val errorMessage = state.lastError
            AnimatedVisibility(visible = errorMessage != null) {
                if (errorMessage != null) ErrorCard(message = errorMessage)
            }

            val report = state.lastReport
            AnimatedVisibility(visible = report != null) {
                if (report != null) {
                    ReportCard(
                        report = report,
                        durationMs = state.lastDurationMs,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("DeviceGuard SDK", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Running on $platformName. Toggle detectors and tap Analyze to inspect the SecurityReport.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun DetectorTogglesCard(
    enabled: Set<DetectorToggle>,
    strictRoot: Boolean,
    onToggle: (DetectorToggle) -> Unit,
    onStrictChange: (Boolean) -> Unit,
    enabledInputs: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Detectors", style = MaterialTheme.typography.titleMedium)
            for (toggle in DetectorToggle.entries) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(toggle.label, style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = toggle in enabled,
                        enabled = enabledInputs,
                        onCheckedChange = { onToggle(toggle) },
                    )
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Strict root threshold", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Trips on any moderate signal (≥ 0.2 confidence).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = strictRoot, enabled = enabledInputs, onCheckedChange = onStrictChange)
            }
        }
    }
}

@Composable
private fun AnalyzeButton(
    running: Boolean,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !running,
        onClick = onClick,
    ) {
        if (running) {
            CircularProgressIndicator(
                modifier = Modifier.size(SPINNER_SIZE.dp),
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(SPINNER_GAP.dp))
        }
        Text(if (running) "Analyzing…" else "Analyze")
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun ReportCard(
    report: SecurityReport,
    durationMs: Long?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Last analysis", style = MaterialTheme.typography.titleMedium)
                AssistChip(
                    onClick = {},
                    label = { Text(report.riskLevel.name) },
                )
            }

            RiskScoreRow(score = report.riskScore, level = report.riskLevel)

            if (durationMs != null) {
                Text(
                    "Took $durationMs ms  —  ${report.threats.size} threat(s), " +
                        "${report.signals.size} signal(s), ${report.errors.size} error(s)",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            val fingerprint = report.fingerprint
            if (fingerprint != null) {
                HorizontalDivider()
                Text("Fingerprint", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = fingerprint.id,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                )
            }

            LabeledBulletList("Threats", report.threats) { threat ->
                "• ${threat.type}  (confidence ${formatConfidence(threat.confidence)}, weight ${threat.weight})"
            }
            LabeledBulletList("Errors", report.errors) { error ->
                "• ${error.detectorId} (${error.errorType ?: "unknown"}): ${error.message}"
            }
        }
    }
}

@Composable
private fun <T> LabeledBulletList(
    label: String,
    items: List<T>,
    line: (T) -> String,
) {
    if (items.isEmpty()) return
    HorizontalDivider()
    Text(label, style = MaterialTheme.typography.labelLarge)
    for (item in items) {
        Text(line(item), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RiskScoreRow(
    score: Int,
    level: RiskLevel,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Risk score", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "$score / 100",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { score / PERCENT.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = level.tint(),
        )
    }
}

@Composable
private fun RiskLevel.tint(): Color =
    when (this) {
        RiskLevel.SAFE -> MaterialTheme.colorScheme.primary
        RiskLevel.LOW -> MaterialTheme.colorScheme.tertiary
        RiskLevel.MEDIUM -> MaterialTheme.colorScheme.secondary
        RiskLevel.HIGH, RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error
    }

/** `String.format` isn't common. Render two decimals manually so JS target compiles. */
private fun formatConfidence(value: Float): String {
    val hundredths = (value * PERCENT).toInt().coerceIn(0, PERCENT)
    if (hundredths == PERCENT) return "1.00"
    return "0.${hundredths.toString().padStart(2, '0')}"
}

private const val PERCENT = 100
private const val SPINNER_SIZE = 20
private const val SPINNER_GAP = 8
