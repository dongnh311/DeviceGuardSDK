package io.github.dongnh311.deviceguard.rootcheck

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class RootCheckJvmTest {
    @Test
    fun desktopJvmIsNotApplicable() =
        runTest {
            val outcome = runRootCheck(fakeContext())
            assertFalse(outcome.applicable)
            assertNotNull(outcome.reason)
            assertEquals(emptyList(), outcome.indicators)
        }
}
