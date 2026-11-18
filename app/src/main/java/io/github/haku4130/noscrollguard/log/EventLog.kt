package io.github.haku4130.noscrollguard.log

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(val timestampMs: Long, val message: String, val component: String? = null)

/** Event journal in a text file: one line per event, tab-separated. */
class EventLog(private val file: File, private val maxEntries: Int = 500) {

    @Synchronized
    fun append(timestampMs: Long, message: String, component: String? = null): Boolean {
        // One entry is always one line. Messages are produced by this app and never
        // span multiple lines, so collapsing newlines is enough.
        val flat = message.replace("\n", " / ").replace('\r', ' ').replace('\t', ' ').take(2048)
        val metadata = component?.takeIf { it.length <= 512 && it.none { ch -> ch == '\t' || ch == '\n' || ch == '\r' } }?.let { "\t$it" }.orEmpty()
        val lines = currentLines() + "$timestampMs\t$flat$metadata"
        val trimmed = bounded(lines)
        val temporary = File(file.parentFile, file.name + ".tmp")
        return try {
            temporary.writeText(trimmed.joinToString("\n"))
            try { Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            catch (_: AtomicMoveNotSupportedException) { Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING) }
            true
        } catch (_: IOException) { false }
        catch (_: SecurityException) { false }
        finally { runCatching { temporary.delete() } }
    }

    @Synchronized
    fun read(): List<LogEntry> = currentLines().mapNotNull { line ->
        val parts = line.split('\t', limit = 3)
        if (parts.size < 2) return@mapNotNull null
        val ts = parts[0].toLongOrNull() ?: return@mapNotNull null
        LogEntry(ts, parts[1], parts.getOrNull(2))
    }

    fun asText(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return read().joinToString("\n") { "${fmt.format(Date(it.timestampMs))}  ${it.message}" }
    }

    private fun currentLines(): List<String> = try {
        if (!file.isFile || file.length() > 2_000_000) emptyList()
        else bounded(file.readText().split("\n").filter { it.isNotBlank() })
    } catch (_: IOException) { emptyList() }
    catch (_: SecurityException) { emptyList() }

    private fun bounded(lines: List<String>): List<String> {
        var characters = 0
        val kept = mutableListOf<String>()
        for (line in lines.asReversed()) {
            if (kept.size >= maxEntries || characters + line.length + 1 > 200_000) break
            kept += line; characters += line.length + 1
        }
        return kept.asReversed()
    }
}
