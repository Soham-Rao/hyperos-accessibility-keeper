package io.github.haku4130.noscrollguard

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Rect
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import io.github.haku4130.noscrollguard.ui.*
import java.io.File

/** Manual, local device suite. Intercepts external intents so no log is sent. */
class UiAudit(private val instrumentation: Instrumentation) {
    private val context = instrumentation.targetContext
    private val automation = instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    private var external: Intent? = null
    private val monitor = object : Instrumentation.ActivityMonitor() {
        override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
            if (intent.component?.packageName == context.packageName) return null
            external = intent
            return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
        }
    }
    private fun ui(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private fun views(root: View): List<View> = listOf(root) +
        if (root is ViewGroup) (0 until root.childCount).flatMap { views(root.getChildAt(it)) } else emptyList()
    private fun find(label: String, contains: Boolean = false): View {
        var found: View? = null
        repeat(50) {
            ui {
                found = WindowInspector.getGlobalWindowViews().filter { it.hasWindowFocus() }
                .flatMap(::views).firstOrNull {
                    val text = (it as? TextView)?.text?.toString().orEmpty()
                    (if (contains) text.contains(label) else text == label) || it.contentDescription?.toString() == label
                }
            }
            if (found != null) return found!!
            Thread.sleep(100)
        }
        return checkNotNull(found) { "Missing UI control: $label" }
    }
    private fun click(label: String, contains: Boolean = false) {
        val view = find(label, contains)
        ui {
            view.requestRectangleOnScreen(Rect(0, 0, view.width, view.height), true)
            var action = view
            while (!action.isClickable && action.parent is View) action = action.parent as View
            check(action.performClick()) { "Control is not clickable: $label" }
        }
        instrumentation.waitForIdleSync()
    }
    private fun next(type: Class<out Activity>, action: () -> Unit): Activity {
        val activityMonitor = instrumentation.addMonitor(type.name, null, false)
        action()
        val activity = instrumentation.waitForMonitorWithTimeout(activityMonitor, 5000)
        instrumentation.removeMonitor(activityMonitor)
        instrumentation.waitForIdleSync()
        return checkNotNull(activity) { "Screen did not open: ${type.simpleName}" }
    }
    private fun finish(activity: Activity) { ui { activity.finish() }; instrumentation.waitForIdleSync() }
    private fun span(label: String) {
        val view = find(label, true) as TextView
        ui {
            val text = view.text as Spanned
            val start = text.toString().indexOf(label)
            val links = text.getSpans(start, start + label.length, ClickableSpan::class.java)
            check(links.size == 1) { "Expected one working link: $label" }
            links.single().onClick(view)
        }
        instrumentation.waitForIdleSync()
    }
    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        Thread.sleep(500) // Let the system's screen transition complete before capture.
        val bitmap = checkNotNull(automation.takeScreenshot())
        File(context.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    fun run() {
        instrumentation.addMonitor(monitor)
        try {
            check(Consent.accepted(context)) { "Run isolated control checks first" }
            var main = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            instrumentation.waitForIdleSync()
            val activityButton = find("Recent activity")
            ui { check(activityButton.createAccessibilityNodeInfo().className == "android.widget.Button") }
            val all = find("All apps")
            ui { check(all.isSelected) }
            val search = find("Search accessibility apps") as EditText
            ui { search.setText("no-such-accessibility-service-123") }
            check((find("No apps match your search") as TextView).isShown)
            ui { search.setText("") }
            ui {
                search.requestFocus()
                main.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                    .showSoftInput(search, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
            Thread.sleep(1000)
            ui {
                val root = main.window.decorView
                val insets = androidx.core.view.ViewCompat.getRootWindowInsets(root)
                check(insets?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true) { "Keyboard did not appear for layout check" }
                val keyboard = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom
                val rect = Rect()
                activityButton.getGlobalVisibleRect(rect)
                check(rect.bottom <= root.height - keyboard) { "Keyboard covers bottom action" }
                androidx.core.view.WindowCompat.getInsetsController(main.window, root).hide(androidx.core.view.WindowInsetsCompat.Type.ime())
                search.clearFocus()
            }
            instrumentation.waitForIdleSync()
            val fixture = Targets.installed(context).single { it.packageName == "com.example.keeperfixture" }
            val toggle = find("Protect ${fixture.name}") as CompoundButton
            val selected = toggle.isChecked
            click(fixture.name)
            find("Open app")
            click("Done")
            ui { check(toggle.isChecked == selected) { "Tile tap changed selection" } }
            click("Protected ·", true)
            ui { check(findSelected(main, "Protected ·")) }
            click("All apps")
            click("More options"); click("Settings")
            val retention = find("Keep choices after reinstalling apps") as CompoundButton
            ui { check(retention.createAccessibilityNodeInfo().isCheckable) }
            next(ConsentActivity::class.java) { click("Privacy, safety & terms") }
            instrumentation.waitForIdleSync()
            val safety = automation.rootInActiveWindow
            check(safety != null)
            val permission = next(LegalDocumentActivity::class.java) { click("Permission guide") }
            ui { check(findHeading(permission, "Secure settings: WRITE_SECURE_SETTINGS")) }
            screenshot("audit-permissions-dark.png")
            external = null; span("Android permission reference")
            check(external?.dataString == "https://developer.android.com/reference/android/Manifest.permission")
            finish(permission)
            val privacy = next(LegalDocumentActivity::class.java) { click("Privacy policy") }
            val security = next(LegalDocumentActivity::class.java) { span("security reporting") }
            find("Security & reporting"); finish(security); finish(privacy)
            val terms = next(LegalDocumentActivity::class.java) { click("Terms & limitations") }
            val mit = next(LegalDocumentActivity::class.java) { span("Full MIT license") }
            find("Copyright (c) 2026 haku4130", true)
            find("Copyright (c) 2026 Soham Rao", true)
            finish(mit); finish(terms)
            click("Done") // Existing acknowledgment; returns to Settings.
            find("Privacy, safety & terms")
            val scroll = main.findViewById<ScrollView>(R.id.settings_scroll)
            val before = scroll.scrollY
            val about = next(AboutActivity::class.java) { click("About Keeper") }
            find("Original project"); finish(about)
            ui { check(main.findViewById<ScrollView>(R.id.settings_scroll).scrollY >= before) { "Returning reset Settings scroll" } }
            click("Back to apps"); click("Recent activity")
            val preview = next(LogPreviewActivity::class.java) { click("Share activity log") }
            val log = GuardApp.eventLog(context).asText()
            find(log)
            external = null; click("Share this log")
            val payload = external?.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            check(payload?.getStringExtra(Intent.EXTRA_TEXT) == log) { "Share differs from reviewed text" }
            finish(preview)
            click("Back to apps")
            click("More options"); click("Settings"); click("Appearance")
            val recreated = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
            click("Light")
            main = checkNotNull(instrumentation.waitForMonitorWithTimeout(recreated, 5000)) { "Theme change did not recreate screen" }
            instrumentation.removeMonitor(recreated)
            instrumentation.waitForIdleSync()
            screenshot("audit-settings-light.png")
            // Change only this disposable process's resources, never the phone's font setting.
            ui {
                val config = android.content.res.Configuration(main.resources.configuration).apply { fontScale = 2f }
                @Suppress("DEPRECATION")
                main.resources.updateConfiguration(config, main.resources.displayMetrics)
            }
            // Rebuild the views using this process-local resource configuration.
            click("Back to apps"); click("More options"); click("Settings")
            instrumentation.waitForIdleSync()
            check(main.resources.configuration.fontScale >= 1.9f)
            screenshot("audit-settings-large-text.png")
            find("Keep choices after reinstalling apps")
            finish(main)
        } finally { instrumentation.removeMonitor(monitor) }
    }
    private fun findSelected(activity: Activity, prefix: String): Boolean = views(activity.window.decorView)
        .filterIsInstance<TextView>().first { it.text.toString().startsWith(prefix) }.isSelected
    private fun findHeading(activity: Activity, title: String): Boolean = views(activity.window.decorView)
        .filterIsInstance<TextView>().first { it.text.toString() == title }.createAccessibilityNodeInfo().isHeading
}

