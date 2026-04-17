package io.github.dongnh311.deviceguard.core

import kotlin.test.Test
import kotlin.test.assertTrue

class DeviceGuardVersionTest {
    @Test
    fun versionIsNotBlank() {
        assertTrue(DeviceGuardVersion.VERSION.isNotBlank())
    }

    @Test
    fun schemaVersionIsPositive() {
        assertTrue(DeviceGuardVersion.REPORT_SCHEMA > 0)
    }
}
