package com.example.fruitshop.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fruitshop.databinding.ActivityAdminDashboardBinding

class AdminDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requireRole("Admin")) return
        val binding = ActivityAdminDashboardBinding.inflate(layoutInflater); setContentView(binding.root)
        binding.fruitsButton.setOnClickListener { startActivity(Intent(this, FruitListActivity::class.java)) }
        binding.ordersButton.setOnClickListener { startActivity(Intent(this, OrderManagementActivity::class.java)) }
        binding.logoutButton.setOnClickListener { logout() }
    }
}

