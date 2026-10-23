package com.example.fruitshop

import android.app.Activity
import android.app.AlertDialog
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var db: FruitShopDbHelper
    private lateinit var root: LinearLayout
    private val preferences by lazy { getSharedPreferences("login_session", MODE_PRIVATE) }
    private var account: LoginAccount? = null
    private var customerId: Long? = null
    private var searchText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FruitShopDbHelper(this)
        account = restoreSession()
        customerId = account?.let { db.customerIdForAccount(it.id) }
        render()
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }

    private fun restoreSession(): LoginAccount? {
        val id = preferences.getLong("account_id", -1L)
        if (id < 0) return null
        val username = preferences.getString("username", null) ?: return null
        val role = preferences.getString("role", null) ?: return null
        return LoginAccount(id, username, role)
    }

    private fun render() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 12)
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        val current = account
        if (current == null) {
            renderLogin()
            return
        }
        text("Fruit Shop Management", 24f)
        text("Xin chào ${current.username} (${current.role})", 16f)
        button("Đăng xuất") {
            preferences.edit().clear().apply()
            account = null
            customerId = null
            render()
        }
        if (current.role == "Admin") renderAdmin() else renderCustomer()
    }

    private fun renderLogin() {
        text("Fruit Shop Management", 26f)
        text("Đăng nhập bằng tài khoản mẫu hoặc tài khoản trong cơ sở dữ liệu.", 15f)
        val username = edit("Tên đăng nhập")
        val password = edit("Mật khẩu").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        button("Đăng nhập") {
            val loggedIn = db.login(username.text.toString(), password.text.toString())
            if (loggedIn == null) {
                toast("Tên đăng nhập hoặc mật khẩu không đúng.")
            } else {
                account = loggedIn
                customerId = db.customerIdForAccount(loggedIn.id)
                preferences.edit()
                    .putLong("account_id", loggedIn.id)
                    .putString("username", loggedIn.username)
                    .putString("role", loggedIn.role)
                    .apply()
                render()
            }
        }
        text("Admin: admin / admin123\nCustomer: customer / customer123", 14f)
    }

    private fun renderAdmin() {
        button("Thêm trái cây") { showFruitDialog(null) }
        button("Quản lý đơn hàng") { showOrders(adminView = true) }
        renderSearch()
        db.fruits(searchText).forEach { fruit -> renderFruit(fruit, adminView = true) }
    }

    private fun renderCustomer() {
        button("Lịch sử đơn hàng") { showOrders(adminView = false) }
        renderSearch()
        db.fruits(searchText).forEach { fruit -> renderFruit(fruit, adminView = false) }
    }

    private fun renderSearch() {
        val input = edit("Tìm theo tên hoặc danh mục")
        input.setText(searchText)
        button("Tìm kiếm") {
            searchText = input.text.toString().trim()
            render()
        }
        if (searchText.isNotBlank()) {
            button("Xóa tìm kiếm") {
                searchText = ""
                render()
            }
        }
    }

    private fun renderFruit(fruit: Fruit, adminView: Boolean) {
        val expired = fruit.isExpired()
        val state = when {
            expired -> "ĐÃ HẾT HẠN"
            fruit.quantity <= 0 -> "HẾT HÀNG"
            else -> "Còn hàng"
        }
        text(
            "${fruit.name} — ${fruit.category}\n" +
                "Giá: ${money(fruit.price)} | Tồn kho: ${fruit.quantity}\n" +
                "Hạn sử dụng: ${fruit.expiryDate} ($state)\n${fruit.description}",
            16f
        )
        if (adminView) {
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(actionButton("Sửa") { showFruitDialog(fruit) })
            actions.addView(actionButton("Xóa") { confirmDelete(fruit) })
            root.addView(actions)
        } else {
            val orderButton = actionButton("Đặt mua") { showOrderDialog(fruit) }
            orderButton.isEnabled = !expired && fruit.quantity > 0
            root.addView(orderButton)
        }
        divider()
    }

    private fun showFruitDialog(fruit: Fruit?) {
        val fields = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 0)
        }
        val name = fields.field("Tên trái cây", fruit?.name.orEmpty())
        val category = fields.field("Danh mục", fruit?.category.orEmpty())
        val price = fields.field("Giá", fruit?.price?.toString().orEmpty(), InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val quantity = fields.field("Số lượng", fruit?.quantity?.toString().orEmpty(), InputType.TYPE_CLASS_NUMBER)
        val expiry = fields.field("Hạn sử dụng (YYYY-MM-DD)", fruit?.expiryDate.orEmpty())
        val description = fields.field("Mô tả", fruit?.description.orEmpty())
        AlertDialog.Builder(this)
            .setTitle(if (fruit == null) "Thêm trái cây" else "Cập nhật trái cây")
            .setView(fields)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Lưu", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        try {
                            db.saveFruit(
                                Fruit(
                                    fruit?.id ?: 0L, name.text.toString(), category.text.toString(),
                                    price.text.toString().toDouble(), quantity.text.toString().toInt(),
                                    expiry.text.toString(), description.text.toString()
                                )
                            )
                            dialog.dismiss()
                            render()
                        } catch (error: NumberFormatException) {
                            toast("Giá hoặc số lượng không hợp lệ.")
                        } catch (error: IllegalArgumentException) {
                            toast(error.message ?: "Dữ liệu không hợp lệ.")
                        } catch (error: SQLiteException) {
                            toast("Không thể lưu trái cây: ${error.message}")
                        }
                    }
                }
                dialog.show()
            }
    }

    private fun confirmDelete(fruit: Fruit) {
        AlertDialog.Builder(this)
            .setMessage("Xóa ${fruit.name}?")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ ->
                db.deleteFruit(fruit.id)
                render()
            }
            .show()
    }

    private fun showOrderDialog(fruit: Fruit) {
        val quantityInput = EditText(this).apply {
            hint = "Số lượng (còn ${fruit.quantity})"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        AlertDialog.Builder(this)
            .setTitle("Đặt ${fruit.name}")
            .setView(quantityInput)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Đặt hàng") { _, _ ->
                try {
                    val customer = customerId ?: throw IllegalStateException("Tài khoản chưa có hồ sơ khách hàng.")
                    db.placeOrder(customer, fruit.id, quantityInput.text.toString().toInt())
                    toast("Đặt hàng thành công.")
                    render()
                } catch (error: NumberFormatException) {
                    toast("Số lượng đặt không hợp lệ.")
                } catch (error: IllegalArgumentException) {
                    toast(error.message ?: "Không thể đặt hàng.")
                } catch (error: IllegalStateException) {
                    toast(error.message ?: "Không thể đặt hàng.")
                } catch (error: SQLiteException) {
                    toast(error.message ?: "Không thể đặt hàng.")
                }
            }
            .show()
    }

    private fun showOrders(adminView: Boolean) {
        val orders = db.orders(if (adminView) null else customerId)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 8)
        }
        lateinit var dialog: AlertDialog
        if (orders.isEmpty()) {
            content.addView(TextView(this).apply { text = "Chưa có đơn hàng." })
        }
        orders.forEach { order ->
            val item = TextView(this).apply {
                text = "#${order.id} | ${order.orderDate}\n${order.items}\n" +
                    "Tổng: ${money(order.totalAmount)} | ${order.status}"
                textSize = 16f
                setPadding(0, 12, 0, 12)
            }
            content.addView(item)
            if (adminView) {
                val next = FruitShopDbHelper.ORDER_STATUSES[
                    (FruitShopDbHelper.ORDER_STATUSES.indexOf(order.status) + 1)
                        .coerceAtMost(FruitShopDbHelper.ORDER_STATUSES.lastIndex)
                ]
                val update = Button(this).apply {
                    text = if (next == order.status) "Đã hoàn tất" else "Chuyển sang $next"
                    isEnabled = next != order.status
                    setOnClickListener {
                        db.updateOrderStatus(order.id, next)
                        toast("Đã cập nhật đơn hàng.")
                        dialog.dismiss()
                        showOrders(adminView)
                    }
                }
                content.addView(update)
            }
            content.addView(TextView(this).apply { text = "────────────────────" })
        }
        dialog = AlertDialog.Builder(this)
            .setTitle(if (adminView) "Quản lý đơn hàng" else "Lịch sử đơn hàng")
            .setView(ScrollView(this).apply { addView(content) })
            .setPositiveButton("Đóng", null)
            .create()
        dialog.show()
    }

    private fun edit(hintText: String): EditText = EditText(this).apply {
        hint = hintText
        maxLines = 1
        root.addView(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun LinearLayout.field(
        label: String,
        value: String,
        type: Int = InputType.TYPE_CLASS_TEXT
    ): EditText = EditText(this@MainActivity).apply {
        hint = label
        setText(value)
        inputType = type
        maxLines = 1
        fieldsAdd(this)
    }

    private fun LinearLayout.fieldsAdd(view: EditText) {
        addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun button(label: String, action: () -> Unit) {
        root.addView(actionButton(label, action))
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { action() }
    }

    private fun text(value: String, size: Float) {
        root.addView(TextView(this).apply {
            text = value
            textSize = size
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 10, 0, 10)
        })
    }

    private fun divider() {
        root.addView(TextView(this).apply { text = "────────────────────────────" })
    }

    private fun money(value: Double) = "%,.0f đ".format(value)

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
