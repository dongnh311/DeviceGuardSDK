package io.github.dongnh311.deviceguard.fingerprint

import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import io.github.dongnh311.deviceguard.core.DeviceGuardContext
import java.util.TimeZone

/**
 * Android signals.
 *
 * Collects stable device-class identifiers (manufacturer, model, SDK) and environment shape
 * (locale, timezone, screen density). `Settings.Secure.ANDROID_ID` is included when
 * available — users may reset it by wiping app data or factory-resetting, which is the
 * intended invalidation signal.
 *
 * Nothing in this list is considered PII. MAC addresses, IMEI, ad id, and account names are
 * excluded by design.
 */
internal actual fun collectFingerprintSignals(context: DeviceGuardContext): Map<String, String> {
    val android = context.androidContext
    val signals = HashMap<String, String>()

    signals["device.manufacturer"] = Build.MANUFACTURER.orEmpty()
    signals["device.brand"] = Build.BRAND.orEmpty()
    signals["device.model"] = Build.MODEL.orEmpty()
    signals["device.device"] = Build.DEVICE.orEmpty()
    signals["device.product"] = Build.PRODUCT.orEmpty()
    signals["device.hardware"] = Build.HARDWARE.orEmpty()
    signals["os.sdk_int"] = Build.VERSION.SDK_INT.toString()
    signals["os.release"] = Build.VERSION.RELEASE.orEmpty()
    signals["os.abis"] = Build.SUPPORTED_ABIS?.joinToString(",").orEmpty()

    runCatching {
        val id = Settings.Secure.getString(android.contentResolver, Settings.Secure.ANDROID_ID)
        if (!id.isNullOrBlank() && id != INVALID_ANDROID_ID) signals["android.id"] = id
    }

    runCatching {
        val display = android.resources.displayMetrics
        signals["screen.width_px"] = display.widthPixels.toString()
        signals["screen.height_px"] = display.heightPixels.toString()
        signals["screen.density_dpi"] = display.densityDpi.toString()
    }

    runCatching {
        val locale = android.resources.configuration.primaryLocale()
        signals["locale"] = locale
        signals["timezone"] = TimeZone.getDefault().id
    }

    return signals
}

/** Known placeholder ANDROID_ID emitted by buggy emulators pre-Android 8; treat as absent. */
private const val INVALID_ANDROID_ID = "9774d56d682e549c"

private fun Configuration.primaryLocale(): String =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        locales[0]?.toLanguageTag().orEmpty()
    } else {
        @Suppress("DEPRECATION")
        locale?.toLanguageTag().orEmpty()
    }
