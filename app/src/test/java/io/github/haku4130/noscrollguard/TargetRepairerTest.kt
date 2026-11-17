package io.github.haku4130.noscrollguard

import io.github.haku4130.noscrollguard.repair.*
import io.github.haku4130.noscrollguard.settings.SecureKeys
import io.github.haku4130.noscrollguard.state.TargetRuntime
import org.junit.Assert.*
import org.junit.Test

class TargetRepairerTest {
    private val target = "com.wispr.flowapp/com.wispr.flowapp.service.FlowAccessibilityService"
    private val other = "other.app/other.app.Service"

    @Test fun `rebind preserves unrelated services and never switches master off`() {
        val fake = FakeSecureSettings(mutableMapOf(SecureKeys.ENABLED_SERVICES to "$other:$target"))
        assertEquals(RepairResult.Success, TargetRepairer(fake) {}.repair(target))
        assertEquals(other, fake.writes.first().second)
        assertEquals("$other:$target", fake.values[SecureKeys.ENABLED_SERVICES])
        assertFalse(fake.writes.contains(SecureKeys.ACCESSIBILITY_ENABLED to "0"))
    }

    @Test fun `merges changes made during the rebind delay`() {
        val fake = FakeSecureSettings(mutableMapOf(SecureKeys.ENABLED_SERVICES to "$other:$target"))
        TargetRepairer(fake) { fake.values[SecureKeys.ENABLED_SERVICES] = "new.app/.Service" }.repair(target)
        assertEquals("new.app/.Service:$target", fake.values[SecureKeys.ENABLED_SERVICES])
    }

    @Test fun `relative service names are not duplicated`() {
        val fake = FakeSecureSettings(mutableMapOf(SecureKeys.ENABLED_SERVICES to "com.wispr.flowapp/.service.FlowAccessibilityService:$other"))
        TargetRepairer(fake) {}.repair(target)
        assertEquals("$other:$target", fake.values[SecureKeys.ENABLED_SERVICES])
    }

    @Test fun `denied writes leave unrelated settings untouched`() {
        val fake = FakeSecureSettings(mutableMapOf(SecureKeys.ENABLED_SERVICES to "$other:$target"))
        fake.writesFail = true
        assertEquals(RepairResult.NoPermission, TargetRepairer(fake) {}.repair(target))
        assertEquals("$other:$target", fake.values[SecureKeys.ENABLED_SERVICES])
    }

    @Test fun `absent target is added without removing existing services`() {
        val fake = FakeSecureSettings(mutableMapOf(SecureKeys.ENABLED_SERVICES to other))
        assertEquals(RepairResult.Success, TargetRepairer(fake) { fail("No rebind delay needed") }.repair(target))
        assertEquals("$other:$target", fake.values[SecureKeys.ENABLED_SERVICES])
    }

    @Test fun `interrupted delay still restores target`() {
        val fake = FakeSecureSettings(mutableMapOf(SecureKeys.ENABLED_SERVICES to "$other:$target"))
        try { TargetRepairer(fake) { throw InterruptedException() }.repair(target) } catch (_: InterruptedException) { }
        assertEquals("$other:$target", fake.values[SecureKeys.ENABLED_SERVICES])
    }

    @Test fun `runtime state uses current user and exact component`() {
        val dump = """currentUserId=0
            User state[
              attributes:{id=0, test=true}
              Bound services:{{com.wispr.flowapp/.service.FlowAccessibilityService}}
              Enabled services:{{$target}}
              Crashed services:{}
            User state[
              attributes:{id=999, test=true}
              Bound services:{}
              Enabled services:{}
              Crashed services:{{$target}}
        """.trimIndent()
        assertEquals(true, TargetRuntime.parse(dump, target)?.bound)
        assertEquals(false, TargetRuntime.parse(dump, target)?.crashed)
        assertNull(TargetRuntime.parse("Permission Denial", target))
    }

    @Test fun `POCO multiline label-only bound entries are recognized`() {
        val dump = """currentUserId=0
            User state[
              attributes:{id=0, test=true}
              Bound services:{Service[label=Auto Clicker *, feedbackType[FEEDBACK_GENERIC]],
                Service[label=Wispr Flow, feedbackType[FEEDBACK_GENERIC]]}
              Enabled services:{{$target}}
              Crashed services:{}
        """.trimIndent()
        assertEquals(true, TargetRuntime.parse(dump, target, "Wispr Flow")?.bound)
    }

    @Test fun `duplicate service labels are not proof that a specific service is bound`() {
        val catalog = listOf(GuardTarget("Same name", "first.app/first.app.Service"), GuardTarget("Same name", "second.app/second.app.Service"))
        assertNull(Targets.runtimeLabel(catalog, catalog.first()))
        val dump = """currentUserId=0
            User state[
              attributes:{id=0, test=true}
              Bound services:{Service[label=Same name, feedbackType[FEEDBACK_GENERIC]]}
              Enabled services:{{first.app/first.app.Service}}
              Crashed services:{}
        """.trimIndent()
        assertNull(TargetRuntime.parse(dump, catalog.first().component, Targets.runtimeLabel(catalog, catalog.first())))
    }

    @Test fun `two services in one package have independent runtime state`() {
        val dump = """currentUserId=0
            User state[
              attributes:{id=0, test=true}
              Bound services:{{example.app/.FirstService}}
              Enabled services:{{example.app/.FirstService}, {example.app/.SecondService}}
              Crashed services:{{example.app/.SecondService}}
        """.trimIndent()
        assertEquals(true, TargetRuntime.parse(dump, "example.app/example.app.FirstService")?.bound)
        assertEquals(false, TargetRuntime.parse(dump, "example.app/example.app.SecondService")?.bound)
        assertEquals(true, TargetRuntime.parse(dump, "example.app/example.app.SecondService")?.crashed)
    }
}
