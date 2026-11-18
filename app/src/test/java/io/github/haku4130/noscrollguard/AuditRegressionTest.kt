package io.github.haku4130.noscrollguard

import io.github.haku4130.noscrollguard.log.EventLog
import io.github.haku4130.noscrollguard.repair.*
import io.github.haku4130.noscrollguard.state.RuntimeState
import io.github.haku4130.noscrollguard.state.TargetRuntime
import io.github.haku4130.noscrollguard.settings.SecureKeys
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class AuditRegressionTest {
    private val target = "example.voice/example.voice.Service"
    @Test fun `unfamiliar nonempty bound format is unknown instead of failed`() {
        val dump = """currentUserId=0
            User state[
              attributes:{id=0, test=true}
              Bound services:{NewFirmwareConnection(unknown-format)}
              Crashed services:{}
        """.trimIndent()
        assertNull(TargetRuntime.parse(dump, target))
    }
    @Test fun `structured component identifiers support Unicode service classes`() {
        val unicode = "example.voice/example.voice.音声Service"
        val dump = """currentUserId=0
            User state[
              attributes:{id=0, test=true}
              Bound services:{{$unicode}}
              Crashed services:{}
        """.trimIndent()
        assertEquals(RuntimeState(true, false), TargetRuntime.parse(dump, unicode))
    }
    @Test fun `profile diagnostics do not borrow the parent user bound state`() {
        val dump = """currentUserId=0
            User state[
              attributes:{id=0, test=true}
              Bound services:{{$target}}
              Enabled services:{{$target}}
              Crashed services:{}
            User state[
              attributes:{id=10, test=true}
              Bound services:{}
              Enabled services:{{$target}}
              Crashed services:{{$target}}
        """.trimIndent()
        assertEquals(RuntimeState(false, true), TargetRuntime.parse(dump, target, expectedUserId = 10))
        assertEquals(RuntimeState(true, false), TargetRuntime.parse(dump, target, expectedUserId = 0))
    }
    @Test fun `missing own profile diagnostics are unverified`() {
        val dump = """currentUserId=0
            User state[
              attributes:{id=0, test=true}
              Bound services:{{$target}}
              Crashed services:{}
        """.trimIndent()
        assertNull(TargetRuntime.parse(dump, target, expectedUserId = 10))
    }
    @Test fun `journal export stays bounded for Android share transactions`() {
        val file = Files.createTempFile("keeper-journal", ".log").toFile()
        try {
            val log = EventLog(file)
            repeat(500) { log.append(it.toLong(), "x".repeat(2048)) }
            assertTrue(log.asText().length <= 210_000)
            assertTrue(log.read().size < 500)
            assertEquals(499L, log.read().last().timestampMs)
        } finally { file.delete() }
    }
    @Test fun `stop immediately before repair prevents all writes`() {
        val settings = FakeSecureSettings(mutableMapOf(SecureKeys.ENABLED_SERVICES to "other.app/.Service"))
        assertEquals(RepairResult.Cancelled, TargetRepairer(settings) {}.repair(target, canStart = { false }))
        assertTrue(settings.writes.isEmpty())
    }
    @Test fun `changed installation during rebind never receives automatic restored access`() {
        val settings = FakeSecureSettings(mutableMapOf(SecureKeys.ENABLED_SERVICES to "other.app/.Service:$target"))
        var sameInstall = true
        val result = TargetRepairer(settings) { sameInstall = false }.repair(target, canRestore = { sameInstall })
        assertEquals(RepairResult.Cancelled, result)
        assertEquals("other.app/.Service", settings.values[SecureKeys.ENABLED_SERVICES])
        assertFalse(settings.writes.any { it.first == SecureKeys.ACCESSIBILITY_ENABLED })
    }
    @Test fun `stopping during rebind finishes restoring original trusted installation`() {
        val settings = FakeSecureSettings(mutableMapOf(SecureKeys.ENABLED_SERVICES to target))
        var enabled = true
        assertEquals(RepairResult.Success, TargetRepairer(settings) { enabled = false }.repair(target, canStart = { enabled }))
        assertEquals(target, settings.values[SecureKeys.ENABLED_SERVICES])
    }
    @Test fun `a transient unbound reading without a crash gets time to settle`() {
        assertEquals(RecoveryDecision.WAIT, recoveryDecision(true, RuntimeState(false, false), false, 0))
        assertEquals(RecoveryDecision.REPAIR, recoveryDecision(true, RuntimeState(false, false), false, 30_000))
    }
    @Test fun `all entries in a multiline crashed set are read`() {
        val dump = """currentUserId=0
            User state[
              attributes:{id=0, test=true}
              Bound services:{}
              Enabled services:{{$target}}
              Binding services:{}
              Crashed services:{{other.app/.Service},
                {$target}}
        """.trimIndent()
        assertTrue(TargetRuntime.parse(dump, target)?.crashed == true)
    }
    @Test fun `component strings inside app labels cannot prove binding`() {
        val dump = """currentUserId=0
            User state[
              attributes:{id=0, test=true}
              Bound services:{Service[label={$target}, feedbackType[FEEDBACK_GENERIC]]}
              Enabled services:{{$target}}
              Crashed services:{}
        """.trimIndent()
        assertNull(TargetRuntime.parse(dump, target))
    }
    @Test fun `diagnostic-like labels disable label-only proof for the catalog`() {
        val catalog = listOf(GuardTarget("Voice helper", target), GuardTarget("Service[label=Voice helper,", "other.app/other.app.Service"))
        assertNull(Targets.runtimeLabel(catalog, catalog.first()))
    }
    @Test fun `carriage returns cannot create misleading log lines`() {
        val file = Files.createTempFile("keeper-journal", ".log").toFile()
        try {
            val log = EventLog(file)
            log.append(1, "App\rforged event", "example.app/.Service\rforged")
            assertFalse(log.read().single().message.contains('\r'))
            assertNull(log.read().single().component)
        } finally { file.delete() }
    }
    @Test fun `one oversized app label cannot produce an unbounded journal entry`() {
        val file = Files.createTempFile("keeper-journal", ".log").toFile()
        try {
            EventLog(file).append(1, "x".repeat(100_000))
            assertTrue(file.length() < 4096)
        } finally { file.delete() }
    }
    @Test fun `journal I O failure does not throw into recovery`() {
        val directory = Files.createTempDirectory("keeper-journal").toFile()
        try {
            EventLog(directory).append(1, "event")
            assertTrue(EventLog(directory).read().isEmpty())
        } finally { directory.delete() }
    }
}
