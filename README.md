# Fruit Shop Management

Ứng dụng Android viết bằng Kotlin, dùng SQLite (`SQLiteOpenHelper`) để lưu dữ liệu.

## Mở và chạy

Mở thư mục `Test2` bằng Android Studio, chờ Gradle Sync hoàn tất, sau đó chạy cấu hình `app` trên emulator hoặc thiết bị Android.

## Tài khoản mẫu

- Admin: `admin` / `admin123`
- Customer: `customer` / `customer123`

Phiên đăng nhập (id, username, role) được lưu trong `SharedPreferences`; mật khẩu không được lưu ở đó.

## Lưu trữ và chức năng

- `Account`, `Fruit`, `Customer`, `"Order"` là các bảng chính. Bảng `OrderItem` bổ sung chi tiết sản phẩm để hiển thị lịch sử đơn hàng.
- Admin có thể thêm, sửa, xóa, tìm kiếm trái cây và cập nhật trạng thái đơn hàng theo thứ tự Pending → Confirmed → Completed.
- Customer có thể xem/tìm kiếm trái cây, xem hạn sử dụng, đặt hàng và xem lịch sử đơn hàng.
- Tạo đơn hàng dùng transaction; kiểm tra hạn sử dụng và tồn kho trước khi ghi đơn, đồng thời cập nhật tồn kho nguyên tử.
- Hạn sử dụng dùng định dạng ISO `YYYY-MM-DD`, cho phép so sánh ngày chính xác.
- Database mẫu được khởi tạo tự động ở lần chạy đầu tiên.
