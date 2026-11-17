package io.github.haku4130.noscrollguard

import org.junit.Assert.*
import org.junit.Test

class SelectionPolicyTest {
    private val installed = InstallIdentity(10, "signer-a")
    @Test fun `migration preserves existing installed choices`() {
        assertTrue(keepSelection(null, installed, false))
    }
    @Test fun `missing package is not selected`() {
        assertFalse(keepSelection(installed, null, false))
        assertFalse(keepSelection(installed, null, true))
    }
    @Test fun `ordinary updates keep their choice with retention off`() {
        assertTrue(keepSelection(installed, installed, false))
    }
    @Test fun `reinstall clears by default`() {
        assertFalse(keepSelection(installed, installed.copy(firstInstallTime = 20), false))
    }
    @Test fun `opted in reinstall with same signer can retain choice`() {
        assertTrue(keepSelection(installed, installed.copy(firstInstallTime = 20), true))
    }
    @Test fun `changed signing identity never inherits choice`() {
        assertFalse(keepSelection(installed, installed.copy(signer = "different"), true))
        assertFalse(keepSelection(installed, installed.copy(signer = "different"), false))
    }
}
