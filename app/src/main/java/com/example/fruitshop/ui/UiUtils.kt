package com.example.fruitshop.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.example.fruitshop.utils.SessionManager
import java.util.Locale

internal fun Activity.requireRole(role: String): Boolean {
    val session = SessionManager(this)
    if (session.role == role && session.userId > 0) return true
    startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    finish()
    return false
}
internal fun Activity.logout() {
    SessionManager(this).clear()
    startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    finish()
}
internal fun Activity.notice(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
internal fun money(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)

