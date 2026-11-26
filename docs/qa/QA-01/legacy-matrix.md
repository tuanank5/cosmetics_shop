# Đối chiếu 73 mã ca lịch sử

Cột Owner lấy phân loại từ phiếu lịch sử. Mỗi dòng nêu bằng chứng hồi quy và phạm vi quan sát. Các kết quả fixture không thay thế kết quả chạy tay của owner. Tên lớp tra trong `integration.txt`, tên gói Chrome tra trong JSON cùng thư mục.

| Mã | Chức năng | Owner lịch sử | Bằng chứng và phạm vi QA-01 |
|---|---|---|---|
| SHOP-01 | Logo và trang chủ | Ghi nhận khác | NAV-01 Chrome · Logo về chương đầu |
| SHOP-02 | Đi qua chương trang chủ | Đạt | NAV-01 Chrome · Chương và giảm chuyển động |
| SHOP-03 | Lối vào theo loại da, nghi thức, hoạt chất | Đạt | TrangChuPagesTest · Link SSR |
| SHOP-04 | Tối đa 12 sản phẩm trên home | Ghi nhận khác | TrangChuPagesTest, CuaHang_MariaDb_Test · Giới hạn và thứ tự |
| SHOP-05 | Tìm tên, SKU và hoạt chất | Ghi nhận khác | CuaHangTimKiemThanhPhanTest · Tìm SKU/thành phần, hướng dẫn hiểu SKU còn là phản hồi UX |
| SHOP-06 | Menu khám phá | Đạt | GiaoDienPagesTest · Neo catalog |
| SHOP-07 | Áp dụng bộ lọc | Đạt kèm ghi chú | UI-01C Chrome · Trạng thái bộ lọc qua pager |
| SHOP-08 | Chips và phân trang catalog | Ghi nhận khác | UI-01C Chrome, GiaoDienPagesTest · Pager và bộ lọc |
| SHOP-09 | Mở thẻ và link tim | Đạt | NAV-01, WISH-01 Chrome · PDP, Back và tim |
| SHOP-10 | Chọn SKU và thông tin sản phẩm | Đạt | CuaHang_MariaDb_Test, GiaoDienPagesTest · SKU, ảnh/giá và hết hàng |
| SHOP-11 | Xem và phân trang review | Chưa chạy | MongoDanhGia_Store_Test, DanhGiaPagesTest · Thứ tự và pager |
| SHOP-12 | Footer | Ghi nhận khác | Ngoài phạm vi · Footer và tài khoản mạng xã hội |
| SHOP-13 | Placeholder, video và giảm chuyển động | Chưa chạy | NAV-01 Chrome · Reduced motion và fallback, chưa thử media thương phẩm |
| TK-01 | Thêm SKU vào giỏ | Đạt | GioHangPagesTest, GioHangRepositoryTest · SKU và giỏ |
| TK-02 | Sửa số lượng và kiểm tồn | Đạt | GioHangPagesTest, DonHangCOD_MariaDb_Test · Giới hạn tồn |
| TK-03 | Xác nhận xóa dòng | Đạt | GioHangPagesTest · Xóa dòng theo form, chưa thao tác dialog lần này |
| TK-04 | Hợp nhất giỏ sau login | Ghi nhận khác | CART-01 Chrome, OAuth2Callback_MariaDb_Test · Hai tab, thông báo và gộp |
| TK-05 | Đăng ký Customer | Đạt | XacThuc_MariaDb_Test · Đăng ký và xác minh fixture |
| TK-06 | Đăng nhập và hiện mật khẩu | Ghi nhận khác | UI-01C Chrome, XacThuc_MariaDb_Test · Login và rect nút mật khẩu |
| TK-07 | Cập nhật hồ sơ | Đạt | XacThuc_MariaDb_Test · Hồ sơ đúng chủ |
| TK-08 | Quản lý địa chỉ | Đạt | DTO-01 Chrome, XacThuc_MariaDb_Test · Thêm/sửa, mặc định và đường về checkout |
| TK-09 | Thêm và bỏ yêu thích | Ghi nhận khác | WISH-01 Chrome, SauMua_MongoDanhGia_Test · Hai tab và đổi chủ |
| TK-10 | Điều hướng tài khoản/đăng xuất | Đạt | CART-01 Chrome, XacThuc_MariaDb_Test · Logout và phiên |
| DH-01 | Mở checkout | Đạt | CheckoutPagesTest, DTO-01 Chrome · HTML và địa chỉ |
| DH-02 | Báo giá và phí ship | Đạt | CheckoutIntegrityTest, DTO-01 Chrome · Báo giá và phí ship |
| DH-03 | Voucher không hợp lệ | Chưa chạy | CheckoutPagesTest, PhieuGiamGia_MariaDb_Test · Validation và voucher |
| DH-04 | Đặt COD | Đạt | DonHangCOD_MariaDb_Test · Đặt COD, kho và snapshot |
| DH-05 | Xem đơn đúng chủ | Đạt kèm ghi chú | HttpLoi_MariaDb_Test, OAuth2Callback_MariaDb_Test, DTO-01 Chrome · Đúng chủ, 404 khác chủ |
| DH-06 | Hủy COD | Đạt | DonHangCOD_MariaDb_Test · Hủy và hoàn đúng một lần |
| DH-07 | Xem tiến độ giao COD | Chưa chạy | DonHangCOD_MariaDb_Test · Chuyển trạng thái và thu tiền |
| DH-08 | Hai người tranh SKU hoặc lượt cuối | Chưa chạy | DonHangCOD_MariaDb_Test, PhieuGiamGia_MariaDb_Test · Tranh SKU/lượt và rollback |
| SM-01 | Mở review từ đơn giao | Đạt | SauMua_MongoDanhGia_Test · Đơn đã giao và chủ item |
| SM-02 | Gửi review và công khai sau duyệt | Đạt | SauMua_MongoDanhGia_Test, MongoDanhGia_Store_Test · Unique và duyệt |
| SM-03 | Sửa và duyệt lại review | Đạt | REVIEW-01 Chrome, SauMua_MongoDanhGia_Test · Từ chối revision cũ |
| SM-04 | Gửi yêu cầu đổi trả | Đạt | SauMua_MongoDanhGia_Test · Yêu cầu trên dòng đơn |
| SM-05 | Sai số lượng, ngoài hạn, sai chủ | Đạt | SauMua_MongoDanhGia_Test · Số lượng, hạn và chủ |
| SM-06 | Xem tiến độ trả hàng | Đạt | DTO-01 Chrome, SauMua_MongoDanhGia_Test · Chi tiết đúng chủ |
| AD-01 | Login Admin và redirect | Đạt | UI-01C Chrome, XacThuc_MariaDb_Test · Admin login |
| AD-02 | Sidebar, mobile và logout | Đạt | UI-01C Chrome, XacThuc_MariaDb_Test · Menu bàn phím và logout |
| AD-03 | Kỳ và KPI dashboard | Đạt | dashboard-matrix.json, ThongKe_MariaDb_Test · 8 KPI, 7/30/90 ngày |
| AD-04 | Biểu đồ, bảng, cảnh báo và link | Đạt | dashboard-matrix.json, AdminDashboardPagesTest · Chart/bảng và kỳ |
| AD-05 | Lọc và cập nhật trạng thái đơn | Ghi nhận khác | ORDER-01 Chrome, AdminOrderList_MariaDb_Test · Lọc và PRG |
| AD-06 | Xem tất cả và hủy COD Admin | Ghi nhận khác | ORDER-01 Chrome, DonHangCOD_MariaDb_Test · Xóa lọc và hủy |
| AD-07 | CRUD danh mục, thương hiệu, sản phẩm | Đạt | DanhMucSanPhamThuongHieu_MariaDb_Test · CRUD theo chính sách trạng thái |
| AD-08 | SKU, lô minh họa và ảnh | Đạt | BienTheSanPham_MariaDb_Test · Tồn, ảnh và lô |
| AD-09 | Tìm, khóa/mở tài khoản | Đạt | UI-01C Chrome, XacThuc_MariaDb_Test · Quản lý tài khoản và thu hồi phiên |
| AD-10 | Admin duyệt và ẩn review | Ghi nhận khác | REVIEW-01 Chrome, AdminDanhGiaPagesTest · Rect, focus và clamp |
| AD-11 | Admin xử lý đổi trả | Đạt | SauMua_MongoDanhGia_Test, UI-01C Chrome · Trạng thái và nguồn Admin |
| AD-12 | Tạo voucher | Đạt | PhieuGiamGia_MariaDb_Test, CheckoutIntegrityTest · Tạo và tính tiền |
| AD-13 | Sửa, tắt và xóa voucher | Đạt | PhieuGiamGia_MariaDb_Test · Lượt dùng và FK |
| AD-14 | Trang tri thức Admin | Chưa chạy | TuVanRag_MariaDb_Test · Mock, chưa có provider thật |
| AD-15 | Ngừng hoạt động nhóm catalog | Ghi nhận khác | DanhMucSanPhamThuongHieu_MariaDb_Test, AdminCatalogSecurityTest · Ngừng hoạt động và URL xóa cũ |
| MO-01 | COD khi MoMo tắt | Đạt | CheckoutPagesTest, DonHangCOD_MariaDb_Test · MoMo tắt |
| MO-02 | Checkout MoMo mock | Chưa chạy | Momo_MariaDb_Test · Mock, đơn chờ và giữ kho/lượt |
| MO-03 | MoMo mock thanh toán thành công | Chưa chạy | Momo_MariaDb_Test · Mock, chốt và IPN trùng |
| MO-04 | MoMo mock từ chối và retry | Chưa chạy | Momo_MariaDb_Test · Mock, retry/mã lượt |
| MO-05 | MoMo mock hết hạn | Chưa chạy | Momo_MariaDb_Test · Mock, giữ và giải phóng khi đã xác minh |
| MO-06 | Admin đối soát MoMo | Chưa chạy | Momo_MariaDb_Test, MomoReconciliationJobTest · Mock, con trỏ theo lô, chưa đợi tick định kỳ |
| MO-07 | MoMo Sandbox thật | Chưa chạy | Chưa chạy thật · Merchant và HTTPS IPN ngoài gate fixture |
| TV-01 | Hai trang tư vấn | Đạt | TuVanDaControllerTest, TuVan_ControllerTest · Hai route/form |
| TV-02 | Validation tư vấn da | Đạt | TuVanDaControllerTest · Validation field |
| TV-03 | Kết quả form tư vấn da | Đạt | TuVanDaServiceTest · Fixture, chưa có model thật |
| TV-04 | Tìm FAQ | Đạt | TuVanFaq_MariaDb_Test · FAQ SQL |
| TV-05 | Ngoài nguồn và câu sai | Đạt | TuVanFaq_MariaDb_Test · Thiếu nguồn |
| TV-06 | Giới hạn tần suất chat | Đạt | TuVanFaq_MariaDb_Test · 429 và Retry-After |
| TV-07 | Nạp kho RAG và hỏi | Chưa chạy | TuVanRag_MariaDb_Test · Mock, chưa chạy Ollama thật |
| QA-01 | Phân quyền Guest/Customer/Admin | Đạt | HttpLoi_MariaDb_Test, XacThuc_MariaDb_Test · Role, CSRF, phiên |
| QA-02 | Tài nguyên người khác và URL ẩn | Đạt | HttpLoi_MariaDb_Test, DonHangCOD_MariaDb_Test, DTO-01 Chrome · 404 tài nguyên khác chủ |
| QA-03 | Responsive và zoom | Trống | dashboard-matrix.json, UI-01C Chrome · Bốn viewport, mô phỏng zoom 200% |
| QA-04 | Bàn phím và focus | Đạt | UI-01C, NAV-01, ORDER-01 và REVIEW-01 Chrome · Tab, Enter, focus |
| QA-05 | Sử dụng không JS | Đạt | dashboard-matrix.json, DTO-01 và REVIEW-01 Chrome · Bảng, form và pager khi tắt JS |
| QA-06 | Mongo tắt và đổi kho | Chưa chạy | MongoTat_MariaDb_Test, MongoDanhGia_Store_Test, REVIEW-01 Chrome · Lỗi kho và phục hồi index |
| QA-07 | Upload SKU và HTTP 413 | Đạt | HttpLoi_MariaDb_Test, BienTheSanPham_MariaDb_Test · Upload và 413 |
