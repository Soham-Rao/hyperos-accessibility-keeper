package io.github.haku4130.noscrollguard

import org.junit.Assert.*
import org.junit.Test

class DownloadedAppsPolicyTest {
    @Test fun `any installed third party service can appear without changing a hardcoded app list`() {
        assertTrue(DownloadedAppsPolicy.includes("new.app/.NewService", emptySet(), true))
    }
    @Test fun `categorized service is excluded using canonical identity`() {
        assertFalse(DownloadedAppsPolicy.includes("reader.app/.Reader", setOf("reader.app/reader.app.Reader"), false))
    }
    @Test fun `HyperOS system categories are excluded without excluding all system packages`() {
        assertFalse(DownloadedAppsPolicy.includes("com.miui.securitycenter/com.miui.gamebooster.gbservices.AntiMsgAccessibilityService", emptySet(), true))
        assertFalse(DownloadedAppsPolicy.includes("com.android.systemui.accessibility.accessibilitymenu/.AccessibilityMenuService", emptySet(), true))
        assertTrue(DownloadedAppsPolicy.includes("com.miui.screenshot/.accessibility.ScreenshotAccessibilityService", emptySet(), true))
        assertTrue(DownloadedAppsPolicy.includes("com.milink.service/com.miui.circulate.world.MLAccessibilityService", emptySet(), true))
    }
    @Test fun `HyperOS exclusions do not apply to other manufacturers`() {
        assertTrue(DownloadedAppsPolicy.includes("com.android.systemui.accessibility.accessibilitymenu/.AccessibilityMenuService", emptySet(), false))
    }
    @Test fun `separate services in one package are not merged`() {
        val category = setOf("example.app/example.app.First")
        assertFalse(DownloadedAppsPolicy.includes("example.app/.First", category, true))
        assertTrue(DownloadedAppsPolicy.includes("example.app/.Second", category, true))
    }
}
