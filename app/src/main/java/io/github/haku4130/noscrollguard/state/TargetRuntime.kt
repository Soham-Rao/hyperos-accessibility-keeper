package io.github.haku4130.noscrollguard.state

import io.github.haku4130.noscrollguard.repair.canonicalService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class RuntimeState(val bound: Boolean, val crashed: Boolean, val binding: Boolean = false)

object TargetRuntime {
    fun read(): String? {
        var child: Process? = null
        return try {
            val process = ProcessBuilder("dumpsys", "-t", "3", "accessibility")
                .redirectErrorStream(true).start()
            child = process
            val output = StringBuilder()
            val incomplete = AtomicBoolean(false)
            val reader = Thread {
                runCatching {
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line -> synchronized(output) {
                            val remaining = 500_000 - output.length
                            if (line.length + 1 > remaining) incomplete.set(true)
                            if (remaining > 0) output.append((line + "\n").take(remaining))
                        } }
                    }
                }.onFailure { incomplete.set(true) }
            }.apply { isDaemon = true; start() }
            if (!process.waitFor(4, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                reader.join(500)
                null
            } else {
                reader.join(500)
                if (reader.isAlive || incomplete.get() || process.exitValue() != 0) null else synchronized(output) { output.toString() }
            }
        } catch (_: InterruptedException) { Thread.currentThread().interrupt(); null }
        catch (_: Exception) { null }
        finally { if (child?.isAlive == true) child.destroyForcibly() }
    }

    fun forCurrentProcess(dump: String?, component: String, label: String? = null): RuntimeState? =
        // AOSP UIDs reserve 100000 IDs per Android user. Avoid hidden APIs and
        // never mistake a parent user's service for this profile's same component.
        parse(dump, component, label, expectedUserId = android.os.Process.myUid() / 100_000)

    fun parse(dump: String?, component: String, label: String? = null, expectedUserId: Int? = null): RuntimeState? {
        if (dump == null) return null
        val user = expectedUserId?.toString() ?: Regex("currentUserId=(\\d+)").find(dump)?.groupValues?.get(1) ?: return null
        val section = dump.split("User state[").firstOrNull {
            Regex("attributes:\\{id=$user[,}]").containsMatchIn(it)
        } ?: return null
        // Sets can span lines. Require a complete balanced field; truncated or
        // unfamiliar diagnostics must not be interpreted as a stopped service.
        fun field(name: String): String? {
            val marker = Regex("(?m)^[ \\t]*${Regex.escape(name)}:[ \\t]*").find(section) ?: return null
            val start = marker.range.last + 1
            if (section.getOrNull(start) != '{') return null
            var depth = 0
            for (index in start until section.length) {
                when (section[index]) { '{' -> depth++; '}' -> depth-- }
                if (depth == 0) return section.substring(start, index + 1)
            }
            return null
        }
        val componentPattern = Regex("\\{([^{}\\s,/]+/[^{}\\s,/]+)\\}")
        fun entries(text: String): Set<String> =
            componentPattern.findAll(text)
                .map { canonicalService(it.groupValues[1]) }.toSet()
        val bound = field("Bound services") ?: return null
        val crashed = field("Crashed services") ?: return null
        val target = canonicalService(component)
        val labelOnly = bound.contains("Service[label=")
        if (!labelOnly && componentPattern.replace(bound, "").any { !it.isWhitespace() && it !in "{}," }) {
            return if (target in entries(crashed)) RuntimeState(false, true) else null
        }
        val safeLabel = label?.takeUnless { Regex("[\\[\\]{},\\r\\n]").containsMatchIn(it) }
        if (safeLabel == null && labelOnly) {
            return if (target in entries(crashed)) RuntimeState(false, true) else null
        }
        // POCO F6 / HyperOS 3 prints bound services as labels rather than component IDs.
        val labelBound = safeLabel != null && bound.contains("Service[label=$safeLabel,")
        val binding = field("Binding services")?.let { target in entries(it) } ?: false
        return RuntimeState((!labelOnly && target in entries(bound)) || labelBound, target in entries(crashed), binding)
    }
}
