package io.github.dongnh311.deviceguard.remote

import io.github.dongnh311.deviceguard.core.DeviceGuardContext

/** A single remote-control or screen-capture signal. */
internal data class RemoteIndicator(
    val name: String,
    val weight: Float,
)

/**
 * Outcome of platform-specific remote-control probing.
 *
 * `installedIndicators` feeds `RemoteControlInstalled`, `captureIndicators` feeds
 * `ScreenBeingCaptured`. JS always returns `applicable = false` — browsers cannot
 * enumerate installed apps or running processes.
 */
internal data class RemoteCheckOutcome(
    val applicable: Boolean,
    val reason: String? = null,
    val installedIndicators: List<RemoteIndicator> = emptyList(),
    val captureIndicators: List<RemoteIndicator> = emptyList(),
)

/** Platform entry point. Implementations must never throw — catch and return no indicators. */
internal expect suspend fun runRemoteCheck(context: DeviceGuardContext): RemoteCheckOutcome

/**
 * Known Android package IDs for mainstream remote-control / screen-sharing apps.
 *
 * List is small on purpose — false positives here are expensive (a user with TeamViewer
 * installed for legitimate work support would be flagged). Consumers can extend via
 * `RemoteCheckDetector(additionalPackages = ...)`.
 */
internal val KNOWN_REMOTE_PACKAGES: List<String> =
    listOf(
        "com.teamviewer.teamviewer.market.mobile",
        "com.teamviewer.quicksupport.market",
        "com.anydesk.anydeskandroid",
        "com.sand.airdroid",
        "com.sand.airmirror",
        "com.rustdesk.rustdesk",
        "com.splashtop.remote.pad.v2",
        "com.splashtop.remote.business",
        "com.google.chromeremotedesktop",
        "com.microsoft.rdc.android",
        "com.microsoft.rdc.androidx",
        "com.realvnc.viewer.android",
        "net.christianbeier.droidvnc_ng",
        "com.islonline.islpronto",
        "com.monect.free",
        "com.Relmtech.Remote",
    )

/**
 * Known desktop process names for remote-control / VNC / RDP software.
 *
 * Matched case-insensitively against the process basename. Same rationale as the Android
 * list — keep narrow to limit false positives.
 */
internal val KNOWN_REMOTE_PROCESSES: List<String> =
    listOf(
        "teamviewer",
        "teamviewer_service",
        "teamviewerd",
        "tv_bin",
        "anydesk",
        "vncviewer",
        "vncserver",
        "tightvnc",
        "tigervnc",
        "realvnc",
        "x11vnc",
        "rdesktop",
        "xfreerdp",
        "mstsc", // Windows Remote Desktop client
        "rdpclip",
        "splashtop",
        "chromeremotedesktophost",
        "remote_assistance_host",
        "rustdesk",
        "screenconnect",
        "connectwise",
        "logmein",
        "gotoassist",
        "screensharingd", // macOS screen sharing agent
    )
