package com.transitops.driver.data.auth

import android.content.Context
import android.content.SharedPreferences

object TokenProvider {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    }

    var token: String?
        get() = prefs?.getString("jwt_token", null)
        set(value) {
            prefs?.edit()?.putString("jwt_token", value)?.apply()
        }
}
