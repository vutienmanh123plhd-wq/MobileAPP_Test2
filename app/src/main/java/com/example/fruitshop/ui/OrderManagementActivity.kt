package com.example.fruitshop.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fruitshop.adapter.OrderAdapter
import com.example.fruitshop.database.DatabaseHelper
import com.example.fruitshop.databinding.ActivityOrderManagementBinding

class OrderManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrderManagementBinding
    private lateinit var db: DatabaseHelper
    private lateinit var adapter: OrderAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requireRole("Admin")) return
        binding = ActivityOrderManagementBinding.inflate(layoutInflater); setContentView(binding.root)
        db = DatabaseHelper(this)
        adapter = OrderAdapter { startActivity(Intent(this, OrderDetailActivity::class.java).putExtra("orderId", it.id)) }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    override fun onResume() {
        super.onResume()
        if (!::db.isInitialized) return
        val orders = db.getAllOrders()
        adapter.submit(orders)
        binding.emptyText.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
    }
    override fun onDestroy() { if (::db.isInitialized) db.close(); super.onDestroy() }
}

