package io.github.haku4130.noscrollguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import io.github.haku4130.noscrollguard.repair.canonicalService
import java.security.MessageDigest

/** Stores identities only for selected services. Never selects new service components. */
object SelectionStore {
    private fun prefs(context: Context) = context.getSharedPreferences("selection_identity", Context.MODE_PRIVATE)
    fun retain(context: Context) = prefs(context).getBoolean("retain_reinstall", false)
    @Synchronized fun setRetain(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("retain_reinstall", enabled).apply()
        reconcile(context)
    }
    private fun identity(context: Context, pkg: String): InstallIdentity? = runCatching {
        val info = context.packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        val signers = info.signingInfo?.apkContentsSigners.orEmpty()
        if (signers.isEmpty()) return@runCatching null
        val digest = MessageDigest.getInstance("SHA-256")
        val fingerprint = signers.map { signature -> digest.digest(signature.toByteArray()).joinToString("") { "%02x".format(it) } }.sorted().joinToString(":")
        InstallIdentity(info.firstInstallTime, fingerprint)
    }.getOrNull()
    @Synchronized fun selected(context: Context, target: GuardTarget): Boolean {
        val key = canonicalService(target.component)
        val selections = context.getSharedPreferences("targets", Context.MODE_PRIVATE)
        if (!selections.getBoolean(key, false)) return false
        val store = prefs(context)
        val previous = store.getString("$key.signer", null)?.let { InstallIdentity(store.getLong("$key.time", 0), it) }
        val current = identity(context, target.packageName)
        if (!keepSelection(previous, current, retain(context))) {
            set(context, target, false)
            return false
        }
        if (current != null && previous != current) store.edit()
            .putString("$key.signer", current.signer).putLong("$key.time", current.firstInstallTime).apply()
        return true
    }
    @Synchronized fun set(context: Context, target: GuardTarget, enabled: Boolean) {
        val key = canonicalService(target.component)
        val current = if (enabled) identity(context, target.packageName) else null
        context.getSharedPreferences("targets", Context.MODE_PRIVATE).edit().putBoolean(key, enabled && current != null).apply()
        val edit = prefs(context).edit()
        if (enabled && current != null) edit.putString("$key.signer", current.signer).putLong("$key.time", current.firstInstallTime)
        else edit.remove("$key.signer").remove("$key.time")
        edit.apply()
    }
    @Synchronized fun reconcile(context: Context) {
        context.getSharedPreferences("targets", Context.MODE_PRIVATE).all.filterValues { it == true }.keys.forEach { component ->
            val target = GuardTarget(component, component)
            // Retained records stay dormant while the package is absent.
            if (identity(context, target.packageName) == null && retain(context) &&
                prefs(context).contains("${canonicalService(component)}.signer")) return@forEach
            selected(context, target)
        }
    }
    @Synchronized fun removed(context: Context, pkg: String) {
        if (retain(context)) return
        context.getSharedPreferences("targets", Context.MODE_PRIVATE).all.keys.filter { it.substringBefore('/') == pkg }
            .forEach { set(context, GuardTarget(it, it), false) }
    }
}

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_REMOVED && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
            intent.data?.schemeSpecificPart?.let { SelectionStore.removed(context, it) }
        }
    }
}
