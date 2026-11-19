# Đối chiếu tên SQL tiếng Việt không dấu (BeautyShop v5)

Đây là bộ tên chuẩn từ V1 mới. Dùng bảng này khi viết Entity/Repository và JPQL/SQL.

| Bảng cũ | Bảng MariaDB chuẩn v5 | Java Entity gợi ý |
|---|---|---|
| `users` | `nguoi_dung` | `NguoiDung` |
| `addresses` | `dia_chi_nguoi_dung` | `DiaChiNguoiDung` |
| `categories` | `danh_muc` | `DanhMuc` |
| `brands` | `thuong_hieu` | `ThuongHieu` |
| `products` | `san_pham` | `SanPham` |
| `product_variants` | `bien_the_san_pham` | `BienTheSanPham` |
| `carts` | `gio_hang` | `GioHang` |
| `cart_items` | `chi_tiet_gio_hang` | `ChiTietGioHang` |
| `vouchers` | `phieu_giam_gia` | `PhieuGiamGia` |
| `orders` | `don_hang` | `DonHang` |
| `order_items` | `chi_tiet_don_hang` | `ChiTietDonHang` |
| `voucher_reservations` | `tam_giu_phieu_giam_gia` | `TamGiuPhieuGiamGia` |
| `inventory_reservations` | `tam_giu_ton_kho` | `TamGiuTonKho` |
| `payment_attempts` | `lan_thanh_toan` | `LanThanhToan` |
| `shipments` | `van_chuyen` | `VanChuyen` |
| `reviews` | `danh_gia` | `DanhGia` |
| `wishlist_items` | `san_pham_yeu_thich` | `YeuThich` |
| `return_requests` | `yeu_cau_doi_tra` | `YeuCauDoiTra` |
| `return_request_items` | `chi_tiet_doi_tra` | `ChiTietDoiTra` |
| `faq_articles` | `bai_viet_faq` | `BaiVietFaq` |

**Quy ước Java:** có thể dùng thuộc tính tiếng Việt không dấu `maNguoiDung`, `hoTen`, `soLuong`, `giaBan`. Ánh xạ bằng `@Table`, `@Column`, `@JoinColumn` chính xác như SQL.

**Quy ước nghiệp vụ:** `GUEST` là người chưa đăng nhập (không có bản ghi người dùng). Chỉ giữ SKU của khách thanh toán online; giỏ Guest ở `HttpSession`. Các mã trạng thái `PENDING_PAYMENT`, `PAID`, `COD`, `MOMO` vẫn giữ nguyên vì là giá trị giao tiếp hệ thống, không phải tên biến.

Xem `docs/data/bang_doi_chieu_ten.csv` cho danh sách đầy đủ tất cả cột.
