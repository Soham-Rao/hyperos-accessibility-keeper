package io.github.haku4130.noscrollguard.repair

import io.github.haku4130.noscrollguard.state.RuntimeState

enum class RecoveryDecision { HEALTHY, UNVERIFIED, WAIT, REPAIR, OPEN_APP }

/** Avoid interrupting an in-progress bind or endlessly cycling a nonresponsive service. */
fun recoveryDecision(configured: Boolean, runtime: RuntimeState?, needsOpen: Boolean,
    bindingForMs: Long, manual: Boolean = false): RecoveryDecision = when {
    !configured -> RecoveryDecision.REPAIR
    runtime?.bound == true && !runtime.crashed -> RecoveryDecision.HEALTHY
    runtime == null -> RecoveryDecision.UNVERIFIED
    needsOpen && !manual -> RecoveryDecision.OPEN_APP
    !runtime.crashed && bindingForMs < 30_000 && !manual -> RecoveryDecision.WAIT
    else -> RecoveryDecision.REPAIR
}
