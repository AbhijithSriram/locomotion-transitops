package com.transitops.driver.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Singleton that persists JWT tokens and driver identity to EncryptedSharedPreferences.
 * Must be initialised once in MainActivity before use.
 */
object TokenProvider {
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            "transitops_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var token: String?
        get() = prefs?.getString("jwt_access_token", null)
        set(value) { prefs?.edit()?.putString("jwt_access_token", value)?.apply() }

    var refreshToken: String?
        get() = prefs?.getString("jwt_refresh_token", null)
        set(value) { prefs?.edit()?.putString("jwt_refresh_token", value)?.apply() }

    var driverId: String?
        get() = prefs?.getString("driver_id", null)
        set(value) { prefs?.edit()?.putString("driver_id", value)?.apply() }

    var driverName: String?
        get() = prefs?.getString("driver_name", null)
        set(value) { prefs?.edit()?.putString("driver_name", value)?.apply() }

    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }

    val isLoggedIn: Boolean
        get() = token != null
}
