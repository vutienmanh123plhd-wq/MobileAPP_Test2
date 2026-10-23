package com.example.fruitshop.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.fruitshop.model.Account
import com.example.fruitshop.model.Customer
import com.example.fruitshop.model.Fruit
import com.example.fruitshop.model.Order
import com.example.fruitshop.utils.PasswordUtils
import com.example.fruitshop.utils.ValidationUtils
import java.time.LocalDate

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object { const val DATABASE_NAME = "fruit_shop.db"; const val DATABASE_VERSION = 1 }

    override fun onConfigure(db: SQLiteDatabase) { super.onConfigure(db); db.setForeignKeyConstraintsEnabled(true) }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE Account (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password TEXT NOT NULL, role TEXT NOT NULL CHECK(role IN ('Admin','Customer')))")
        db.execSQL("CREATE TABLE Fruit (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, category TEXT, price REAL NOT NULL, quantity INTEGER NOT NULL, expiryDate TEXT NOT NULL, description TEXT)")
        db.execSQL("CREATE TABLE Customer (id INTEGER PRIMARY KEY AUTOINCREMENT, fullname TEXT NOT NULL, phone TEXT, email TEXT, accountId INTEGER NOT NULL, FOREIGN KEY(accountId) REFERENCES Account(id))")
        db.execSQL("CREATE TABLE Orders (id INTEGER PRIMARY KEY AUTOINCREMENT, customerId INTEGER NOT NULL, orderDate TEXT NOT NULL, totalAmount REAL NOT NULL, status TEXT NOT NULL CHECK(status IN ('Pending','Confirmed','Completed')), FOREIGN KEY(customerId) REFERENCES Customer(id))")
        // Development-only demo credentials, stored in the database as salted hashes.
        insertAccount(db, "admin", "Admin123!", "Admin")
        val customerId = insertAccount(db, "customer", "Customer123!", "Customer")
        db.insertOrThrow("Customer", null, ContentValues().apply { put("fullname", "Sample Customer"); put("phone", ""); put("email", ""); put("accountId", customerId) })
        listOf(
            Fruit(name = "Apple", category = "Fresh Fruit", price = 2.50, quantity = 30, expiryDate = LocalDate.now().plusDays(10).toString(), description = "Crisp and sweet"),
            Fruit(name = "Banana", category = "Fresh Fruit", price = 1.80, quantity = 24, expiryDate = LocalDate.now().plusDays(2).toString(), description = "Ripe yellow bananas"),
            Fruit(name = "Orange", category = "Citrus", price = 3.20, quantity = 18, expiryDate = LocalDate.now().plusDays(7).toString(), description = "Juicy oranges")
        ).forEach { db.insertOrThrow("Fruit", null, fruitValues(it)) }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No schema upgrade exists yet. Never drop user data implicitly.
        throw IllegalStateException("Unsupported database migration $oldVersion to $newVersion")
    }

    private fun insertAccount(db: SQLiteDatabase, username: String, password: String, role: String): Long =
        db.insertOrThrow("Account", null, ContentValues().apply { put("username", username); put("password", PasswordUtils.hash(password)); put("role", role) })

    fun insertAccount(username: String, password: String, role: String): Long {
        require(role == "Admin" || role == "Customer")
        return insertAccount(writableDatabase, username.trim(), password, role)
    }
    fun getAccountByUsername(username: String): Account? = readableDatabase.query("Account", null, "username = ?", arrayOf(username), null, null, null).use { if (it.moveToFirst()) it.account() else null }
    fun validateLogin(username: String, password: String): Account? = getAccountByUsername(username.trim())?.takeIf { PasswordUtils.verify(password, it.passwordHash) }
    fun insertCustomer(fullname: String, phone: String, email: String, accountId: Long): Long = writableDatabase.insertOrThrow("Customer", null, ContentValues().apply { put("fullname", fullname); put("phone", phone); put("email", email); put("accountId", accountId) })
    fun getCustomerByAccountId(accountId: Long): Customer? = readableDatabase.query("Customer", null, "accountId = ?", arrayOf(accountId.toString()), null, null, null).use { if (it.moveToFirst()) it.customer() else null }
    fun getCustomerById(id: Long): Customer? = readableDatabase.query("Customer", null, "id = ?", arrayOf(id.toString()), null, null, null).use { if (it.moveToFirst()) it.customer() else null }

    private fun fruitValues(fruit: Fruit) = ContentValues().apply { put("name", fruit.name.trim()); put("category", fruit.category.trim()); put("price", fruit.price); put("quantity", fruit.quantity); put("expiryDate", fruit.expiryDate); put("description", fruit.description.trim()) }
    fun insertFruit(fruit: Fruit): Long { require(ValidationUtils.fruitError(fruit) == null); return writableDatabase.insertOrThrow("Fruit", null, fruitValues(fruit)) }
    fun updateFruit(fruit: Fruit): Boolean { require(ValidationUtils.fruitError(fruit) == null); return writableDatabase.update("Fruit", fruitValues(fruit), "id = ?", arrayOf(fruit.id.toString())) == 1 }
    fun deleteFruit(id: Long): Boolean = writableDatabase.delete("Fruit", "id = ?", arrayOf(id.toString())) == 1
    fun getAllFruits(): List<Fruit> = fruits(null, null)
    fun getFruitById(id: Long): Fruit? = readableDatabase.query("Fruit", null, "id = ?", arrayOf(id.toString()), null, null, null).use { if (it.moveToFirst()) it.fruit() else null }
    fun searchFruits(query: String): List<Fruit> = fruits("name LIKE ? ESCAPE '\\'", arrayOf("%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"))
    private fun fruits(selection: String?, args: Array<String>?): List<Fruit> = readableDatabase.query("Fruit", null, selection, args, null, null, "name COLLATE NOCASE").use { c -> buildList { while (c.moveToNext()) add(c.fruit()) } }

    /** A single-fruit purchase. The four-table schema cannot retain which fruit an order bought. */
    fun createOrder(customerId: Long, fruitId: Long, requestedQuantity: Int): Long {
        require(requestedQuantity > 0) { "Quantity must be greater than zero" }
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (getCustomerById(customerId) == null) error("Customer profile not found")
            val fruit = db.query("Fruit", null, "id = ?", arrayOf(fruitId.toString()), null, null, null).use { if (it.moveToFirst()) it.fruit() else null }
                ?: error("Fruit is no longer available")
            if (ValidationUtils.expired(fruit)) error("This fruit has expired")
            if (fruit.price < 0 || !fruit.price.isFinite()) error("Invalid fruit price")
            if (fruit.quantity < requestedQuantity) error("Only ${fruit.quantity} in stock")
            val total = fruit.price * requestedQuantity
            if (!total.isFinite()) error("Invalid order total")
            val orderId = db.insertOrThrow("Orders", null, ContentValues().apply { put("customerId", customerId); put("orderDate", LocalDate.now().toString()); put("totalAmount", total); put("status", "Pending") })
            val updated = db.update("Fruit", ContentValues().apply { put("quantity", fruit.quantity - requestedQuantity) }, "id = ? AND quantity >= ? AND expiryDate >= ?", arrayOf(fruitId.toString(), requestedQuantity.toString(), LocalDate.now().toString()))
            if (updated != 1) error("Stock changed; please try again")
            db.setTransactionSuccessful()
            return orderId
        } finally { db.endTransaction() }
    }
    fun getOrdersByCustomer(customerId: Long): List<Order> = orders("customerId = ?", arrayOf(customerId.toString()))
    fun getAllOrders(): List<Order> = orders(null, null)
    fun getOrderById(id: Long): Order? = readableDatabase.query("Orders", null, "id = ?", arrayOf(id.toString()), null, null, null).use { if (it.moveToFirst()) it.order() else null }
    fun updateOrderStatus(id: Long, status: String): Boolean {
        require(status in setOf("Pending", "Confirmed", "Completed"))
        return writableDatabase.update("Orders", ContentValues().apply { put("status", status) }, "id = ?", arrayOf(id.toString())) == 1
    }
    private fun orders(selection: String?, args: Array<String>?): List<Order> = readableDatabase.query("Orders", null, selection, args, null, null, "id DESC").use { c -> buildList { while (c.moveToNext()) add(c.order()) } }

    private fun Cursor.account() = Account(getLong(getColumnIndexOrThrow("id")), getString(getColumnIndexOrThrow("username")), getString(getColumnIndexOrThrow("password")), getString(getColumnIndexOrThrow("role")))
    private fun Cursor.customer() = Customer(getLong(getColumnIndexOrThrow("id")), getString(getColumnIndexOrThrow("fullname")), getString(getColumnIndexOrThrow("phone")) ?: "", getString(getColumnIndexOrThrow("email")) ?: "", getLong(getColumnIndexOrThrow("accountId")))
    private fun Cursor.fruit() = Fruit(getLong(getColumnIndexOrThrow("id")), getString(getColumnIndexOrThrow("name")), getString(getColumnIndexOrThrow("category")) ?: "", getDouble(getColumnIndexOrThrow("price")), getInt(getColumnIndexOrThrow("quantity")), getString(getColumnIndexOrThrow("expiryDate")), getString(getColumnIndexOrThrow("description")) ?: "")
    private fun Cursor.order() = Order(getLong(getColumnIndexOrThrow("id")), getLong(getColumnIndexOrThrow("customerId")), getString(getColumnIndexOrThrow("orderDate")), getDouble(getColumnIndexOrThrow("totalAmount")), getString(getColumnIndexOrThrow("status")))
}

