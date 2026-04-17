package io.github.dongnh311.deviceguard.sample.shared

import android.annotation.SuppressLint
import android.content.Context
import io.github.dongnh311.deviceguard.core.DeviceGuardContext

internal actual val platformName: String = "Android"

@SuppressLint("StaticFieldLeak")
private lateinit var appContext: Context

/**
 * Called from the Android wrapper's `Application.onCreate()` to hand the shared module the
 * application context. Detector modules only ever use `.applicationContext`, so the held
 * reference is not an Activity leak.
 */
public fun initializeAndroidSample(context: Context) {
    appContext = context.applicationContext
}

internal actual fun createDeviceGuardContext(): DeviceGuardContext {
    check(::appContext.isInitialized) {
        "initializeAndroidSample(context) must be called before composing App()"
    }
    return DeviceGuardContext(appContext)
}
