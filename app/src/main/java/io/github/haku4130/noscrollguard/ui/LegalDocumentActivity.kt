package io.github.haku4130.noscrollguard.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LegalDocumentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only fixed bundled documents can be opened; no arbitrary file/URL loading.
        val document = when (intent.getStringExtra("document")) {
            "mit" -> "MIT license" to "legal/MIT.txt"
            "third-party" -> "Third-party software" to "legal/THIRD_PARTY_NOTICES.txt"
            "apache" -> "Apache License 2.0" to "legal/APACHE-2.0.txt"
            else -> { finish(); return }
        }
        val ui = InfoUi(this)
        val body = ui.screen(document.first)
        val content = assets.open(document.second).bufferedReader(Charsets.UTF_8).use { it.readText() }
        body.addView(ui.text(content, 14f).apply {
            setTextIsSelectable(true)
            setLineSpacing(ui.dp(3).toFloat(), 1f)
            setPadding(ui.dp(4), 0, ui.dp(4), ui.dp(16))
        })
    }
}
