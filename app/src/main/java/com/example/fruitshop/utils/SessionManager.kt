package com.example.fruitshop.utils

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("fruit_shop_session", Context.MODE_PRIVATE)
    val userId: Long get() = prefs.getLong("userId", -1)
    val username: String get() = prefs.getString("username", "") ?: ""
    val role: String get() = prefs.getString("role", "") ?: ""
    fun save(id: Long, username: String, role: String) = prefs.edit().putLong("userId", id).putString("username", username).putString("role", role).apply()
    fun clear() = prefs.edit().clear().apply()
}

