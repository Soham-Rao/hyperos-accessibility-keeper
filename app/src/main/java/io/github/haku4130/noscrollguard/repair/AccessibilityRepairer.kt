package io.github.haku4130.noscrollguard.repair

import io.github.haku4130.noscrollguard.Constants
import io.github.haku4130.noscrollguard.settings.SecureSettings

/** Delay for Android to unbind a service before it is restored. */
const val REBIND_PAUSE_MS = 2000L

sealed class RepairResult {
    object Success : RepairResult()
    object NoPermission : RepairResult()
    object Cancelled : RepairResult()
    data class Failed(val reason: String) : RepairResult()
}

/** Compatibility entry point for the upstream NoScroll tests and helpers. */
class AccessibilityRepairer(
    private val settings: SecureSettings,
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) }
) {
    fun repair(): RepairResult = TargetRepairer(settings, sleeper).repair(Constants.NOSCROLL_SERVICE)
}
