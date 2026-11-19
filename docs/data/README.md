# Tên SQL và dữ liệu demo

## Ánh xạ MariaDB v5

[Bảng đối chiếu CSV](bang_doi_chieu_ten.csv) liệt kê tên bảng và cột cũ → mới. [ERD v5](../design/02-erd.md) mô tả quan hệ, còn [Flyway V1](../../src/main/resources/db/migration/V1__khoi_tao_co_so_du_lieu.sql) chứa định nghĩa SQL thực tế. Trong truy vấn SQL dùng tên vật lý MariaDB. Trong JPQL dùng tên entity và thuộc tính Java.

P01 ánh xạ `DanhMuc` → `danh_muc`, `ThuongHieu` → `thuong_hieu`, `SanPham` → `san_pham`. Thuộc tính JPA dùng camelCase, ví dụ `maThuongHieu` → `ma_thuong_hieu`. JPA liên kết thuộc tính và quan hệ với các cột qua `@Column` và `@JoinColumn`. Tên kỹ thuật như `email`, `sku` và mã trạng thái như `PENDING_PAYMENT`, `PAID` giữ theo schema và quy tắc nghiệp vụ.

## Dữ liệu demo

**Chưa có bộ seed đã kiểm chứng** trong scaffold. Để tránh bịa thành phần/giá/hình và rủi ro bản quyền, nhóm cần chuẩn bị dữ liệu từ website chính thức của thương hiệu/nhà bán có quyền phù hợp, ghi nguồn và ngày kiểm tra từng sản phẩm.

## Tiêu chí dữ liệu

- Mục tiêu 40 sản phẩm: 8–10 thương hiệu, 4–6 danh mục; mỗi sản phẩm 1–3 SKU khi thực tế có màu/dung tích.
- Bắt buộc mỗi sản phẩm có: tên, brand, category, tên SKU/size, nguồn, ngày kiểm tra, mô tả, URL ảnh nếu được phép, giá VND **demo** (giá chỉ mang tính mô phỏng, không được ghi là giá chính thức nếu không kiểm chứng), thành phần/loại da có căn cứ.
- Không scrape ảnh từ website không cho phép. Ảnh do nhóm tạo phải có tỷ lệ kích thước thống nhất, không làm giả nhãn/khẳng định sản phẩm thật.
- Tránh tuyên bố điều trị/y tế không được xác minh. Không đưa dữ liệu người mua thật vào seed.

## Quy trình

1. Thu thập vào `product-research.csv` (template chưa xác minh).
2. Điền `source_url`, `reviewed_at`, `image_license`, reviewer.
3. Chủ sản phẩm module A kiểm tra; D kiểm tra ảnh và metadata RAG.
4. Sau khi xác minh, chọn migration dành riêng cho môi trường demo hoặc seeder dev. Giữ dữ liệu demo ngoài đường chạy mặc định của production.

CSV `verified=no` nghĩa là **không được coi là sản phẩm thực tế đã kiểm chứng**.

Phân công và mốc kiểm tra nguồn nằm trong [roadmap](../03-roadmap-and-team.md).
