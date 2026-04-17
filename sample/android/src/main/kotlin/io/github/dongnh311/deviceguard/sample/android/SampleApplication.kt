package io.github.dongnh311.deviceguard.sample.android

import android.app.Application
import io.github.dongnh311.deviceguard.sample.shared.initializeAndroidSample

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeAndroidSample(this)
    }
}
