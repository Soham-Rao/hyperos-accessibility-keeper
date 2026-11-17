package io.github.haku4130.noscrollguard.log

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(val timestampMs: Long, val message: String, val component: String? = null)

/** Event journal in a text file: one line per event, tab-separated. */
class EventLog(private val file: File, private val maxEntries: Int = 500) {

    @Synchronized
    fun append(timestampMs: Long, message: String, component: String? = null) {
        // One entry is always one line. Messages are produced by this app and never
        // span multiple lines, so collapsing newlines is enough.
        val flat = message.replace("\n", " / ").replace('\t', ' ')
        val metadata = component?.takeIf { !it.contains('\t') && !it.contains('\n') }?.let { "\t$it" }.orEmpty()
        val lines = currentLines() + "$timestampMs\t$flat$metadata"
        val trimmed = if (lines.size > maxEntries) lines.takeLast(maxEntries) else lines
        file.writeText(trimmed.joinToString("\n"))
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

    private fun currentLines(): List<String> =
        if (!file.exists()) emptyList()
        else file.readText().split("\n").filter { it.isNotBlank() }
}
