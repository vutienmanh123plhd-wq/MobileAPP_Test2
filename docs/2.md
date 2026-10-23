# Fruit Shop Management

A native Kotlin Android application for a four-table SQLite university assignment. It uses XML layouts, Material components, RecyclerView, ViewBinding, `SQLiteOpenHelper`, and SharedPreferences. All data stays on the Android device. There is no server or network permission.

## Open and run

1. Install Android Studio with Android SDK platform 37 and a JDK compatible with Gradle 9.6 / Android Gradle Plugin 9.4.
2. Open this `FruitShopManagement` directory in Android Studio.
3. Let Gradle sync, then run the `app` configuration on an Android emulator or device with Android 8.0 (API 26) or newer.
4. On first launch, the database seeds three fruits and two demo accounts:
   - Admin: `admin` / `Admin123!`
   - Customer: `customer` / `Customer123!`

The demo passwords are development-only seed data. They are salted and hashed with PBKDF2-HMAC-SHA256 before being stored in SQLite. SharedPreferences stores the account ID, username, and role, never a password. Login checks the stored hash. This is a local demonstration: an attacker who can alter the app or its local data can bypass local role checks. It is not server-side authentication.

## Exact database schema

Database: `fruit_shop.db`, version 1. The application creates exactly four tables (SQLite may create its own internal `sqlite_sequence` table for `AUTOINCREMENT`):

```sql
CREATE TABLE Account (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE,
  password TEXT NOT NULL, role TEXT NOT NULL CHECK(role IN ('Admin','Customer')));
CREATE TABLE Fruit (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, category TEXT,
  price REAL NOT NULL, quantity INTEGER NOT NULL, expiryDate TEXT NOT NULL, description TEXT);
CREATE TABLE Customer (id INTEGER PRIMARY KEY AUTOINCREMENT, fullname TEXT NOT NULL,
  phone TEXT, email TEXT, accountId INTEGER NOT NULL,
  FOREIGN KEY(accountId) REFERENCES Account(id));
CREATE TABLE Orders (id INTEGER PRIMARY KEY AUTOINCREMENT, customerId INTEGER NOT NULL,
  orderDate TEXT NOT NULL, totalAmount REAL NOT NULL,
  status TEXT NOT NULL CHECK(status IN ('Pending','Confirmed','Completed')),
  FOREIGN KEY(customerId) REFERENCES Customer(id));
```

Foreign keys are enabled in `onConfigure`. Database version 1 is the initial schema; `onUpgrade` deliberately stops an unsupported upgrade instead of dropping user data. A future schema change needs a versioned migration.

## How ordering works within four tables

Each checkout purchases one fruit and a positive quantity. A confirmation dialog shows the fruit, quantity, and calculated total. At checkout the app reads current fruit data, checks that expiry is today or later, checks stock and price, inserts an `Orders` row, and reduces stock in one SQLite transaction. A failed check or update rolls back the order and stock change. Fruits expiring today are purchasable. Dates use `yyyy-MM-dd`.

**Assignment schema limitation:** `Orders` has no fruit ID, quantity, or line items. After checkout, the database cannot identify which fruit or quantity was purchased. The order detail screen can therefore show only the order ID, customer, date, total, and status. It cannot truthfully show purchased item details or reconstruct a historical price. This is inherent in the required schema. Add an order-detail table only if the assignment rules are relaxed.

## Project map

- `app/src/main/java/com/example/fruitshop/database/DatabaseHelper.kt`: schema, seed data, queries, transaction
- `app/src/main/java/com/example/fruitshop/model/`: Account, Fruit, Customer, Order
- `app/src/main/java/com/example/fruitshop/ui/`: login, dashboards, fruit and order screens
- `app/src/main/java/com/example/fruitshop/adapter/`: RecyclerView adapters
- `app/src/main/java/com/example/fruitshop/utils/`: password hashing, session, date and fruit validation
- `app/src/main/res/layout/`: XML screens and list rows

## Phân công thành viên

