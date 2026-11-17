package io.github.haku4130.noscrollguard

import android.content.Context
import android.content.ComponentName
import android.os.Build
import android.view.accessibility.AccessibilityManager
import io.github.haku4130.noscrollguard.repair.canonicalService
import java.text.Collator

/** One accessibility service, rather than one package (an app can expose several). */
data class GuardTarget(val name: String, val component: String) {
    val packageName: String get() = component.substringBefore('/')
}

object Targets {
    fun installed(context: Context): List<GuardTarget> {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        val categorized = preinstalledCategories(context)
        val hyperOs = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
        val collator = Collator.getInstance()
        return manager.installedAccessibilityServiceList.orEmpty().mapNotNull { info ->
            val resolve = info.resolveInfo ?: return@mapNotNull null
            val service = resolve.serviceInfo ?: return@mapNotNull null
            val component = ComponentName(service.packageName, service.name).flattenToString()
            if (!DownloadedAppsPolicy.includes(component, categorized, hyperOs)) return@mapNotNull null
            GuardTarget(resolve.loadLabel(context.packageManager)?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() } ?: service.packageName, canonicalService(component))
        }.distinctBy { it.component }.sortedWith { a, b ->
            val byName = collator.compare(a.name, b.name)
            if (byName == 0) a.component.compareTo(b.component) else byName
        }
    }

    /** Settings puts these services in built-in categories instead of Downloaded apps.
     * Read the phone's own configuration, including OEM overlays, rather than guess
     * based on FLAG_SYSTEM (HyperOS lists some system apps in Downloaded apps).
     */
    private fun preinstalledCategories(context: Context): Set<String> = runCatching {
        val resources = context.packageManager.getResourcesForApplication("com.android.settings")
        val names = listOf("screen_reader", "audio_and_caption", "audio", "captions", "display", "interaction_control", "speech")
            .map { "config_preinstalled_${it}_services" } +
            if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)) listOf("config_downloaded_services") else emptyList()
        names.flatMap { name ->
                val id = resources.getIdentifier(name, "array", "com.android.settings")
                if (id == 0) emptyList() else resources.getStringArray(id).toList()
            }.map(::canonicalService).toSet()
    }.getOrDefault(emptySet())

    fun protected(context: Context, target: GuardTarget): Boolean =
        context.getSharedPreferences("targets", Context.MODE_PRIVATE)
            .getBoolean(canonicalService(target.component), false)

    fun setProtected(context: Context, target: GuardTarget, enabled: Boolean) {
        context.getSharedPreferences("targets", Context.MODE_PRIVATE).edit()
            .putBoolean(canonicalService(target.component), enabled).apply()
    }

    // Some HyperOS builds expose only labels in the bound-service dump. A duplicate
    // label cannot identify a specific service, so do not use it as proof of binding.
    fun runtimeLabel(catalog: List<GuardTarget>, target: GuardTarget): String? =
        target.name.takeIf { name -> catalog.count { it.name == name } == 1 }
}
