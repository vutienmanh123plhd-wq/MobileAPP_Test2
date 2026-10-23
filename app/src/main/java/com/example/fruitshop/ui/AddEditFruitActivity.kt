package com.example.fruitshop.ui

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fruitshop.model.Fruit
import com.example.fruitshop.database.DatabaseHelper
import com.example.fruitshop.databinding.ActivityAddEditFruitBinding
import com.example.fruitshop.utils.ValidationUtils
import java.time.LocalDate

class AddEditFruitActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEditFruitBinding
    private lateinit var db: DatabaseHelper
    private var fruitId = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requireRole("Admin")) return
        binding = ActivityAddEditFruitBinding.inflate(layoutInflater); setContentView(binding.root)
        db = DatabaseHelper(this)
        fruitId = intent.getLongExtra("fruitId", 0)
        if (fruitId != 0L) {
            val fruit = db.getFruitById(fruitId) ?: run { notice("Fruit not found"); finish(); return }
            binding.titleText.text = "Edit fruit"
            binding.nameInput.setText(fruit.name)
            binding.categoryInput.setText(fruit.category)
            binding.priceInput.setText(fruit.price.toString())
            binding.quantityInput.setText(fruit.quantity.toString())
            binding.expiryInput.setText(fruit.expiryDate)
            binding.descriptionInput.setText(fruit.description)
        }
        binding.expiryInput.setOnClickListener { pickDate() }
        binding.saveButton.setOnClickListener { save() }
    }
    private fun pickDate() {
        val date = ValidationUtils.date(binding.expiryInput.text.toString()) ?: LocalDate.now()
        DatePickerDialog(this, { _, year, month, day -> binding.expiryInput.setText(LocalDate.of(year, month + 1, day).toString()) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }
    private fun save() {
        val price = binding.priceInput.text.toString().toDoubleOrNull()
        val quantity = binding.quantityInput.text.toString().toIntOrNull()
        if (price == null || quantity == null) { notice("Enter a valid price and quantity"); return }
        val fruit = Fruit(fruitId, binding.nameInput.text.toString(), binding.categoryInput.text.toString(), price, quantity, binding.expiryInput.text.toString(), binding.descriptionInput.text.toString())
        val error = ValidationUtils.fruitError(fruit)
        if (error != null) { notice(error); return }
        try {
            val success = if (fruitId == 0L) db.insertFruit(fruit) > 0 else db.updateFruit(fruit)
            if (success) { notice("Fruit saved"); finish() } else notice("Fruit not found")
        } catch (e: Exception) { notice(e.message ?: "Unable to save fruit") }
    }
    override fun onDestroy() { if (::db.isInitialized) db.close(); super.onDestroy() }
}

