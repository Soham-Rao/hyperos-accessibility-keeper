package io.github.haku4130.noscrollguard.repair

import io.github.haku4130.noscrollguard.state.RuntimeState

/** One check at a time owns this state. A healthy sample never erases a recent attempt. */
class RecoveryTracker(private val clock: () -> Long) {
    private val attempts = mutableMapOf<String, Long>()
    private val unsettledSince = mutableMapOf<String, Long>()
    fun retain(components: Set<String>) {
        attempts.keys.retainAll(components); unsettledSince.keys.retainAll(components)
    }
    fun decide(component: String, configured: Boolean, runtime: RuntimeState?, needsOpen: Boolean,
        manual: Boolean = false): RecoveryDecision {
        val now = clock()
        val unsettled = configured && runtime != null && !runtime.bound && !runtime.crashed
        val duration = if (unsettled) (now - unsettledSince.getOrPut(component) { now }).coerceAtLeast(0)
            else { unsettledSince.remove(component); 0L }
        val decision = recoveryDecision(configured, runtime, needsOpen, duration, manual)
        if (decision != RecoveryDecision.REPAIR) return decision
        if (!manual && attempts[component]?.let { now - it < 60_000 } == true) return RecoveryDecision.WAIT
        attempts[component] = now
        return decision
    }
}
