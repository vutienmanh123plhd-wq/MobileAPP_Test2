package com.example.fruitshop.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.fruitshop.database.DatabaseHelper
import com.example.fruitshop.databinding.ActivityOrderDetailBinding
import com.example.fruitshop.utils.SessionManager

class OrderDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrderDetailBinding
    private lateinit var db: DatabaseHelper
    private var orderId = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val role = SessionManager(this).role
        if (role !in setOf("Admin", "Customer") || !requireRole(role)) return
        binding = ActivityOrderDetailBinding.inflate(layoutInflater); setContentView(binding.root)
        db = DatabaseHelper(this)
        orderId = intent.getLongExtra("orderId", 0)
        binding.statusButton.visibility = if (role == "Admin") View.VISIBLE else View.GONE
        binding.statusButton.setOnClickListener {
            if (!requireRole("Admin")) return@setOnClickListener
            val statuses = arrayOf("Pending", "Confirmed", "Completed")
            AlertDialog.Builder(this).setTitle("Update order status").setItems(statuses) { _, which ->
                if (db.updateOrderStatus(orderId, statuses[which])) { notice("Status updated"); showOrder() } else notice("Order not found")
            }.show()
        }
    }
    override fun onResume() { super.onResume(); if (::db.isInitialized) showOrder() }
    private fun showOrder() {
        val order = db.getOrderById(orderId) ?: run { notice("Order not found"); finish(); return }
        val role = SessionManager(this).role
        if (role == "Customer") {
            val customer = db.getCustomerByAccountId(SessionManager(this).userId)
            if (order.customerId != customer?.id) { notice("Order unavailable"); finish(); return }
        } else if (role != "Admin") { finish(); return }
        val customerName = if (role == "Admin") db.getCustomerById(order.customerId)?.fullname ?: "Unknown customer" else "Your order"
        binding.detailsText.text = "Order #${order.id}\n$customerName\nDate: ${order.orderDate}\nTotal: ${money(order.totalAmount)}\nStatus: ${order.status}"
    }
    override fun onDestroy() { if (::db.isInitialized) db.close(); super.onDestroy() }
}

