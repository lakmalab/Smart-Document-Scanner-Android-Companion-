package com.projectinsight.smartdocumentscanner.util

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {
    private const val PREFS_NAME = "app_prefs"
    private const val TOKEN_KEY = "token"
    private const val URL_KEY = "url"
    private const val USER_NAME = "user"
    private const val USER_ID = "userID"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(context: Context, token: String) {
        getSharedPreferences(context).edit().putString(TOKEN_KEY, token).apply()
    }
    fun saveUserName(context: Context, token: String) {
        getSharedPreferences(context).edit().putString(USER_NAME, token).apply()
    }
    fun saveUserID(context: Context, token: Int) {
        getSharedPreferences(context).edit().putInt(USER_ID, token).apply()
    }
    fun getUserName(context: Context): String? {
        return getSharedPreferences(context).getString(USER_NAME, null)
    }
    fun getUserID(context: Context): Int? {
        return getSharedPreferences(context).getInt(USER_ID, 0)
    }
    fun getToken(context: Context): String? {
        return getSharedPreferences(context).getString(TOKEN_KEY, null)
    }

    fun saveUrl(context: Context, url: String) {
        getSharedPreferences(context).edit().putString(URL_KEY, url).apply()
    }

    fun getUrl(context: Context): String? {
        return getSharedPreferences(context).getString(URL_KEY, null)
    }
}