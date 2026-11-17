package io.github.haku4130.noscrollguard.repair

import io.github.haku4130.noscrollguard.settings.SecureKeys
import io.github.haku4130.noscrollguard.settings.SecureSettings

/** Android accepts both full and package-relative component names. */
fun canonicalService(value: String): String {
    val parts = value.trim().split('/', limit = 2)
    if (parts.size != 2) return value.trim()
    return parts[0] + "/" + if (parts[1].startsWith('.')) parts[0] + parts[1] else parts[1]
}

fun serviceList(raw: String?): List<String> = raw.orEmpty().split(':')
    .map(String::trim).filter { it.isNotEmpty() && it != "null" }.distinctBy(::canonicalService)

/** Rebind one selected service, without clearing the list or disabling the master switch. */
class TargetRepairer(
    private val settings: SecureSettings,
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) }
) {
    fun repair(component: String): RepairResult {
        val target = canonicalService(component)
        val before = serviceList(settings.getString(SecureKeys.ENABLED_SERVICES))
        val listed = before.any { canonicalService(it) == target }
        if (listed && !settings.putString(SecureKeys.ENABLED_SERVICES,
                before.filterNot { canonicalService(it) == target }.joinToString(":"))) {
            return RepairResult.NoPermission
        }
        var restored = false
        try {
            if (listed) sleeper(REBIND_PAUSE_MS)
        } finally {
            // Read again: another app/user may have changed unrelated services during the pause.
            val latest = serviceList(settings.getString(SecureKeys.ENABLED_SERVICES))
            val merged = (latest + component).distinctBy(::canonicalService).joinToString(":")
            restored = settings.putString(SecureKeys.ENABLED_SERVICES, merged)
            if (restored) restored = settings.putInt(SecureKeys.ACCESSIBILITY_ENABLED, 1)
        }
        if (!restored) return RepairResult.Failed("could not restore service or master setting")
        val verified = settings.getInt(SecureKeys.ACCESSIBILITY_ENABLED, 0) == 1 &&
            serviceList(settings.getString(SecureKeys.ENABLED_SERVICES)).any { canonicalService(it) == target }
        return if (verified) RepairResult.Success else RepairResult.Failed("settings read-back failed")
    }
}
