package com.simats.pcos

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREF_NAME = "pcos_user_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_ROLE = "user_role" // "PATIENT", "DOCTOR", "ADMIN"
    private const val KEY_USERNAME = "user_name"

    const val ROLE_PATIENT = "PATIENT"
    const val ROLE_DOCTOR = "DOCTOR"
    const val ROLE_ADMIN = "ADMIN"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Saves user session when login succeeds.
     */
    fun saveSession(context: Context, role: String, username: String) {
        val editor = getPrefs(context).edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_ROLE, role)
        editor.putString(KEY_USERNAME, username)
        editor.apply()
    }

    /**
     * Checks if a user is currently logged in.
     */
    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Returns the role of the logged in user ("PATIENT", "DOCTOR", or "ADMIN").
     */
    fun getRole(context: Context): String? {
        return getPrefs(context).getString(KEY_ROLE, null)
    }

    /**
     * Returns the username / ID of the logged in user.
     */
    fun getUsername(context: Context): String {
        return getPrefs(context).getString(KEY_USERNAME, "") ?: ""
    }

    /**
     * Clears user session on logout.
     */
    fun clearSession(context: Context) {
        val editor = getPrefs(context).edit()
        editor.clear()
        editor.apply()
    }
}
