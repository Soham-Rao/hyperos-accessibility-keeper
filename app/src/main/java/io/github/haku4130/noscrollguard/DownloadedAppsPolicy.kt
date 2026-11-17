package io.github.haku4130.noscrollguard

import io.github.haku4130.noscrollguard.repair.canonicalService

/** HyperOS 3 Settings' built-in exclusions, verified against the POCO F6 Settings
 * APK's AccessibilitySettings / RestrictedPreferenceHelper on 2026-11-18.
 * These are system categories, never an allowlist of personally selected apps.
 */
object DownloadedAppsPolicy {
    private val hiddenPackages = setOf(
        "com.miui.securitycenter", "com.miui.personalassistant", "com.xiaomi.misettings",
        "com.miui.powerkeeper", "com.miui.newhome", "com.miui.handwriting", "com.miui.voiceassist",
        "com.bjbyhd.voiceback", "com.android.tback", "com.nirenr.talkman", "com.dianming.phoneapp"
    )
    private val hiddenClasses = setOf(
        "com.google.android.accessibility.accessibilitymenu.AccessibilityMenuService",
        "com.android.systemui.accessibility.accessibilitymenu.AccessibilityMenuService"
    )
    fun includes(component: String, categorized: Set<String>, hyperOs: Boolean): Boolean {
        val canonical = canonicalService(component)
        if (canonical in categorized) return false
        if (!hyperOs) return true
        return canonical.substringBefore('/') !in hiddenPackages && canonical.substringAfter('/') !in hiddenClasses
    }
}
