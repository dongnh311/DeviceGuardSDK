package io.github.dongnh311.deviceguard.core

/** Returns a platform-appropriate [DeviceGuardContext] for tests. */
internal expect fun testContext(): DeviceGuardContext

internal fun <T> stubDetector(
    id: String,
    result: DetectionResult<T>,
): Detector<T> =
    object : Detector<T> {
        override val id: String = id

        override suspend fun detect(context: DeviceGuardContext): DetectionResult<T> = result
    }
