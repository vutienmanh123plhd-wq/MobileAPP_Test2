# Chức năng Customer

Tài khoản mẫu: `customer` / `customer123`. Đường dẫn: `MainActivity.kt` (MA) và `FruitShopDbHelper.kt` (DB).

## Đăng nhập và hồ sơ khách hàng
- `renderLogin()` (MA:69–92): đăng nhập qua `db.login()` (DB:122–130), lưu phiên vào `SharedPreferences`.
- `customerIdForAccount()` (DB:168–173): lấy `Customer.id` từ `accountId`; gọi ở MA:29 và MA:82.
- `render()` (MA:46–67): role khác `Admin` thì gọi `renderCustomer()` (MA:66).

## Xem danh sách trái cây
- `renderCustomer()` (MA:101–105): nút "Lịch sử đơn hàng", ô tìm kiếm và danh sách trái cây.
- `renderFruit(fruit, adminView = false)` (MA:122–146): hiển thị tên, danh mục, giá, tồn kho, hạn sử dụng,
  trạng thái "ĐÃ HẾT HẠN" / "HẾT HÀNG" / "Còn hàng" (MA:124–128) và nút "Đặt mua" (MA:141).

## Tìm kiếm
- `renderSearch()` (MA:107–120): nhập từ khóa, nút "Tìm kiếm" và "Xóa tìm kiếm".
- `fruits(search)` (DB:132–142): lọc `name LIKE ? OR category LIKE ?`, sắp xếp theo tên.

## Đặt hàng
- `showOrderDialog()` (MA:202–228): hộp thoại nhập số lượng, gọi `db.placeOrder()` (MA:214) và báo lỗi bằng toast.
- `placeOrder()` (DB:175–213): trong một transaction, tạo bản ghi `"Order"` (trạng thái `Pending`, DB:190–196),
  thêm `OrderItem` (DB:197–204) và trừ tồn kho (DB:205–208).

## Chặn hết hạn / thiếu hàng
- Giao diện: nút "Đặt mua" bị vô hiệu khi hết hạn hoặc tồn kho = 0 (MA:142).
- CSDL: `require(quantity > 0)` (DB:176), `require(!fruit.isExpired())` (DB:187), `require(fruit.quantity >= quantity)` (DB:188).
- `Fruit.isExpired()` (DB:20): so sánh chuỗi ISO `YYYY-MM-DD` với `todayIsoDate()` (DB:309).

## Lịch sử đơn hàng
- `showOrders(adminView = false)` (MA:230–273): hiển thị mã đơn, ngày, sản phẩm, tổng tiền, trạng thái; không có nút đổi trạng thái.
- `orders(customerId)` (DB:215–241): lọc theo `o.customerId`, gộp sản phẩm bằng `GROUP_CONCAT`, mới nhất trước.
