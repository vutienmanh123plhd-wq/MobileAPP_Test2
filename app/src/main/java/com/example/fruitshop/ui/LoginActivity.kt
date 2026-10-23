package com.example.fruitshop.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fruitshop.database.DatabaseHelper
import com.example.fruitshop.databinding.ActivityLoginBinding
import com.example.fruitshop.utils.SessionManager

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: DatabaseHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater); setContentView(binding.root)
        db = DatabaseHelper(this)
        val session = SessionManager(this)
        if (session.userId > 0 && session.role in setOf("Admin", "Customer")) {
            val account = db.getAccountByUsername(session.username)
            if (account?.id == session.userId && account.role == session.role) { navigate(session.role); return }
            session.clear()
        }
        binding.loginButton.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()
            if (username.isEmpty() || password.isEmpty()) { binding.errorText.text = "Enter username and password"; return@setOnClickListener }
            val account = db.validateLogin(username, password)
            if (account == null) { binding.errorText.text = "Invalid username or password"; return@setOnClickListener }
            session.save(account.id, account.username, account.role)
            navigate(account.role)
        }
    }
    private fun navigate(role: String) {
        startActivity(Intent(this, if (role == "Admin") AdminDashboardActivity::class.java else CustomerDashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
    override fun onDestroy() { db.close(); super.onDestroy() }
}

