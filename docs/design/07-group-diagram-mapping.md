# Đối chiếu Class Diagram nhóm tự vẽ ↔ mô hình v4

**Bản chính thức để nộp báo cáo hiện tại:** `docs/design/06-domain-classes.puml` + `docs/design/02-erd.md` + migration V1. Không nộp sơ đồ ảnh cũ như schema đang triển khai khi tên/quan hệ không khớp.

| Lớp trong sơ đồ nhóm | Mô hình v4 | Lý do |
|---|---|---|
| `NguoiDung` | `User` / `users` | Giữ actor Customer/Admin bằng `role`; Guest anonymous |
| `VaiTro` | `User.role` VARCHAR / Java `UserRole` enum | Chỉ 2 vai trò lưu DB, tránh bảng lookup không cần thiết; nếu bài yêu cầu bảng role riêng, migration V2 |
| `DiaChiNguoiDung` | `Address` / `addresses` | Giữ, đơn có snapshot địa chỉ độc lập |
| `PhienDangNhap` | Spring Security HttpSession | Session do container quản lý, không lưu bảng login session |
| `LichSu` | `orders`, `order_items`, `payment_attempts` | Lịch sử mua/trả tiền được truy vấn từ các bảng thực; không có bảng ghi thao tác tổng quát v1 |
| `SanPham` | `Product` + `ProductVariant` | SKU có giá/stock riêng, tránh sai lệch khi nhiều màu/dung tích |
| `ThuongHieu` | `Brand` | Giữ |
| Thiếu `DanhMuc` | `Category` | Bổ sung để lọc theo danh mục |
| `GioHang`, `ChiTietGioHang` | `Cart`, `CartItem` | Guest session; Customer lưu DB; CartItem->SKU |
| `DonHang`, `ChiTietDonHang` | `Order`, `OrderItem` | Snapshot SKU, giá và địa chỉ khi đặt |
| `ThanhToan` | `PaymentAttempt` | Hỗ trợ nhiều lần thử thanh toán/đơn; PAID thuộc payment, không thuộc order |
| `VanChuyen` | `Shipment` | Admin cập nhật thủ công |
| `KhuyenMai` | `Voucher` | Giảm giá toàn đơn, 1 mã/đơn |
| `ChiTietPhieuGiamGia` | `VoucherReservation` | Giữ lượt voucher theo order online, không phát mã riêng từng User |
| `SanPham_KhuyenMai` | Không dùng ở MVP | Không làm giảm giá riêng theo sản phẩm; nếu cần sẽ thêm migration riêng |
| `DanhGia`, `YeuThich` | `Review`, `WishlistItem` | Giữ, review chỉ sau DELIVERED |
| Chưa có | `InventoryReservation`, `ReturnRequest`, `ReturnRequestItem`, `FaqArticle` | Bổ sung cho MoMo/đổi trả/RAG |

**Không nhầm lẫn:** Sơ đồ class mô tả đối tượng miền, ERD mô tả bảng SQL. `@SessionScope`/Spring Security có thể có lớp Java nhưng không bắt buộc bảng tương ứng. Bản đồ đối chiếu dùng khi thuyết trình lý do sửa sơ đồ nhóm.