Bảng phân công dưới đây do nhóm cung cấp. Dự án hiện tại viết bằng Kotlin: lớp cơ sở dữ liệu là `DatabaseHelper.kt` (kế thừa `SQLiteOpenHelper`) và bảng đơn hàng trong SQLite có tên `Orders`.

| Thành viên | Phần phụ trách | Sản phẩm bàn giao |
| --- | --- | --- |
| [vutienmanh123plhd-wq](https://github.com/vutienmanh123plhd-wq) | Thiết kế 4 bảng `Account`, `Fruit`, `Customer`, `Orders`; khóa chính, khóa ngoại, dữ liệu mẫu và hàm truy vấn trong `DatabaseHelper.kt`; thiết lập repository, thống nhất package và tích hợp code. | Cơ sở dữ liệu, API truy vấn dùng chung và dự án tích hợp. |
| [Izim-24](https://github.com/Izim-24) | Màn hình đăng nhập, kiểm tra tài khoản và mật khẩu, phân quyền Admin/Customer, lưu phiên bằng SharedPreferences, đăng xuất và điều hướng theo vai trò. | Luồng đăng nhập, đăng xuất và phân quyền. |
| [khanhhsd](https://github.com/khanhhsd) | Danh sách trái cây cho Admin; thêm, sửa, xóa, tìm kiếm; quản lý thông tin trái cây, cảnh báo hết hạn, kiểm tra đầu vào và xác nhận xóa. | CRUD và tìm kiếm trái cây. |
| [yuhna111](https://github.com/yuhna111) | Danh sách trái cây có thể mua, tìm kiếm và xem chi tiết; chọn số lượng, đặt hàng, xem lịch sử; chặn trái cây hết hạn hoặc không đủ tồn kho. | Luồng xem sản phẩm, đặt hàng và xem lịch sử. |
| [hastur-78](https://github.com/hastur-78) | Danh sách và chi tiết đơn hàng cho Admin, cập nhật `Pending`/`Confirmed`/`Completed`; kiểm thử đăng nhập, đặt hàng, hạn sử dụng và tồn kho; test case, ảnh kết quả và báo cáo. | Quản lý trạng thái đơn hàng và bộ kiểm thử. |

## Demo test checklist

- [ v] Admin login opens Admin dashboard; customer login opens Customer dashboard.
- [v ] Wrong credentials show an error. Relaunch restores the session. Logout clears it.
- [ v] Admin adds, edits, deletes, searches, and opens fruits. Delete asks for confirmation.
- [ v] Invalid name, price, quantity, or expiry date is rejected.
- [ v] Expired fruits have an Expired label; fruit expiring today stays purchasable.
- [ v] Customer can search and open fruits; expired and out-of-stock fruit cannot be ordered.
- [ v] Ordering more than stock fails; a valid order shows confirmation and reduces stock.
- [ v] Failed checkout leaves order count and stock unchanged.
- [ v] Customer history lists only that customer’s orders; admin sees all orders.
- [ v] Admin changes statuses among Pending, Confirmed, and Completed.
- [ v] Reopening an order displays its saved total and current status.
- [ v] Attempting an invalid foreign key insert is rejected by SQLite.

Screenshots to capture for submission: login, each dashboard, fruit list with expiry labels, add/edit form, fruit detail, checkout confirmation, customer history, order detail, and admin order management.

## GitHub

Repository: [MobileAPP_Test2](https://github.com/vutienmanh123plhd-wq/MobileAPP_Test2). Mỗi thành viên tạo nhánh cho phần mình phụ trách, commit thay đổi thực tế bằng tài khoản và email GitHub của mình, rồi mở pull request vào `main`. Bảng phân công ở trên ghi trách nhiệm của nhóm; danh sách Contributors trên GitHub được tính từ tác giả commit thực tế. Không commit `local.properties`, tệp build hoặc thông tin đăng nhập ngoài tài khoản demo đã ghi rõ.
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
        //Xem chi tiết trái cây, hiển thị cảnh báo hết hạn/tồn kho, cung cấp nút Sửa và Xóa
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


