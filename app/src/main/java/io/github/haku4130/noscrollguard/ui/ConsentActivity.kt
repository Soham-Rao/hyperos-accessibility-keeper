package io.github.haku4130.noscrollguard.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.CheckBox
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.github.haku4130.noscrollguard.Consent
import io.github.haku4130.noscrollguard.R

class ConsentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ui = InfoUi(this)
        val alreadyAccepted = Consent.accepted(this)
        val body = ui.screen(if (alreadyAccepted) "Privacy & safety" else "Before you begin")
        body.addView(ui.card("Choose carefully", "Keeper actively restores accessibility access for services you select. Those third-party services may read screen content or interact with your apps. Only choose apps you trust.", R.drawable.ic_accessibility))
        body.addView(ui.card("Powerful permissions", "Secure-settings access can change system settings. Diagnostic access can read broader system information. Keeper uses these to restore selected services and check their status; it does not limit what the permissions themselves allow.", R.drawable.ic_settings))
        body.addView(ui.card("You're in control", "New services start off. Turn protection off before disabling a service in Android Settings. Pause suspends recovery for 30 minutes. Recovery is best effort and some apps need opening afterward. Uninstalling Keeper stops its work but does not switch off other apps' accessibility access.", R.drawable.ic_power))
        body.addView(ui.card("Private on your phone", "Choices and a limited activity journal stay locally. Keeper has no internet permission or analytics. Review logs before choosing to share them. This experimental beta is provided without warranty under the MIT license, subject to applicable law.", R.drawable.ic_info))
        listOf("permissions" to "Permission guide", "privacy" to "Privacy policy", "terms" to "Terms & limitations", "security" to "Security & reporting").forEach { (id, title) ->
            body.addView(ui.row(title, "Read offline", R.drawable.ic_info) {
                startActivity(Intent(this, LegalDocumentActivity::class.java).putExtra("document", id))
            })
        }
        val confirm = CheckBox(this).apply {
            text = "I understand the risks and agree to the terms. I will choose which services Keeper may restore."
            setTextColor(ui.ink); minHeight = ui.dp(56)
            buttonTintList = ColorStateList.valueOf(ui.accent)
            isChecked = alreadyAccepted
            id = android.R.id.checkbox
        }
        if (!alreadyAccepted) body.addView(confirm)
        val continueButton = Button(this).apply {
            text = if (alreadyAccepted) "Done" else "Continue"; isEnabled = confirm.isChecked
            isAllCaps = false; minHeight = ui.dp(56); elevation = 0f
            background = ui.rounded(ui.accent, 24)
            val states = arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf())
            backgroundTintList = ColorStateList(states, intArrayOf(ui.accent, ui.surface))
            setTextColor(ColorStateList(states, intArrayOf(ui.background, ui.muted)))
            setOnClickListener { Consent.accept(this@ConsentActivity); finish() }
        }
        confirm.setOnCheckedChangeListener { _, checked -> continueButton.isEnabled = checked }
        body.addView(continueButton)
        body.addView(ui.text(if (alreadyAccepted) "Your safety acknowledgment is saved on this phone. Use the app switches or Pause to control recovery."
            else "You can leave this screen without agreeing. Automatic recovery stays off. Your existing selections are preserved.", 12f, ui.muted))
    }
}

