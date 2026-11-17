package io.github.haku4130.noscrollguard.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.haku4130.noscrollguard.R

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ui = InfoUi(this)
        val body = ui.screen("About Keeper")
        val version = packageManager.getPackageInfo(packageName, 0)
        val brand = ui.vertical().apply { gravity = Gravity.CENTER_HORIZONTAL; setPadding(0, ui.dp(6), 0, ui.dp(12)) }
        brand.addView(ImageView(this).apply {
            setImageResource(R.mipmap.ic_keeper)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(ui.dp(72), ui.dp(72)))
        brand.addView(ui.space(14))
        brand.addView(ui.text("Accessibility Keeper", 25f, ui.ink, true).apply { gravity = Gravity.CENTER })
        brand.addView(ui.space(7))
        brand.addView(ui.text("Version ${version.versionName} · build ${version.longVersionCode}", 13f, ui.muted).apply { gravity = Gravity.CENTER })
        brand.addView(ui.space(7))
        brand.addView(ui.text("Experimental beta", 13f, ui.accent, true).apply { gravity = Gravity.CENTER })
        body.addView(brand)
        body.addView(ui.card("Your apps. Your choices.", "Monitors and attempts to restore the accessibility services you choose. Some apps still need to be opened after recovery. Compatibility and recovery are not guaranteed."))
        ui.section(body, "PROJECT & CREDITS")
        body.addView(ui.row("Our repository", "Soham-Rao / hyperos-accessibility-keeper", R.drawable.ic_share) {
            openLink("https://github.com/Soham-Rao/hyperos-accessibility-keeper")
        })
        body.addView(ui.row("Original project", "HyperOS Accessibility Guard by haku4130", R.drawable.ic_info) {
            openLink("https://github.com/haku4130/hyperos-accessibility-guard")
        })
        body.addView(ui.card("Maintained by Soham Rao", "This fork adds selectable multi-app protection, list-preserving recovery, clearer action-needed states and a redesigned interface. Original upstream copyright and license are retained. No upstream or device-vendor endorsement is implied."))
        body.addView(ui.row("Fork changes & provenance", "Source history, changes and license evidence on GitHub", R.drawable.ic_check) {
            openLink("https://github.com/Soham-Rao/hyperos-accessibility-keeper/blob/main/docs/provenance/README.md")
        })
        ui.section(body, "PRIVACY & SAFETY · OFFLINE")
        listOf("permissions" to "Permission guide", "privacy" to "Privacy policy", "terms" to "Terms & limitations", "security" to "Security & reporting").forEach { (id, title) ->
            body.addView(ui.row(title, "Available offline", R.drawable.ic_info) { openDocument(id) })
        }
        ui.section(body, "LICENSES · AVAILABLE OFFLINE")
        body.addView(ui.row("MIT license", "Original and fork notices, with the full license text", R.drawable.ic_info) {
            openDocument("mit")
        })
        body.addView(ui.row("Third-party software", "Dependency credits and resolved component inventory", R.drawable.ic_settings) {
            openDocument("third-party")
        })
        body.addView(ui.row("Apache License 2.0", "Full terms for the runtime libraries", R.drawable.ic_info) {
            openDocument("apache")
        })
        body.addView(ui.space(12))
        body.addView(ui.text("Repository links open your browser. License documents are bundled with the app and need no internet connection.", 12f, ui.muted))
    }
    private fun openDocument(document: String) {
        startActivity(Intent(this, LegalDocumentActivity::class.java).putExtra("document", document))
    }
    private fun openLink(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.onFailure {
            Toast.makeText(this, "No browser is available to open this link", Toast.LENGTH_LONG).show()
        }
    }
}
