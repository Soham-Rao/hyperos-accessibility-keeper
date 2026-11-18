package io.github.haku4130.noscrollguard.settings

import android.content.ContentResolver
import android.provider.Settings

class AndroidSecureSettings(private val resolver: ContentResolver) : SecureSettings {

    override fun getString(key: String): String? =
        Settings.Secure.getString(resolver, key)

    override fun getInt(key: String, default: Int): Int =
        Settings.Secure.getInt(resolver, key, default)

    override fun putString(key: String, value: String): Boolean = try {
        key == SecureKeys.ENABLED_SERVICES && Settings.Secure.putString(resolver, key, value)
    } catch (e: SecurityException) {
        false
    }

    override fun putInt(key: String, value: Int): Boolean = try {
        key == SecureKeys.ACCESSIBILITY_ENABLED && value == 1 && Settings.Secure.putInt(resolver, key, value)
    } catch (e: SecurityException) {
        false
    }
}
