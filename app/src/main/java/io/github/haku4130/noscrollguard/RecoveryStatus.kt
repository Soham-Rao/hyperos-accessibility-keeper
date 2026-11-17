package io.github.haku4130.noscrollguard

import android.content.Context
import io.github.haku4130.noscrollguard.repair.canonicalService

object RecoveryStatus {
    fun needsOpen(context: Context, target: GuardTarget): Boolean =
        context.getSharedPreferences("recovery", Context.MODE_PRIVATE)
            .getBoolean(canonicalService(target.component), false)

    fun setNeedsOpen(context: Context, target: GuardTarget, needed: Boolean) {
        context.getSharedPreferences("recovery", Context.MODE_PRIVATE).edit()
            .putBoolean(canonicalService(target.component), needed).apply()
    }
}
