package com.example.fruitshop.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.fruitshop.R
import com.example.fruitshop.model.Fruit
import com.example.fruitshop.database.DatabaseHelper
import com.example.fruitshop.databinding.ActivityFruitDetailBinding
import com.example.fruitshop.utils.SessionManager
import com.example.fruitshop.utils.ValidationUtils

class FruitDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFruitDetailBinding
    private lateinit var db: DatabaseHelper
    private var fruitId = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val role = SessionManager(this).role
        if (role !in setOf("Admin", "Customer") || !requireRole(role)) return
        binding = ActivityFruitDetailBinding.inflate(layoutInflater); setContentView(binding.root)
        db = DatabaseHelper(this)
        fruitId = intent.getLongExtra("fruitId", 0)
        val admin = role == "Admin"
        binding.quantityLayout.visibility = if (admin) View.GONE else View.VISIBLE
        binding.orderButton.visibility = if (admin) View.GONE else View.VISIBLE
        binding.editButton.visibility = if (admin) View.VISIBLE else View.GONE
        binding.deleteButton.visibility = if (admin) View.VISIBLE else View.GONE
        binding.editButton.setOnClickListener { startActivity(Intent(this, AddEditFruitActivity::class.java).putExtra("fruitId", fruitId)) }
        binding.deleteButton.setOnClickListener { AlertDialog.Builder(this).setTitle("Delete fruit?").setMessage("This removes the fruit from the catalog.").setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ ->
            try { if (db.deleteFruit(fruitId)) { notice("Fruit deleted"); finish() } else notice("Fruit not found") }
            catch (e: Exception) { notice(e.message ?: "Unable to delete fruit") }
        }.show() }
        binding.orderButton.setOnClickListener { confirmOrder() }
    }
    override fun onResume() { super.onResume(); if (::db.isInitialized) showFruit() }
    private fun showFruit() {
        val fruit = db.getFruitById(fruitId) ?: run { notice("Fruit not found"); finish(); return }
        binding.nameText.text = fruit.name
        val status = ValidationUtils.status(fruit)
        binding.statusText.text = status
        binding.statusText.setTextColor(ContextCompat.getColor(this, if (status == "Expired") R.color.danger else if (status == "Near Expiry" || status == "Out of Stock") R.color.warning else R.color.green))
        binding.detailsText.text = "Category: ${fruit.category.ifBlank { "—" }}\nPrice: ${money(fruit.price)}\nStock: ${fruit.quantity}\nExpiry date: ${fruit.expiryDate}\n\n${fruit.description.ifBlank { "No description" }}"
        binding.orderButton.isEnabled = ValidationUtils.purchasable(fruit)
    }
    private fun confirmOrder() {
        val fruit = db.getFruitById(fruitId) ?: run { notice("Fruit not found"); return }
        val quantity = binding.quantityInput.text.toString().toIntOrNull()
        if (quantity == null || quantity <= 0) { notice("Enter a quantity greater than zero"); return }
        if (!ValidationUtils.purchasable(fruit)) { notice("This fruit is not available"); showFruit(); return }
        if (quantity > fruit.quantity) { notice("Only ${fruit.quantity} in stock"); return }
        AlertDialog.Builder(this).setTitle("Place order?").setMessage("$quantity × ${fruit.name}\nTotal: ${money(fruit.price * quantity)}").setNegativeButton("Cancel", null).setPositiveButton("Order") { _, _ -> placeOrder(quantity) }.show()
    }
    private fun placeOrder(quantity: Int) {
        if (!requireRole("Customer")) return
        val customer = db.getCustomerByAccountId(SessionManager(this).userId) ?: run { notice("Customer profile not found"); return }
        try { val orderId = db.createOrder(customer.id, fruitId, quantity); notice("Order #$orderId placed"); showFruit() }
        catch (e: Exception) { notice(e.message ?: "Order failed"); showFruit() }
    }
    override fun onDestroy() { if (::db.isInitialized) db.close(); super.onDestroy() }
}

