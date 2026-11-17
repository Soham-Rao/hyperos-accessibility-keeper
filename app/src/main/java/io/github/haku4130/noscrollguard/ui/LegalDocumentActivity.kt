package io.github.haku4130.noscrollguard.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.haku4130.noscrollguard.R

class LegalDocumentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only fixed bundled documents can be opened; no arbitrary file/URL loading.
        val id = intent.getStringExtra("document")
        val document = when (id) {
            "mit" -> "MIT license" to "legal/MIT.txt"
            "third-party" -> "Third-party software" to "legal/THIRD_PARTY_NOTICES.txt"
            "apache" -> "Apache License 2.0" to "legal/APACHE-2.0.txt"
            "privacy" -> "Privacy policy" to "legal/PRIVACY.md"
            "terms" -> "Terms & limitations" to "legal/TERMS.md"
            "security" -> "Security & reporting" to "legal/SECURITY.md"
            "permissions" -> "Permission guide" to "legal/PERMISSIONS.md"
            else -> { finish(); return }
        }
        val ui = InfoUi(this)
        val body = ui.screen(document.first)
        val raw = assets.open(document.second).bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (!document.second.endsWith(".md")) {
            body.addView(linkedText(ui, raw, markdown = false))
            return
        }
        val overview = when (id) {
            "privacy" -> "Your data, your choice" to "On-device storage · Optional sharing"
            "terms" -> "Know the limits" to "Experimental beta · MIT license"
            "security" -> "Report safely" to "Keep sensitive details private"
            else -> "Understand the access" to "Purpose · Broader capabilities · Your controls"
        }
        body.addView(ui.card(overview.first, overview.second, sectionIcon(id.orEmpty())))
        body.addView(ui.text("Full text below. Underlined links are tappable; policy links stay offline and web links open your browser.", 12f, ui.muted).apply {
            setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(14))
        })
        var section: LinearLayout? = null
        raw.trim().split(Regex("\\r?\\n\\s*\\r?\\n")).forEach { block ->
            when {
                block.startsWith("# ") -> Unit // The title is already in the toolbar.
                block.startsWith("## ") -> {
                    val title = block.removePrefix("## ").trim()
                    section = (ui.card(title, "", sectionIcon(title)) as LinearLayout).also { body.addView(it) }
                }
                else -> {
                    val paragraph = linkedText(ui, block)
                    if (section != null) {
                        section?.addView(ui.space(12)); section?.addView(paragraph)
                    } else body.addView(paragraph.apply { setPadding(ui.dp(4), 0, ui.dp(4), ui.dp(16)) })
                }
            }
        }
    }

    private fun sectionIcon(title: String): Int = when {
        title.contains("notification", true) -> R.drawable.ic_bell
        title.contains("sharing", true) || title.contains("contact", true) -> R.drawable.ic_share
        title.contains("control", true) || title.contains("retention", true) || title.contains("background", true) -> R.drawable.ic_power
        title.contains("diagnostic", true) || title.contains("security", true) -> R.drawable.ic_check
        title.contains("permission", true) || title.contains("settings", true) -> R.drawable.ic_settings
        title.contains("app", true) || title.contains("choice", true) -> R.drawable.ic_accessibility
        else -> R.drawable.ic_info
    }

    private fun linkedText(ui: InfoUi, raw: String, markdown: Boolean = true): TextView {
        val text = SpannableStringBuilder()
        val links = mutableListOf<Triple<Int, Int, String>>()
        var cursor = 0
        if (markdown) Regex("\\[([^]]+)]\\(([^)]+)\\)").findAll(raw).forEach { match ->
            text.append(raw.substring(cursor, match.range.first))
            val start = text.length
            text.append(match.groupValues[1])
            links += Triple(start, text.length, match.groupValues[2])
            cursor = match.range.last + 1
        }
        text.append(raw.substring(cursor))
        Linkify.addLinks(text, Linkify.WEB_URLS)
        links.forEach { (start, end, destination) ->
            text.getSpans(start, end, URLSpan::class.java).forEach(text::removeSpan)
            text.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) { openLink(destination) }
            }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return ui.text("", 14f, ui.muted).apply {
            setTextIsSelectable(true)
            setText(text, TextView.BufferType.SPANNABLE)
            setLinkTextColor(ui.accent)
            movementMethod = LinkMovementMethod.getInstance()
            setLineSpacing(ui.dp(3).toFloat(), 1f)
        }
    }

    private fun openLink(destination: String) {
        val internal = when (destination) {
            "PRIVACY.md" -> "privacy"
            "TERMS.md" -> "terms"
            "SECURITY.md" -> "security"
            "docs/PERMISSIONS.md" -> "permissions"
            "LICENSE" -> "mit"
            else -> null
        }
        if (internal != null) {
            startActivity(Intent(this, LegalDocumentActivity::class.java).putExtra("document", internal))
            return
        }
        val url = if (destination == "docs/INSTALL.md") "https://github.com/Soham-Rao/hyperos-accessibility-keeper/blob/main/docs/INSTALL.md" else destination
        if (Uri.parse(url).scheme !in listOf("https", "http")) return
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.onFailure {
            Toast.makeText(this, "No browser is available to open this link", Toast.LENGTH_LONG).show()
        }
    }
}
