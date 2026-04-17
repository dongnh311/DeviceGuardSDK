package io.github.dongnh311.deviceguard.core

import android.content.Context

/**
 * Android actualization of [DeviceGuardContext].
 *
 * Wraps an `android.content.Context`. Callers should pass `context.applicationContext` to
 * avoid retaining an Activity beyond its lifecycle.
 */
public actual class DeviceGuardContext(
    public val androidContext: Context,
)
