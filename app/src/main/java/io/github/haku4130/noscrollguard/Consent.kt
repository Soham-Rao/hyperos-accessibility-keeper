package io.github.haku4130.noscrollguard

import android.content.Context

object Consent {
    const val REVISION = 1
    fun accepted(context: Context) = context.getSharedPreferences("consent", Context.MODE_PRIVATE)
        .getInt("revision", 0) == REVISION
    fun accept(context: Context) = context.getSharedPreferences("consent", Context.MODE_PRIVATE)
        .edit().putInt("revision", REVISION).apply()
}
