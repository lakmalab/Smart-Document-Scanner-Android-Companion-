package com.projectinsight.smartdocumentscanner.util

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {
    private const val PREFS_NAME = "app_prefs"
    private const val TOKEN_KEY = "token"
    private const val URL_KEY = "url"
    private const val USER_NAME = "user"
    private const val USER_ID = "userID"
    private const val JWTTOKEN_KEY = "jwttoken"
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveJwtToken(context: Context, token: String) {
        getSharedPreferences(context).edit().putString(JWTTOKEN_KEY, token).apply()
    }
    fun getJwtToken(context: Context): String? {
        return getSharedPreferences(context).getString(JWTTOKEN_KEY, null)
    }
    fun saveToken(context: Context, token: String?) {
        getSharedPreferences(context).edit().putString(TOKEN_KEY, token).apply()
    }

    fun saveUserName(context: Context, token: String) {
        getSharedPreferences(context).edit().putString(USER_NAME, token).apply()
    }
    fun saveUserID(context: Context, token: Long) {
        getSharedPreferences(context).edit().putInt(USER_ID, token.toInt()).apply()
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
    fun setSelectedTemplateId(context: Context, id: Int) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("selected_template_id", id).apply()
    }

    fun getSelectedTemplateId(context: Context): Int {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("selected_template_id", -1)
    }

}