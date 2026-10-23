package com.example.fruitshop.model
data class Order(val id: Long, val customerId: Long, val orderDate: String, val totalAmount: Double, val status: String)

