package com.example.fruitshop

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Fruit(
    val id: Long,
    val name: String,
    val category: String,
    val price: Double,
    val quantity: Int,
    val expiryDate: String,
    val description: String
) {
    fun isExpired(today: String = FruitShopDbHelper.todayIsoDate()): Boolean = expiryDate < today
}

data class OrderHistory(
    val id: Long,
    val orderDate: String,
    val totalAmount: Double,
    val status: String,
    val items: String
)

data class LoginAccount(val id: Long, val username: String, val role: String)

class FruitShopDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE Account (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                role TEXT NOT NULL CHECK (role IN ('Admin', 'Customer'))
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE Fruit (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                price REAL NOT NULL CHECK (price >= 0),
                quantity INTEGER NOT NULL CHECK (quantity >= 0),
                expiryDate TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE Customer (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fullname TEXT NOT NULL,
                phone TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                accountId INTEGER NOT NULL UNIQUE REFERENCES Account(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE "Order" (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                customerId INTEGER NOT NULL REFERENCES Customer(id),
                orderDate TEXT NOT NULL,
                totalAmount REAL NOT NULL CHECK (totalAmount >= 0),
                status TEXT NOT NULL DEFAULT 'Pending'
                    CHECK (status IN ('Pending', 'Confirmed', 'Completed'))
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE OrderItem (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                orderId INTEGER NOT NULL REFERENCES "Order"(id) ON DELETE CASCADE,
                fruitId INTEGER REFERENCES Fruit(id) ON DELETE SET NULL,
                fruitName TEXT NOT NULL,
                quantity INTEGER NOT NULL CHECK (quantity > 0),
                unitPrice REAL NOT NULL CHECK (unitPrice >= 0)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_fruit_name ON Fruit(name)")
        db.execSQL("CREATE INDEX idx_order_customer ON \"Order\"(customerId)")
        insertSampleData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS OrderItem (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    orderId INTEGER NOT NULL REFERENCES "Order"(id) ON DELETE CASCADE,
                    fruitId INTEGER REFERENCES Fruit(id) ON DELETE SET NULL,
                    fruitName TEXT NOT NULL,
                    quantity INTEGER NOT NULL CHECK (quantity > 0),
                    unitPrice REAL NOT NULL CHECK (unitPrice >= 0)
                )
                """.trimIndent()
            )
        }
    }

    fun login(username: String, password: String): LoginAccount? {
        readableDatabase.rawQuery(
            "SELECT id, username, role FROM Account WHERE username = ? AND password = ?",
            arrayOf(username.trim(), password)
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return LoginAccount(cursor.getLong(0), cursor.getString(1), cursor.getString(2))
        }
    }

    fun fruits(search: String = ""): List<Fruit> {
        val result = mutableListOf<Fruit>()
        val selection = if (search.isBlank()) null else "name LIKE ? OR category LIKE ?"
        val args = if (search.isBlank()) null else arrayOf("%$search%", "%$search%")
        readableDatabase.query(
            "Fruit", FRUIT_COLUMNS, selection, args, null, null, "name COLLATE NOCASE"
        ).use { cursor ->
            while (cursor.moveToNext()) result.add(cursor.toFruit())
        }
        return result
    }

    fun saveFruit(fruit: Fruit): Long {
        require(fruit.name.isNotBlank()) { "Tên trái cây không được để trống." }
        require(fruit.category.isNotBlank()) { "Danh mục không được để trống." }
        require(fruit.price >= 0 && fruit.quantity >= 0) { "Giá và số lượng phải không âm." }
        require(isValidIsoDate(fruit.expiryDate)) { "Hạn sử dụng phải theo định dạng YYYY-MM-DD." }
        val values = ContentValues().apply {
            put("name", fruit.name.trim())
            put("category", fruit.category.trim())
            put("price", fruit.price)
            put("quantity", fruit.quantity)
            put("expiryDate", fruit.expiryDate)
            put("description", fruit.description.trim())
        }
        return if (fruit.id == 0L) writableDatabase.insertOrThrow("Fruit", null, values)
        else {
            writableDatabase.update("Fruit", values, "id = ?", arrayOf(fruit.id.toString()))
            fruit.id
        }
    }

    fun deleteFruit(id: Long) {
        writableDatabase.delete("Fruit", "id = ?", arrayOf(id.toString()))
    }

    fun customerIdForAccount(accountId: Long): Long? {
        readableDatabase.query(
            "Customer", arrayOf("id"), "accountId = ?", arrayOf(accountId.toString()),
            null, null, null
        ).use { cursor -> return if (cursor.moveToFirst()) cursor.getLong(0) else null }
    }

    fun placeOrder(customerId: Long, fruitId: Long, quantity: Int) {
        require(quantity > 0) { "Số lượng đặt phải lớn hơn 0." }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val fruit = db.query(
                "Fruit", FRUIT_COLUMNS, "id = ?", arrayOf(fruitId.toString()),
                null, null, null
            ).use { cursor ->
                if (!cursor.moveToFirst()) throw IllegalArgumentException("Không tìm thấy trái cây.")
                cursor.toFruit()
            }
            require(!fruit.isExpired()) { "Trái cây đã hết hạn, không thể đặt hàng." }
            require(fruit.quantity >= quantity) { "Số lượng tồn kho không đủ." }

            val orderValues = ContentValues().apply {
                put("customerId", customerId)
                put("orderDate", timestampNow())
                put("totalAmount", fruit.price * quantity)
                put("status", "Pending")
            }
            val orderId = db.insertOrThrow("\"Order\"", null, orderValues)
            val itemValues = ContentValues().apply {
                put("orderId", orderId)
                put("fruitId", fruit.id)
                put("fruitName", fruit.name)
                put("quantity", quantity)
                put("unitPrice", fruit.price)
            }
            db.insertOrThrow("OrderItem", null, itemValues)
            val stockUpdate = ContentValues().apply { put("quantity", fruit.quantity - quantity) }
            check(db.update("Fruit", stockUpdate, "id = ?", arrayOf(fruitId.toString())) == 1) {
                "Không thể cập nhật tồn kho."
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun orders(customerId: Long? = null): List<OrderHistory> {
        val result = mutableListOf<OrderHistory>()
        val selection = if (customerId == null) null else "o.customerId = ?"
        val args = if (customerId == null) null else arrayOf(customerId.toString())
        readableDatabase.rawQuery(
            """
            SELECT o.id, o.orderDate, o.totalAmount, o.status,
                   GROUP_CONCAT(i.fruitName || ' x' || i.quantity, ', ')
            FROM "Order" o
            LEFT JOIN OrderItem i ON i.orderId = o.id
            ${if (selection == null) "" else "WHERE $selection"}
            GROUP BY o.id
            ORDER BY o.id DESC
            """.trimIndent(),
            args
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    OrderHistory(
                        cursor.getLong(0), cursor.getString(1), cursor.getDouble(2),
                        cursor.getString(3), cursor.getString(4) ?: ""
                    )
                )
            }
        }
        return result
    }

    fun updateOrderStatus(orderId: Long, status: String) {
        require(status in ORDER_STATUSES) { "Trạng thái đơn hàng không hợp lệ." }
        val values = ContentValues().apply { put("status", status) }
        check(writableDatabase.update("\"Order\"", values, "id = ?", arrayOf(orderId.toString())) == 1) {
            "Không tìm thấy đơn hàng."
        }
    }

    private fun insertSampleData(db: SQLiteDatabase) {
        db.insertOrThrow(
            "Account", null, ContentValues().apply {
                put("username", "admin")
                put("password", "admin123")
                put("role", "Admin")
            }
        )
        val customerAccountId = db.insertOrThrow(
            "Account", null, ContentValues().apply {
                put("username", "customer")
                put("password", "customer123")
                put("role", "Customer")
            }
        )
        db.insertOrThrow(
            "Customer", null, ContentValues().apply {
                put("fullname", "Khách hàng mẫu")
                put("phone", "")
                put("email", "")
                put("accountId", customerAccountId)
            }
        )
        val samples = listOf(
            Fruit(0, "Táo", "Trái cây nhập khẩu", 45000.0, 30, dateAfter(10), "Táo tươi"),
            Fruit(0, "Cam", "Trái cây có múi", 30000.0, 40, dateAfter(14), "Cam ngọt"),
            Fruit(0, "Chuối", "Trái cây nhiệt đới", 20000.0, 50, dateAfter(7), "Chuối chín tự nhiên")
        )
        samples.forEach { fruit ->
            val values = ContentValues().apply {
                put("name", fruit.name)
                put("category", fruit.category)
                put("price", fruit.price)
                put("quantity", fruit.quantity)
                put("expiryDate", fruit.expiryDate)
                put("description", fruit.description)
            }
            db.insertOrThrow("Fruit", null, values)
        }
    }

    private fun android.database.Cursor.toFruit() = Fruit(
        getLong(getColumnIndexOrThrow("id")),
        getString(getColumnIndexOrThrow("name")),
        getString(getColumnIndexOrThrow("category")),
        getDouble(getColumnIndexOrThrow("price")),
        getInt(getColumnIndexOrThrow("quantity")),
        getString(getColumnIndexOrThrow("expiryDate")),
        getString(getColumnIndexOrThrow("description"))
    )

    companion object {
        private const val DATABASE_NAME = "fruit_shop.db"
        private const val DATABASE_VERSION = 2
        val ORDER_STATUSES = listOf("Pending", "Confirmed", "Completed")
        private val FRUIT_COLUMNS =
            arrayOf("id", "name", "category", "price", "quantity", "expiryDate", "description")

        fun todayIsoDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        private fun timestampNow(): String =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        private fun dateAfter(days: Int): String {
            val date = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, days) }
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date.time)
        }

        private fun isValidIsoDate(value: String): Boolean = try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            format.parse(value) != null && format.format(format.parse(value)!!) == value
        } catch (_: Exception) {
            false
        }
    }
}
