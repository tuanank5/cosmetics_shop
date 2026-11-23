# Tên SQL và dữ liệu demo

## Ánh xạ MariaDB v5

[Bảng đối chiếu CSV](bang_doi_chieu_ten.csv) liệt kê tên bảng và cột cũ → mới. [ERD v5](../design/02-erd.md) mô tả quan hệ, còn [Flyway V1](../../src/main/resources/db/migration/V1__khoi_tao_co_so_du_lieu.sql) chứa định nghĩa SQL thực tế. Trong truy vấn SQL dùng tên vật lý MariaDB. Trong JPQL dùng tên entity và thuộc tính Java.

P01 ánh xạ `DanhMuc` → `danh_muc`, `ThuongHieu` → `thuong_hieu`, `SanPham` → `san_pham`. Thuộc tính JPA dùng camelCase, ví dụ `maThuongHieu` → `ma_thuong_hieu`. JPA liên kết thuộc tính và quan hệ với các cột qua `@Column` và `@JoinColumn`. Tên kỹ thuật như `email`, `sku` và mã trạng thái như `PENDING_PAYMENT`, `PAID` giữ theo schema và quy tắc nghiệp vụ.

## Dữ liệu demo

[Catalog P10](product-research.csv) có 31 sản phẩm, 32 SKU demo, 8 thương hiệu và 6 danh mục. Mỗi sản phẩm ghi trang chính thức và ngày đối chiếu. Một số SKU dùng nhãn minh họa khi chưa xác minh dung tích. Ô thành phần hoặc loại da để trống khi chưa có dữ liệu được đối chiếu.

Giá và tồn dành cho kịch bản demo. Các thẻ dùng minh họa CSS nguyên gốc của BeautyShop với tỷ lệ 4:3. Tệp CSV ghi rõ xuất xứ của minh họa. [Cách nạp catalog](../deployment/06-p10-demo.md).

### Ảnh sản phẩm demo (P16)

Ảnh demo nằm trong JAR, tại `src/main/resources/static/img/products/`, để không bị mất khi host restart hoặc redeploy. Ảnh Admin upload vào `uploads/sku` vẫn dùng cơ chế cũ.

- Tên file viết thường, không dấu, không khoảng trắng, ví dụ `r001.webp`. Định dạng `.webp`, `.jpg` hoặc `.png`, tỉ lệ 4:5 (khoảng 800×1000 px), mỗi file ≤ 300 KB. Chỉ dùng ảnh nhóm có quyền sử dụng và ghi nguồn tại `image_source`/`image_license`.
- Cột `image_path` của CSV ghi `/img/products/<tên file>`. Để trống nếu chưa có ảnh: SKU nhận `NULL` và giao diện hiện placeholder.
- `scripts/seed-demo.ps1` từ chối đường dẫn sai mẫu, file không tồn tại hoặc file > 300 KB, rồi ghi giá trị vào `bien_the_san_pham.url_hinh_anh` (`san_pham` không có cột ảnh). Chạy lại seed chỉ cập nhật ảnh còn trống hoặc ảnh `/img/products/`, không đè ảnh Admin đã upload.
- Tại thời điểm P16, thư mục chưa có ảnh và mọi `image_path` đều trống.

## Tiêu chí dữ liệu

- Mục tiêu 40 sản phẩm: 8–10 thương hiệu, 4–6 danh mục; mỗi sản phẩm 1–3 SKU khi thực tế có màu/dung tích.
- Bắt buộc mỗi sản phẩm có: tên, brand, category, tên SKU/size, nguồn, ngày kiểm tra, mô tả, URL ảnh nếu được phép, giá VND **demo** (giá chỉ mang tính mô phỏng, không được ghi là giá chính thức nếu không kiểm chứng), thành phần/loại da có căn cứ.
- Không scrape ảnh từ website không cho phép. Ảnh do nhóm tạo phải có tỷ lệ kích thước thống nhất, không làm giả nhãn/khẳng định sản phẩm thật.
- Tránh tuyên bố điều trị/y tế không được xác minh. Không đưa dữ liệu người mua thật vào seed.

## Quy trình

1. Thu thập vào `product-research.csv`. Dùng cùng `research_id` cho các SKU của một sản phẩm.
2. Điền `source_url`, `reviewed_at`, `image_license`, reviewer.
3. Chủ sản phẩm module A kiểm tra; D kiểm tra ảnh và metadata RAG.
4. Sau khi xác minh, chạy seeder dev trên database demo riêng đã được Flyway khởi tạo.

CSV `verified=no` nghĩa là **không được coi là sản phẩm thực tế đã kiểm chứng**.

Phân công và mốc kiểm tra nguồn nằm trong [roadmap](../03-roadmap-and-team.md).
