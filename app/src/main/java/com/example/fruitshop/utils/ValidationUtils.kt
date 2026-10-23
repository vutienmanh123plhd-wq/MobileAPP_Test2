package com.example.fruitshop.utils

import com.example.fruitshop.model.Fruit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object ValidationUtils {
    private val formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT)
    fun date(value: String): LocalDate? = try { LocalDate.parse(value, formatter) } catch (_: Exception) { null }
    fun fruitError(fruit: Fruit): String? = when {
        fruit.name.isBlank() -> "Enter a fruit name"
        fruit.price < 0 || !fruit.price.isFinite() -> "Enter a valid non-negative price"
        fruit.quantity < 0 -> "Quantity cannot be negative"
        date(fruit.expiryDate) == null -> "Use a valid date in yyyy-MM-dd format"
        else -> null
    }
    fun expired(fruit: Fruit): Boolean = date(fruit.expiryDate)?.isBefore(LocalDate.now()) ?: true
    fun status(fruit: Fruit): String = when {
        expired(fruit) -> "Expired"
        fruit.quantity == 0 -> "Out of Stock"
        date(fruit.expiryDate)!!.isBefore(LocalDate.now().plusDays(4)) -> "Near Expiry"
        else -> "Available"
    }
    fun purchasable(fruit: Fruit): Boolean = !expired(fruit) && fruit.quantity > 0 && fruit.price >= 0 && fruit.price.isFinite()
}

