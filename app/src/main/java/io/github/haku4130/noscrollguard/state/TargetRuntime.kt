package io.github.haku4130.noscrollguard.state

import io.github.haku4130.noscrollguard.repair.canonicalService
import java.util.concurrent.TimeUnit

data class RuntimeState(val bound: Boolean, val crashed: Boolean, val binding: Boolean = false)

object TargetRuntime {
    fun read(): String? = try {
        val process = ProcessBuilder("dumpsys", "-t", "3", "accessibility")
            .redirectErrorStream(true).start()
        val output = StringBuilder()
        val reader = Thread {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { synchronized(output) { if (output.length < 500_000) output.appendLine(it) } }
            }
        }.apply { isDaemon = true; start() }
        if (!process.waitFor(4, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            reader.join(500)
            null
        } else {
            reader.join(500)
            if (reader.isAlive || process.exitValue() != 0) null else synchronized(output) { output.toString() }
        }
    } catch (_: Exception) { null }

    fun parse(dump: String?, component: String, label: String? = null): RuntimeState? {
        if (dump == null) return null
        val user = Regex("currentUserId=(\\d+)").find(dump)?.groupValues?.get(1) ?: return null
        val section = dump.split("User state[").firstOrNull {
            Regex("attributes:\\{id=$user[,}]").containsMatchIn(it)
        } ?: return null
        fun block(start: String, end: String): String? =
            if (section.contains(start) && section.substringAfter(start).contains(end))
                section.substringAfter(start).substringBefore(end) else null
        fun entries(text: String): Set<String> =
            Regex("[A-Za-z0-9_.$]+/[A-Za-z0-9_.$]+").findAll(text)
                .map { canonicalService(it.value) }.toSet()
        val bound = block("Bound services:", "Enabled services:") ?: return null
        val crashed = section.lineSequence().firstOrNull { it.trimStart().startsWith("Crashed services:") } ?: return null
        val target = canonicalService(component)
        if (label == null && bound.contains("Service[label=") && entries(bound).isEmpty()) {
            return if (target in entries(crashed)) RuntimeState(false, true) else null
        }
        // POCO F6 / HyperOS 3 prints bound services as labels rather than component IDs.
        val labelBound = label != null && bound.contains("Service[label=$label,")
        val binding = block("Binding services:", "Crashed services:")?.let { target in entries(it) } ?: false
        return RuntimeState(target in entries(bound) || labelBound, target in entries(crashed), binding)
    }
}
