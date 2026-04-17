package io.github.dongnh311.deviceguard.integrity

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class IntegrityCheckJvmTest {
    @Test
    fun desktopJvmIsNotApplicable() =
        runTest {
            val outcome =
                runIntegrityCheck(
                    fakeContext(),
                    IntegrityCheckConfig(expectedSignatureSha256 = null, trustedInstallers = emptySet()),
                )
            assertFalse(outcome.applicable)
            assertNotNull(outcome.reason)
        }
}
