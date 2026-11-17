package io.github.haku4130.noscrollguard.ui

import android.Manifest
import android.app.Dialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.text.TextUtils
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import io.github.haku4130.noscrollguard.*
import io.github.haku4130.noscrollguard.pause.DEFAULT_PAUSE_MS
import io.github.haku4130.noscrollguard.repair.*
import io.github.haku4130.noscrollguard.service.GuardService
import io.github.haku4130.noscrollguard.settings.*
import io.github.haku4130.noscrollguard.state.TargetRuntime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val dark get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    private val bg get() = Color.parseColor(if (dark) "#0D1117" else "#F5F7FA")
    private val surface get() = Color.parseColor(if (dark) "#171D25" else "#FFFFFF")
    private val ink get() = Color.parseColor(if (dark) "#EDF3F9" else "#19242E")
    private val muted get() = Color.parseColor(if (dark) "#A2AFBF" else "#5D6B7A")
    private val accent get() = Color.parseColor(if (dark) "#8BE5C2" else "#006B50")
    private val accentBg get() = Color.parseColor(if (dark) "#19372F" else "#DCF5EA")
    private val warning get() = Color.parseColor(if (dark) "#F4CC83" else "#805A12")
    private var catalog = emptyList<GuardTarget>()
    private data class Row(val view: View, val status: TextView, val toggle: SwitchCompat)
    private val rows = mutableMapOf<String, Row>()
    private lateinit var root: LinearLayout
    private lateinit var list: LinearLayout
    private lateinit var title: TextView
    private lateinit var summary: TextView
    private lateinit var pause: TextView
    private lateinit var count: TextView
    private lateinit var empty: TextView
    private lateinit var allChip: TextView
    private lateinit var protectedChip: TextView
    private var query = ""
    private var protectedOnly = false
    private var showingActivity = false
    private var settingsPage = false
    private lateinit var moreButton: ImageButton
    private var busy = false
    private var generation = 0
    private val refresh = object : Runnable {
        override fun run() { render(); handler.postDelayed(this, 4000) }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int = 18) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun touchSurface(color: Int, radius: Int = 18) = RippleDrawable(
        ColorStateList.valueOf(if (dark) 0x248BE5C2 else 0x18006B50), rounded(color, radius), rounded(Color.WHITE, radius)
    )
    private fun label(value: String, size: Float = 15f, color: Int = ink, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private fun vertical() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun horizontal() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    private fun action(value: String, onClick: () -> Unit) = label(value, 14f, accent, true).apply {
        gravity = Gravity.CENTER; minHeight = dp(48); setPadding(dp(14), 0, dp(14), 0)
        background = touchSurface(accentBg, 24); isClickable = true; isFocusable = true
        setOnClickListener { onClick() }
    }
    private fun iconButton(icon: Int, description: String, click: () -> Unit) = ImageButton(this).apply {
        setImageResource(icon); imageTintList = ColorStateList.valueOf(ink)
        contentDescription = description; background = touchSurface(surface, 24); setPadding(dp(12), dp(12), dp(12), dp(12))
        layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)); setOnClickListener { click() }
    }
    private fun buildRoot() {
        generation++
        root = vertical().apply { setBackgroundColor(bg) }
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left + dp(20), bars.top + dp(16), bars.right + dp(20), bars.bottom + dp(12))
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        val appearance = getSharedPreferences("appearance", MODE_PRIVATE).getInt("mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        if (AppCompatDelegate.getDefaultNightMode() != appearance) AppCompatDelegate.setDefaultNightMode(appearance)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
        }
        query = savedInstanceState?.getString("query").orEmpty()
        protectedOnly = savedInstanceState?.getBoolean("protectedOnly") ?: false
        if (savedInstanceState?.getBoolean("settings") == true) showSettings()
        else if (savedInstanceState?.getBoolean("activity") == true) showActivity() else showApps()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (showingActivity) showApps() else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })
        GuardService.start(this)
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("query", query); outState.putBoolean("protectedOnly", protectedOnly)
        outState.putBoolean("activity", showingActivity); super.onSaveInstanceState(outState)
        outState.putBoolean("settings", settingsPage)
    }
    override fun onResume() { super.onResume(); if (settingsPage) showSettings(); handler.post(refresh) }
    override fun onPause() { handler.removeCallbacks(refresh); super.onPause() }
    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }

    private fun showApps() {
        showingActivity = false; settingsPage = false; rows.clear(); buildRoot()
        val toolbar = horizontal()
        toolbar.addView(label("Accessibility Keeper", 23f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        moreButton = iconButton(R.drawable.ic_more, "More options") { showOptions() }
        toolbar.addView(moreButton)
        root.addView(toolbar); root.addView(space(20))
        val card = horizontal().apply { background = rounded(accentBg, 22); setPadding(dp(18), dp(18), dp(10), dp(18)) }
        val words = vertical()
        title = label("You're protected", 19f, accent, true)
        summary = label("Checking your apps…", 13f, accent)
        words.addView(title); words.addView(space(5)); words.addView(summary)
        card.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
        pause = action("Pause") { togglePause() }; card.addView(pause)
        root.addView(card); root.addView(space(14))
        root.addView(label("Choose the apps to keep running.", 14f, muted))
        root.addView(space(18))
        val searchBox = horizontal().apply { background = rounded(surface, 16); setPadding(dp(14), 0, dp(10), 0) }
        searchBox.addView(ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search); imageTintList = ColorStateList.valueOf(muted)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(22), dp(22)))
        val search = EditText(this).apply {
            hint = "Find an app"; setHintTextColor(muted); setTextColor(ink); textSize = 16f
            background = null; setSingleLine(); minHeight = dp(54); setPadding(dp(10), 0, dp(8), 0)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            setText(query); contentDescription = "Search accessibility apps"
            doAfterTextChanged { query = it?.toString().orEmpty(); filterRows() }
        }
        searchBox.addView(search, LinearLayout.LayoutParams(0, -2, 1f)); root.addView(searchBox)
        root.addView(space(14))
        val filters = horizontal()
        allChip = action("All apps") { protectedOnly = false; filterRows() }
        protectedChip = action("Protected") { protectedOnly = true; filterRows() }
        filters.addView(allChip); filters.addView(space(1), LinearLayout.LayoutParams(dp(8), 1)); filters.addView(protectedChip)
        count = label("", 12f, muted).apply { gravity = Gravity.END }
        filters.addView(count, LinearLayout.LayoutParams(0, -2, 1f)); root.addView(filters)
        root.addView(space(12))
        list = vertical()
        empty = label("Loading your accessibility apps…", 15f, muted).apply {
            gravity = Gravity.CENTER; setPadding(dp(12), dp(36), dp(12), dp(36))
        }
        val scrollBody = vertical().apply { addView(list); addView(empty) }
        root.addView(ScrollView(this).apply { isFillViewport = false; addView(scrollBody); clipToPadding = false }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(space(10))
        root.addView(action("Recent activity") { showActivity() }, LinearLayout.LayoutParams(-1, dp(48)))
        populateRows(); render()
    }
    private fun populateRows() {
        if (showingActivity) return
        list.removeAllViews(); rows.clear()
        catalog.forEach { target ->
            val row = horizontal().apply {
                background = rounded(surface, 18); minimumHeight = dp(82); setPadding(dp(14), dp(14), dp(12), dp(14))
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
            }
            val icon = ImageView(this).apply {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageDrawable(runCatching {
                    packageManager.getServiceInfo(ComponentName.unflattenFromString(target.component)!!, 0).loadIcon(packageManager)
                }.getOrNull())
            }
            row.addView(icon, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(14) })
            val texts = vertical()
            texts.addView(label(target.name, 16f, ink, true).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END })
            texts.addView(space(5))
            val state = label("Not protected", 12f, muted); texts.addView(state)
            row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) })
            val toggle = SwitchCompat(this).apply {
                minWidth = dp(52); minimumHeight = dp(48); showText = false
                contentDescription = "Protect ${target.name}"
                thumbTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(accent, muted))
                trackTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(accent, muted))
                isChecked = Targets.protected(this@MainActivity, target)
                setOnCheckedChangeListener { _, enabled ->
                    Targets.setProtected(this@MainActivity, target, enabled)
                    if (enabled) RecoveryStatus.setNeedsOpen(this@MainActivity, target, false)
                    GuardApp.eventLog(this@MainActivity).append(System.currentTimeMillis(), "[user] ${target.name}: protection ${if (enabled) "on" else "off"}")
                    state.text = if (enabled) "Protection on · checking…" else "Not protected"
                    state.setTextColor(if (enabled) accent else muted)
                    updateSummary(); filterRows(); GuardService.start(this@MainActivity, manual = enabled); render()
                }
            }
            row.addView(toggle)
            row.setOnClickListener { showAppOptions(target) }
            row.contentDescription = "Options for ${target.name}"
            row.setOnLongClickListener { showAppOptions(target); true }
            rows[target.component] = Row(row, state, toggle); list.addView(row)
        }
        filterRows(); updateSummary()
    }
    private fun updateSummary() {
        if (showingActivity) return
        val selected = catalog.count { Targets.protected(this, it) }
        val paused = GuardApp.pauseState(this).isPaused()
        val ready = checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        title.text = when { !ready -> "One-time setup needed"; paused -> "Protection paused"; selected == 0 -> "Choose your apps"; else -> "Protection is on" }
        summary.text = when {
            !ready -> "Connect to a computer to finish setup"
            paused -> "Resumes automatically after 30 minutes"
            selected == 1 -> "1 app protected"
            else -> "$selected apps protected"
        }
        pause.text = if (!ready) "Set up" else if (paused) "Resume" else "Pause"
        title.setTextColor(if (paused || !ready) warning else accent)
        protectedChip.text = "Protected · $selected"
    }
    private fun filterRows() {
        if (showingActivity || !::allChip.isInitialized) return
        var visible = 0
        catalog.forEach { target ->
            val show = (!protectedOnly || Targets.protected(this, target)) && target.name.contains(query.trim(), ignoreCase = true)
            rows[target.component]?.view?.visibility = if (show) View.VISIBLE else View.GONE
            if (show) visible++
        }
        count.text = visible.toString()
        allChip.background = rounded(if (!protectedOnly) accentBg else surface, 24)
        allChip.setTextColor(if (!protectedOnly) accent else muted)
        protectedChip.background = rounded(if (protectedOnly) accentBg else surface, 24)
        protectedChip.setTextColor(if (protectedOnly) accent else muted)
        empty.visibility = if (visible == 0) View.VISIBLE else View.GONE
        empty.text = when {
            query.isNotBlank() -> "No apps match your search"
            protectedOnly -> "No protected apps yet. Choose an app from All apps."
            else -> "No accessibility apps found. Installed apps with an accessibility service appear here."
        }
    }
    private fun togglePause() {
        if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) { showSetup(); return }
        val state = GuardApp.pauseState(this)
        if (state.isPaused()) state.resume() else state.pauseFor(DEFAULT_PAUSE_MS)
        updateSummary(); GuardService.start(this)
    }
    private fun showOptions() {
        val menu = vertical().apply {
            background = rounded(surface, 22); setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val popup = PopupWindow(menu, minOf(dp(320), resources.displayMetrics.widthPixels - dp(40)), -2, true).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)); elevation = dp(16).toFloat()
            isOutsideTouchable = true
            enterTransition = android.transition.Fade().apply { duration = 120 }
            exitTransition = android.transition.Fade().apply { duration = 90 }
        }
        fun item(icon: Int, name: String, detail: String, click: () -> Unit) {
            menu.addView(optionRow(icon, name, detail, false) { popup.dismiss(); click() })
        }
        item(R.drawable.ic_check, "Check now", "Recover protected apps") {
            GuardService.start(this, manual = true); Toast.makeText(this, "Checking protected apps", Toast.LENGTH_SHORT).show()
        }
        item(R.drawable.ic_settings, "Settings", "Setup, appearance and help") { showSettings() }
        item(R.drawable.ic_accessibility, "Accessibility settings", "Open your phone's controls") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        popup.showAsDropDown(moreButton, 0, dp(8), Gravity.END)
    }
    private fun optionRow(icon: Int, name: String, detail: String, arrow: Boolean = true, click: () -> Unit): View {
        val row = horizontal().apply {
            minimumHeight = dp(76); setPadding(dp(14), dp(14), dp(14), dp(14))
            background = touchSurface(surface, 18); isClickable = true; isFocusable = true
            setOnClickListener { click() }
            contentDescription = "$name. $detail"
        }
        row.addView(ImageView(this).apply {
            setImageResource(icon); imageTintList = ColorStateList.valueOf(accent)
            setPadding(dp(9), dp(9), dp(9), dp(9)); background = rounded(accentBg, 14)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(14) })
        val words = vertical()
        words.addView(label(name, 15f, ink, true))
        words.addView(space(4)); words.addView(label(detail, 12f, muted))
        row.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
        if (arrow) row.addView(label("›", 24f, muted).apply { setPadding(dp(12), 0, 0, 0) })
        return row
    }
    private fun sheet(name: String, description: String, actions: List<Pair<String, () -> Unit>> = emptyList()) {
        val dialog = Dialog(this)
        val content = vertical().apply {
            background = rounded(surface, 28); setPadding(dp(24), dp(14), dp(24), dp(24))
        }
        content.addView(View(this).apply { background = rounded(muted, 3) }, LinearLayout.LayoutParams(dp(36), dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(22)
        })
        content.addView(label(name, 23f, ink, true)); content.addView(space(14))
        content.addView(label(description, 15f, muted).apply { setLineSpacing(dp(3).toFloat(), 1f) })
        content.addView(space(22))
        actions.forEach { (text, click) ->
            content.addView(action(text) { dialog.dismiss(); click() }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
        content.addView(action("Done") { dialog.dismiss() }, LinearLayout.LayoutParams(-1, -2))
        dialog.setContentView(ScrollView(this).apply { addView(content); background = rounded(surface, 28) })
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)); addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.5f); setGravity(Gravity.BOTTOM)
        }
        dialog.show()
        dialog.window?.setLayout(resources.displayMetrics.widthPixels - dp(24), -2)
    }
    private fun howItWorks() = sheet("You're in control", "Turn a switch on to keep that app's accessibility enabled. If it stops, Keeper tries to bring it back.\n\nTurn the switch off to stop automatic recovery. The app's current accessibility setting stays unchanged.\n\nPause stops recovery for all apps for 30 minutes. Long-press an app for more options.")
    private fun showSettings() {
        showingActivity = true; settingsPage = true; buildRoot()
        val toolbar = horizontal()
        toolbar.addView(iconButton(R.drawable.ic_back, "Back to apps") { showApps() })
        toolbar.addView(label("Settings", 25f, ink, true).apply { setPadding(dp(14), 0, 0, 0) })
        root.addView(toolbar); root.addView(space(20))
        val body = vertical()
        fun section(name: String) {
            body.addView(label(name, 12f, muted, true).apply { setPadding(dp(4), dp(20), 0, dp(12)) })
        }
        fun item(icon: Int, name: String, detail: String, click: () -> Unit) {
            body.addView(optionRow(icon, name, detail, true, click), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
        section("PROTECTION")
        val ready = checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        item(R.drawable.ic_check, "Recovery setup", if (ready) "Ready to protect your apps" else "One-time setup needed") { showSetup() }
        item(R.drawable.ic_power, "Background autostart", "Let Keeper start after a reboot") {
            sheet("Keep protection available", "Allow Background autostart for Accessibility Keeper in your phone's settings.\n\nAlso keep its battery setting unrestricted. If you force-stop Keeper, open it again to resume protection.", listOf("Open app settings" to {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }))
        }
        section("PREFERENCES")
        val mode = getSharedPreferences("appearance", MODE_PRIVATE).getInt("mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        item(R.drawable.ic_appearance, "Appearance", when (mode) {
            AppCompatDelegate.MODE_NIGHT_YES -> "Dark"; AppCompatDelegate.MODE_NIGHT_NO -> "Light"; else -> "Follow phone settings"
        }) { chooseAppearance() }
        val notifications = getSystemService(android.app.NotificationManager::class.java).areNotificationsEnabled()
        item(R.drawable.ic_bell, "Notifications", if (notifications) "Recovery updates are on" else "Recovery updates are off") {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
        section("TOOLS & HELP")
        item(R.drawable.ic_accessibility, "Accessibility settings", "Manage your phone's accessibility services") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        item(R.drawable.ic_help, "How protection works", "A quick guide to switches and recovery") { howItWorks() }
        item(R.drawable.ic_info, "About Keeper", "Version 0.2.0") {
            sheet("Accessibility Keeper", "Version 0.2.0\n\nKeeps the accessibility apps you choose running.\n\nBased on HyperOS Accessibility Guard by haku4130. Licensed under MIT.")
        }
        body.addView(space(12))
        root.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))
    }
    private fun chooseAppearance() {
        val choices = listOf("Follow phone settings" to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            "Light" to AppCompatDelegate.MODE_NIGHT_NO, "Dark" to AppCompatDelegate.MODE_NIGHT_YES)
        sheet("Appearance", "Choose what feels comfortable. Your choice is saved on this phone.", choices.map { (name, mode) ->
            name to {
                getSharedPreferences("appearance", MODE_PRIVATE).edit().putInt("mode", mode).apply()
                AppCompatDelegate.setDefaultNightMode(mode)
                if (settingsPage) showSettings()
            }
        })
    }
    private fun showSetup() {
        val ready = checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        val dump = checkSelfPermission(Manifest.permission.DUMP) == PackageManager.PERMISSION_GRANTED
        sheet(if (ready) "Recovery is ready" else "Finish setup",
            if (ready) "Recovery permission is granted.\nService checks: ${if (dump) "ready" else "permission needed"}.\n\nKeep Background autostart enabled for Accessibility Keeper in your phone's settings."
            else "Connect your phone to a computer for the one-time permission setup. Then select the apps you want to protect.\n\nSetup command:\nadb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS")
    }
    private fun showAppOptions(target: GuardTarget) {
        val detail = when {
            RecoveryStatus.needsOpen(this, target) -> "Accessibility is enabled, but this app hasn't finished connecting. Open the app to help it finish starting."
            Targets.protected(this, target) -> "Protection is on. Open the app to use it or check its settings."
            else -> "Protection is off. Use the switch in the app list to turn it on."
        }
        sheet(target.name, detail, listOf(
            "Open app" to { openTarget(target) },
            "Accessibility settings" to { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        ))
    }
    private fun openTarget(target: GuardTarget) {
        val launch = packageManager.getLaunchIntentForPackage(target.packageName)
        if (launch == null) Toast.makeText(this, "This app has no launch screen", Toast.LENGTH_SHORT).show()
        else runCatching { startActivity(launch) }.onFailure {
            Toast.makeText(this, "Couldn't open the app. Try opening it from your home screen.", Toast.LENGTH_LONG).show()
        }
    }
    private fun showActivity() {
        showingActivity = true; settingsPage = false; buildRoot()
        val toolbar = horizontal()
        toolbar.addView(iconButton(R.drawable.ic_back, "Back to apps") { showApps() })
        toolbar.addView(label("Recent activity", 23f, ink, true).apply { setPadding(dp(12), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        toolbar.addView(iconButton(R.drawable.ic_share, "Share activity log") {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, GuardApp.eventLog(this@MainActivity).asText())
            }, "Share activity log"))
        })
        root.addView(toolbar); root.addView(space(20))
        val entries = GuardApp.eventLog(this).read().filterNot { it.message.contains("recovery started (") }.takeLast(60).asReversed()
        val knownApps = Targets.installed(this)
        val body = vertical()
        if (entries.isEmpty()) body.addView(label("All quiet. Recoveries and changes will appear here.", 15f, muted))
        val time = SimpleDateFormat("EEE, d MMM · HH:mm", Locale.getDefault())
        entries.forEach { entry ->
            val item = vertical().apply {
                background = rounded(surface, 18); setPadding(dp(18), dp(16), dp(18), dp(16))
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
            }
            val message = entry.message.replace(Regex("^\\[[^]]+]\\s*"), "")
                .replace("recovered and bound", "Accessibility restored")
            item.addView(label(message, 15f)); item.addView(space(8)); item.addView(label(time.format(Date(entry.timestampMs)), 12f, muted))
            if (entry.message.contains("open the app", ignoreCase = true)) {
                val target = entry.component?.let { component -> knownApps.firstOrNull { canonicalService(it.component) == canonicalService(component) } }
                    ?: knownApps.filter { message.startsWith("${it.name}:") }.singleOrNull()
                if (target != null) {
                    item.addView(space(12)); item.addView(label("Open app  ↗", 14f, accent, true))
                    item.background = touchSurface(surface, 18)
                    item.contentDescription = "${target.name}. Open app to finish connecting"
                    item.setOnClickListener { openTarget(target) }
                }
            }
            body.addView(item)
        }
        root.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))
    }
    private fun render() {
        if (busy || isFinishing || isDestroyed || showingActivity) return
        updateSummary(); busy = true
        val startedGeneration = generation
        executor.execute {
            try {
                val discovered = Targets.installed(this)
                val settings = AndroidSecureSettings(contentResolver)
                val listed = serviceList(settings.getString(SecureKeys.ENABLED_SERVICES)).map(::canonicalService)
                val master = settings.getInt(SecureKeys.ACCESSIBILITY_ENABLED, 0) == 1
                val dump = if (discovered.any { Targets.protected(this, it) }) TargetRuntime.read() else null
                val states = discovered.associate { target ->
                    val runtime = TargetRuntime.parse(dump, target.component, Targets.runtimeLabel(discovered, target))
                    val status = when {
                        !master || canonicalService(target.component) !in listed -> "Waiting for accessibility"
                        runtime?.bound == true && !runtime.crashed -> "Protected · running"
                        RecoveryStatus.needsOpen(this, target) -> "Open app to finish connecting"
                        runtime == null -> "Protected · enabled"
                        runtime.crashed -> "Recovery needed"
                        else -> "Connecting…"
                    }
                    target.component to status
                }
                handler.post {
                    if (!isDestroyed && !showingActivity && generation == startedGeneration) {
                        if (catalog != discovered) { catalog = discovered; populateRows() }
                        discovered.forEach { target ->
                            val selected = Targets.protected(this, target)
                            rows[target.component]?.let { row ->
                                row.status.text = if (selected) states[target.component] else "Not protected"
                                row.status.setTextColor(if (!selected) muted else if (states[target.component]?.startsWith("Protected") == true) accent else warning)
                            }
                        }
                        updateSummary(); filterRows()
                    }
                    busy = false
                }
            } catch (_: Exception) {
                handler.post {
                    if (!isDestroyed && !showingActivity && generation == startedGeneration) {
                        empty.text = "Couldn't load apps. Reopen the app to try again."
                        empty.visibility = if (catalog.isEmpty()) View.VISIBLE else View.GONE
                    }
                    busy = false
                }
            }
        }
    }
}
