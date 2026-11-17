package io.github.haku4130.noscrollguard

import io.github.haku4130.noscrollguard.repair.*
import io.github.haku4130.noscrollguard.state.RuntimeState
import io.github.haku4130.noscrollguard.state.TargetRuntime
import org.junit.Assert.*
import org.junit.Test

class RecoveryDecisionTest {
    @Test fun `POCO pending bind is parsed separately from bound and crashed`() {
        val component = "com.truedevelopersstudio.automatictap.autoclicker/com.truedevelopersstudio.autoclicker.WorkerService"
        val dump = """currentUserId=0
            User state[
              attributes:{id=0, test=true}
              Bound services:{Service[label=Wispr Flow, feedbackType[FEEDBACK_GENERIC]]}
              Enabled services:{{$component}}
              Binding services:{{$component}}
              Crashed services:{}
        """.trimIndent()
        val state = TargetRuntime.parse(dump, component, "Auto Clicker *")
        assertEquals(RuntimeState(false, false, true), state)
        assertEquals(RecoveryDecision.WAIT, recoveryDecision(true, state, false, 0))
    }
    @Test fun `allow Android to complete an ongoing bind before cycling it`() {
        assertEquals(RecoveryDecision.WAIT, recoveryDecision(true, RuntimeState(false, false, true), false, 10_000))
        assertEquals(RecoveryDecision.REPAIR, recoveryDecision(true, RuntimeState(false, false, true), false, 30_000))
    }
    @Test fun `failed rebind requires opening app instead of endless automatic cycles`() {
        assertEquals(RecoveryDecision.OPEN_APP, recoveryDecision(true, RuntimeState(false, false, true), true, 300_000))
    }
    @Test fun `explicit manual check can retry failed binding`() {
        assertEquals(RecoveryDecision.REPAIR, recoveryDecision(true, RuntimeState(false, false, true), true, 0, manual = true))
    }
    @Test fun `restored binding clears need to open app`() {
        assertEquals(RecoveryDecision.HEALTHY, recoveryDecision(true, RuntimeState(true, false), true, 0))
    }
    @Test fun `missing settings still repaired when previous rebind required opening app`() {
        assertEquals(RecoveryDecision.REPAIR, recoveryDecision(false, RuntimeState(false, false), true, 0))
    }
    @Test fun `unknown runtime is not proof of failure`() {
        assertEquals(RecoveryDecision.UNVERIFIED, recoveryDecision(true, null, false, 0))
    }
}
