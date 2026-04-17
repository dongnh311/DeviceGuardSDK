package io.github.dongnh311.deviceguard.emulator

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmulatorCheckJvmTest {
    @Test
    fun hotspotRunnerReportsApplicableWithNoSignals() =
        runTest {
            val vmName = System.getProperty("java.vm.name").orEmpty()
            val isAndroidVm = vmName.equals("Dalvik", ignoreCase = true) || vmName.equals("ART", ignoreCase = true)

            val outcome = runEmulatorCheck(fakeContext())

            assertTrue(outcome.applicable, "JVM check is always applicable")
            if (!isAndroidVm) {
                assertTrue(outcome.emulatorIndicators.isEmpty(), "unexpected: $outcome")
            }
            assertFalse(
                outcome.debuggerIndicators.any { it.name.contains("jdwp", ignoreCase = true) },
                "unit tests must not run under a JDWP agent",
            )
        }
}
