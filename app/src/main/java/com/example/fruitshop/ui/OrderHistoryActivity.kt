package com.example.fruitshop.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fruitshop.adapter.OrderAdapter
import com.example.fruitshop.database.DatabaseHelper
import com.example.fruitshop.databinding.ActivityOrderHistoryBinding
import com.example.fruitshop.utils.SessionManager

class OrderHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrderHistoryBinding
    private lateinit var db: DatabaseHelper
    private lateinit var adapter: OrderAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requireRole("Customer")) return
        binding = ActivityOrderHistoryBinding.inflate(layoutInflater); setContentView(binding.root)
        db = DatabaseHelper(this)
        adapter = OrderAdapter { startActivity(Intent(this, OrderDetailActivity::class.java).putExtra("orderId", it.id)) }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    override fun onResume() {
        super.onResume()
        if (!::db.isInitialized) return
        val customer = db.getCustomerByAccountId(SessionManager(this).userId)
        val orders = customer?.let { db.getOrdersByCustomer(it.id) } ?: emptyList()
        adapter.submit(orders)
        binding.emptyText.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
    }
    override fun onDestroy() { if (::db.isInitialized) db.close(); super.onDestroy() }
}

