package com.example.fruitshop.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fruitshop.adapter.FruitAdapter
import com.example.fruitshop.database.DatabaseHelper
import com.example.fruitshop.databinding.ActivityFruitListBinding
import com.example.fruitshop.utils.SessionManager

class FruitListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFruitListBinding
    private lateinit var db: DatabaseHelper
    private lateinit var adapter: FruitAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val role = SessionManager(this).role
        if (role !in setOf("Admin", "Customer") || !requireRole(role)) return
        binding = ActivityFruitListBinding.inflate(layoutInflater); setContentView(binding.root)
        db = DatabaseHelper(this)
        binding.titleText.text = if (role == "Admin") "Manage fruits" else "Fresh fruits"
        binding.addButton.visibility = if (role == "Admin") View.VISIBLE else View.GONE
        binding.addButton.setOnClickListener { startActivity(Intent(this, AddEditFruitActivity::class.java)) }
        adapter = FruitAdapter { fruit -> startActivity(Intent(this, FruitDetailActivity::class.java).putExtra("fruitId", fruit.id)) }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refresh() }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    override fun onResume() { super.onResume(); if (::db.isInitialized) refresh() }
    private fun refresh() {
        val query = binding.searchInput.text.toString().trim()
        val fruits = if (query.isEmpty()) db.getAllFruits() else db.searchFruits(query)
        adapter.submit(fruits)
        binding.emptyText.visibility = if (fruits.isEmpty()) View.VISIBLE else View.GONE
    }
    override fun onDestroy() { if (::db.isInitialized) db.close(); super.onDestroy() }
}

