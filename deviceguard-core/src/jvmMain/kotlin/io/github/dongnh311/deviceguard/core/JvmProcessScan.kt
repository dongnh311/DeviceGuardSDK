package io.github.dongnh311.deviceguard.core

import java.util.Locale

/** Lower-cased basename of a process command path; strips extension. */
@InternalDeviceGuardApi
public fun String.toProcessBasename(): String =
    substringAfterLast('/')
        .substringAfterLast('\\')
        .removeSuffix(".exe")
        .lowercase(Locale.US)

/**
 * Iterate every running process, return the subset of [needles] whose basename is present.
 *
 * Used by `deviceguard-remote` and `deviceguard-surveillance` to classify process-based
 * signals. Both callers pass disjoint needles, so no cross-contamination.
 *
 * Returns a [Set] (not a [List]) — duplicates are meaningless here and set comparisons at
 * the call site are O(1).
 */
@InternalDeviceGuardApi
public fun scanJvmProcessBasenames(needles: Set<String>): Set<String> {
    if (needles.isEmpty()) return emptySet()
    val hits = mutableSetOf<String>()
    ProcessHandle.allProcesses().use { stream ->
        stream.forEach { handle ->
            val command = handle.info().command().orElse(null) ?: return@forEach
            val basename = command.toProcessBasename()
            if (basename in needles) hits += basename
        }
    }
    return hits
}
