package io.github.haku4130.noscrollguard.ui

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import io.github.haku4130.noscrollguard.R

/** Small shared view style for the About and offline legal screens. */
internal class InfoUi(private val activity: AppCompatActivity) {
    private val dark = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val background = Color.parseColor(if (dark) "#0D1117" else "#F5F7FA")
    val surface = Color.parseColor(if (dark) "#171D25" else "#FFFFFF")
    val ink = Color.parseColor(if (dark) "#EDF3F9" else "#19242E")
    val muted = Color.parseColor(if (dark) "#A2AFBF" else "#5D6B7A")
    val accent = Color.parseColor(if (dark) "#8BE5C2" else "#006B50")
    private val accentBg = Color.parseColor(if (dark) "#19372F" else "#DCF5EA")
    fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()
    fun rounded(color: Int, radius: Int = 18) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    fun vertical() = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    fun text(value: String, size: Float = 15f, color: Int = ink, bold: Boolean = false) = TextView(activity).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    fun space(size: Int) = View(activity).apply { layoutParams = LinearLayout.LayoutParams(1, dp(size)) }
    fun screen(title: String): LinearLayout {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
        }
        val root = vertical().apply { setBackgroundColor(this@InfoUi.background) }
        activity.setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left + dp(20), bars.top + dp(16), bars.right + dp(20), bars.bottom + dp(12))
            insets
        }
        ViewCompat.requestApplyInsets(root)
        val toolbar = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
        toolbar.addView(ImageButton(activity).apply {
            setImageResource(R.drawable.ic_back); imageTintList = ColorStateList.valueOf(ink)
            setPadding(dp(12), dp(12), dp(12), dp(12)); background = rounded(surface, 24)
            contentDescription = "Back"; setOnClickListener { activity.finish() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        toolbar.addView(text(title, 24f, ink, true).apply { setPadding(dp(14), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(toolbar); root.addView(space(20))
        val body = vertical().apply { setPadding(0, 0, 0, dp(8)) }
        root.addView(ScrollView(activity).apply { addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))
        return body
    }
    fun section(parent: LinearLayout, title: String) {
        parent.addView(text(title, 12f, muted, true).apply { setPadding(dp(4), dp(20), 0, dp(12)) })
    }
    fun card(title: String, detail: String): View = vertical().apply {
        background = rounded(surface); setPadding(dp(18), dp(18), dp(18), dp(18))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
        addView(text(title, 17f, ink, true)); addView(space(9))
        addView(text(detail, 14f, muted).apply { setLineSpacing(dp(3).toFloat(), 1f) })
    }
    fun row(title: String, detail: String, icon: Int, action: () -> Unit): View = LinearLayout(activity).apply {
        gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(78)
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = RippleDrawable(ColorStateList.valueOf(0x248BE5C2), rounded(surface), rounded(Color.WHITE))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
        contentDescription = "$title. $detail"; isFocusable = true; setOnClickListener { action() }
        addView(ImageView(activity).apply {
            setImageResource(icon); imageTintList = ColorStateList.valueOf(accent)
            setPadding(dp(9), dp(9), dp(9), dp(9)); background = rounded(accentBg, 14)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(14) })
        addView(vertical().apply {
            addView(text(title, 15f, ink, true)); addView(space(4)); addView(text(detail, 12f, muted))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(text("›", 24f, muted).apply { setPadding(dp(12), 0, 0, 0) })
    }
}
