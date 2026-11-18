package io.github.haku4130.noscrollguard.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.haku4130.noscrollguard.GuardApp
import io.github.haku4130.noscrollguard.R

class LogPreviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ui = InfoUi(this)
        val body = ui.screen("Review activity log")
        val log = GuardApp.eventLog(this).asText()
        body.addView(ui.card("Check before sharing", "This is the full export, including older entries not shown in Recent activity. It contains app names and timestamps. Review it and share only with someone you trust.", R.drawable.ic_info))
        if (log.isNotBlank()) body.addView(ui.row("Share this log", "Choose where to send the text below", R.drawable.ic_share) {
            runCatching {
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, log)
                }, "Share activity log"))
            }.onFailure { Toast.makeText(this, "No app is available to share this log", Toast.LENGTH_LONG).show() }
        })
        body.addView(ui.text(log.ifBlank { "No activity to share yet." }, 14f, ui.muted).apply {
            setTextIsSelectable(true)
            setLineSpacing(ui.dp(3).toFloat(), 1f)
        })
    }
}
