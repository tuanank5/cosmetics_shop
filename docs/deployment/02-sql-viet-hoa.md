# BeautyShop - SQL tên tiếng Việt không dấu

**Phạm vi:** Chuyển đầy đủ 20 bảng và các cột; giữ khóa chính, khóa ngoại, UNIQUE, CHECK, index, kiểu dữ liệu và mặc định.

Các tên kỹ thuật như `email`, `sku`, `InnoDB`, `DECIMAL` giữ nguyên để tránh gây khó hiểu.

## Bảng đối chiếu

| Tên trước | Tên sau |
|---|---|
| `users` | `nguoi_dung` |
| `addresses` | `dia_chi_nguoi_dung` |
| `categories` | `danh_muc` |
| `brands` | `thuong_hieu` |
| `products` | `san_pham` |
| `product_variants` | `bien_the_san_pham` |
| `carts` | `gio_hang` |
| `cart_items` | `chi_tiet_gio_hang` |
| `vouchers` | `phieu_giam_gia` |
| `orders` | `don_hang` |
| `order_items` | `chi_tiet_don_hang` |
| `voucher_reservations` | `tam_giu_phieu_giam_gia` |
| `inventory_reservations` | `tam_giu_ton_kho` |
| `payment_attempts` | `lan_thanh_toan` |
| `shipments` | `van_chuyen` |
| `reviews` | `danh_gia` |
| `wishlist_items` | `san_pham_yeu_thich` |
| `return_requests` | `yeu_cau_doi_tra` |
| `return_request_items` | `chi_tiet_doi_tra` |
| `faq_articles` | `bai_viet_faq` |

Chi tiết toàn bộ cột trong `bang_doi_chieu_ten.csv`.

## Chạy đúng với Flyway

1. **Database mới, chưa có bảng:** chạy `00_tao_database.sql` trong MariaDB.
2. Copy file `V1__khoi_tao_co_so_du_lieu.sql` vào `src/main/resources/db/migration/` (không được để đồng thời hai file V1).
3. Chạy Spring Boot với `spring.flyway.enabled=true`, kết nối database `beautyshop`.
4. Flyway sẽ tạo 20 bảng tên tiếng Việt không dấu.

**Rất quan trọng:** Nếu đã chạy Flyway V1 tiếng Anh ở cùng database, không được thay V1 tại chỗ. Hãy dùng database **mới và trống** cho bản SQL Việt hóa; nếu cần chuyển dữ liệu thật, phải viết migration chuyển tên bảng/cột và giữ dữ liệu.

## Đồng bộ với Java/JPA

Đã đổi tên thực tế trong SQL; **chưa đổi tên các lớp Entity/Repository/Service chưa được triển khai**. Khi lập trình JPA, luôn chỉ định `@Table(name=...)`, `@Column(name=...)`, `@JoinColumn(name=...)` theo tên SQL mới. Không để Hibernate tự suy diễn theo tên tiếng Anh.

Ví dụ:

```java
@Entity
@Table(name = "nguoi_dung")
public class NguoiDung {
    @Id
    @Column(name = "ma_nguoi_dung")
    private Long maNguoiDung;

    @Column(name = "ho_ten")
    private String hoTen;
}
```

Giữ `spring.jpa.hibernate.ddl-auto=validate` khi schema đã được migration.

## Kiểm tra

Đã kiểm tra tĩnh 20 bảng, quan hệ FK và số lượng CHECK/UNIQUE tương ứng bản gốc. Chưa chạy trực tiếp trên MariaDB server; phải kiểm thử Flyway trên database thật trước khi dùng trong project nhóm.
