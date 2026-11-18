package io.github.haku4130.noscrollguard

import io.github.haku4130.noscrollguard.repair.*
import io.github.haku4130.noscrollguard.state.RuntimeState
import org.junit.Assert.*
import org.junit.Test

class RecoveryTrackerTest {
    @Test fun `unknown diagnostics cannot reuse a previous open-app flag as current failure proof`() {
        assertEquals(RecoveryDecision.UNVERIFIED, recoveryDecision(true, null, true, 0))
    }
    @Test fun `flapping healthy state does not bypass attempt cooldown`() {
        var now = 0L
        val tracker = RecoveryTracker { now }
        val crashed = RuntimeState(false, true)
        assertEquals(RecoveryDecision.REPAIR, tracker.decide("service", true, crashed, false))
        now = 10_000
        assertEquals(RecoveryDecision.HEALTHY, tracker.decide("service", true, RuntimeState(true, false), false))
        now = 11_000
        assertEquals(RecoveryDecision.WAIT, tracker.decide("service", true, crashed, false))
        now = 60_000
        assertEquals(RecoveryDecision.REPAIR, tracker.decide("service", true, crashed, false))
    }
    @Test fun `manual retry and different services remain independent`() {
        val tracker = RecoveryTracker { 0 }
        val crashed = RuntimeState(false, true)
        tracker.decide("first", true, crashed, false)
        assertEquals(RecoveryDecision.REPAIR, tracker.decide("second", true, crashed, false))
        assertEquals(RecoveryDecision.REPAIR, tracker.decide("first", true, crashed, true, manual = true))
    }
    @Test fun `transient disconnected state settles before repair and unknown readings reset grace`() {
        var now = 0L
        val tracker = RecoveryTracker { now }
        val pending = RuntimeState(false, false)
        assertEquals(RecoveryDecision.WAIT, tracker.decide("service", true, pending, false))
        now = 30_000
        assertEquals(RecoveryDecision.UNVERIFIED, tracker.decide("service", true, null, false))
        assertEquals(RecoveryDecision.WAIT, tracker.decide("service", true, pending, false))
        now = 60_000
        assertEquals(RecoveryDecision.REPAIR, tracker.decide("service", true, pending, false))
    }
}
