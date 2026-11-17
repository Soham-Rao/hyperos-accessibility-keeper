package io.github.haku4130.noscrollguard

/** Updates preserve choices; reinstalls require opt-in and the same signing identity. */
data class InstallIdentity(val firstInstallTime: Long, val signer: String)

fun keepSelection(previous: InstallIdentity?, current: InstallIdentity?, retain: Boolean): Boolean =
    current != null && (previous == null ||
        (previous.signer == current.signer &&
            (previous.firstInstallTime == current.firstInstallTime || retain)))
