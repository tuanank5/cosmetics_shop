# P20B – Ảnh sản phẩm demo

Trạng thái: **BƯỚC 2 đã chạy, nhưng chưa nạp được ảnh nào (0/32 SKU).** Mọi ảnh owner đặt vào đều dưới ngưỡng tối thiểu 600×750, hoặc có tên không ghép được với sản phẩm. Chưa commit/push.

## 1. Tóm tắt

| Mục | Kết quả |
|---|---|
| File trong `assets-raw/products/` | 11 |
| Ghép được với `research_id` | 9 (r001–r009, mỗi mã một ảnh chính, không có ảnh phụ) |
| Không ghép được | 2 (`r0010.jpg`, `r0010-1.jpg`) |
| Đạt ngưỡng 600×750 sau khi cắt 4:5 | **0** |
| Ảnh đã xuất vào `static/img/products/` | **0** |
| SKU đã nạp ảnh / tổng số SKU | **0 / 32** (32 SKU vẫn dùng placeholder) |
| Tổng dung lượng `src/main/resources/static/img/products/` | 0 B (thư mục rỗng) |

## 2. Bảng file → sản phẩm

Mọi file đều là JPEG thật (nhận diện theo nội dung bằng Pillow, không theo đuôi) và không có EXIF.

| File gốc | Kích thước gốc | Sau cắt 4:5 | Ghép với | Kết quả |
|---|---|---|---|---|
| `r001.jpg` | 150×200 | 150×187 | r001 · DEMO-001 · CeraVe Moisturizing Cream | QUA_NHO, bỏ qua |
| `r002.jpg` | 150×200 | 150×187 | r002 · DEMO-002 · CeraVe Hydrating Hyaluronic Acid Water Gel | QUA_NHO, bỏ qua |
| `r003.jpg` | 150×200 | 150×187 | r003 · DEMO-003 · CeraVe Invisible Mineral Sunscreen SPF 50 | QUA_NHO, bỏ qua |
| `r004.jpg` | 150×200 | 150×187 | r004 · DEMO-004 · CeraVe Hydrating Micellar Water | QUA_NHO, bỏ qua |
| `r005.jpg` | 150×200 | 150×187 | r005 · DEMO-005 · CeraVe Eczema Relief Moisturizing Cream | QUA_NHO, bỏ qua |
| `r006.jpg` | 101×200 | 101×126 | r006 · DEMO-006 · La Roche-Posay Toleriane Purifying Foaming Face Wash | QUA_NHO, bỏ qua |
| `r007.jpg` | 90×200 | 90×112 | r007 · DEMO-007 · La Roche-Posay Vitamin C12 Serum | QUA_NHO, bỏ qua |
| `r008.jpg` | 150×200 | 150×187 | r008 · DEMO-008 · La Roche-Posay Hyalu B5 Serum | QUA_NHO, bỏ qua |
| `r009.jpg` | 150×200 | 150×187 | r009 · DEMO-009 · La Roche-Posay Anthelios Mineral Tinted | QUA_NHO, bỏ qua |
| `r0010.jpg` | 150×200 | – | không ghép | KHONG_GHEP_DUOC |
| `r0010-1.jpg` | 150×200 | – | không ghép | KHONG_GHEP_DUOC |

Ngưỡng 600×750 là tối thiểu; kích thước đích là 800×1000. Ảnh hiện có nhỏ hơn ngưỡng khoảng 4 lần theo mỗi chiều.

### Ảnh bị bỏ qua và lý do

- **QUA_NHO (9 file, r001–r009):** sau khi cắt 4:5, vùng ảnh còn lại nhỏ hơn 600×750, theo ngưỡng owner đặt. Không phóng to.
- **KHONG_GHEP_DUOC: `r0010.jpg`.** Mã phải là `r` + đúng 3 chữ số. `r0010` có thể là `r001` thêm `0`, hoặc `r010` gõ thừa số 0. Theo quy tắc không đoán, file bị bỏ qua.
  - Nếu là Bioderma Atoderm Lip Balm (r010), đổi tên thành `r010.jpg`.
- **KHONG_GHEP_DUOC: `r0010-1.jpg`.** Cùng lý do về mã. Thêm nữa, hậu tố ảnh phụ là `-2`, `_2` hoặc ` 2`, không phải `-1`.
  - Nếu là ảnh phụ của r010, đổi tên thành `r010-2.jpg`.

## 3. Cập nhật dữ liệu

- **`docs/data/danh-sach-anh-san-pham.csv`:**
  - thêm hai cột cuối `nguon_anh`, `giay_phep` (để trống cho owner điền);
  - `trang_thai`: r001–r009 là `QUA_NHO`; 23 SKU còn lại (r010–r031, gồm hai SKU của r026) là `CHUA_CO_ANH`.
  - File vẫn UTF-8, CRLF, mọi ô được quote.
- **`docs/data/product-research.csv`: không đổi byte nào** (`git diff --quiet` sạch), vì chưa có ảnh chính nào được nạp.
  - `image_path` vẫn trống.
  - `image_source` / `image_license` giữ nguyên giá trị cũ ("BeautyShop CSS · minh họa chung").
  - Script chỉ chép `nguon_anh`/`giay_phep` sang hai cột này với SKU **đã nạp ảnh** và khi owner **đã điền đủ cả hai cột**. SKU có ảnh nhưng thiếu nguồn sẽ được liệt kê trong `sku_can_owner_dien_nguon` ở JSON.
- **Database demo:** đã chạy lại `scripts/seed-demo.ps1 -Database beautyshop_demo -User root -Client 'C:\Program Files\MariaDB 12.2\bin\mariadb.exe'`.
  - Kết quả: 31 sản phẩm, 32 SKU. Cả 32 SKU có `url_hinh_anh` NULL (placeholder); không có ảnh `/uploads/` nào của Admin để phải giữ.
  - Seed chỉ cập nhật `url_hinh_anh` khi trùng khóa, nên không đổi giá hay tồn.
  - Bằng chứng: `docs/qa/P20B/seed-demo.txt`.

### Ảnh cần owner điền nguồn/giấy phép

Hiện chưa có ảnh nào được nạp, nên chưa có dòng nào cần điền. Khi ảnh đạt ngưỡng, owner điền `nguon_anh` và `giay_phep` trong `danh-sach-anh-san-pham.csv` rồi chạy lại script.

## 4. Công cụ và quy trình (`scripts/process-product-images.py`)

- **Công cụ:**
  - Python 3.12 (`C:\Users\phamm\AppData\Local\Programs\Python\Python312\python.exe`) cùng Pillow 13.0.0 có hỗ trợ WebP, cài bằng `pip install --user pillow` sau khi owner đồng ý.
  - Python của MSYS2 không có pip nên không dùng.
- **Ghép tên:**
  - regex `^r(\d{3})(?![0-9])([-_ ]2(?![0-9]))?`, không phân biệt hoa thường;
  - `R001.JPG` và `r001_final.png` đều hiểu là r001; `-2`, `_2`, ` 2` là ảnh phụ;
  - `r0010…` không được hiểu là r001 hay r010;
  - một mã có nhiều ảnh chính hoặc nhiều ảnh phụ thì bỏ qua cả nhóm và liệt kê lại.
- **Xử lý:**
  - xoay theo EXIF Orientation;
  - ảnh trong suốt được đặt lên nền trắng;
  - cắt 4:5 quanh tâm, không bóp méo;
  - vùng cắt < 600×750 là `QUA_NHO`; < 800×1000 thì giữ cỡ và ghi cảnh báo; lớn hơn thì thu về 800×1000 (LANCZOS);
  - xuất WebP q80, giảm 5 mỗi bước tới tối thiểu 60 nếu > 300 KB; vẫn vượt thì ghi `QUA_DUNG_LUONG`;
  - không ghi EXIF/ICC/XMP.
- **Đầu ra:** `static/img/products/<id>.webp` và `<id>-2.webp`, chữ thường, khớp regex của `seed-demo.ps1`. Ảnh phụ chỉ lưu file, không ghi vào DB (để P25 dùng).
- **Tự kiểm tra bằng ảnh tổng hợp** (tạo trong bộ nhớ, không ghi vào repo), vì chưa có ảnh thật nào đạt ngưỡng:
  - 10/10 trường hợp tên file đúng như kỳ vọng;
  - ảnh 2400×1800 có EXIF máy chụp/GPS cho ra WebP 800×1000, tỉ lệ đúng 0.8, không EXIF, không ICC;
  - ảnh nhiễu, trường hợp nặng nhất, xuống q60 còn 305 702 B (≤ 307 200 B);
  - ảnh vuông trong suốt cho ra 800×1000 với nền trắng.

Cách chạy lại khi có ảnh mới:

```
C:\Users\phamm\AppData\Local\Programs\Python\Python312\python.exe scripts\process-product-images.py
.\scripts\seed-demo.ps1 -Database beautyshop_demo -User root -Client 'C:\Program Files\MariaDB 12.2\bin\mariadb.exe'
```

## 5. Kiểm tra

| Kiểm tra | Kết quả | Bằng chứng |
|---|---|---|
| Script xử lý ảnh trên 11 file thật | 0 xuất, 9 QUA_NHO, 2 KHONG_GHEP_DUOC | `docs/qa/P20B/process-images.txt`, `docs/qa/P20B/process-images.json` |
| `/products`, trang chủ (chương nổi bật + các khu sản phẩm P21), trang chi tiết `demo-r001` ở 1366 và 390; giỏ hàng ở 1366 | 7/7 PASS: placeholder hiển thị, không ảnh vỡ, không méo, không tràn ngang, 0 exception | `docs/qa/P20B/qa-browser.txt`, `docs/qa/P20B/results.json` |
| `curl` `/img/products/r001.webp` | 404 (đúng, vì chưa có file) | – |
| Tổng dung lượng `static/img/products/` | 0 B | – |
| `mvn -o test` (JDK 21.0.13) | 219 test, 0 lỗi, 38 skip | `docs/qa/P20B/mvn-test.txt` |
| `mvn -o -DskipTests package` | BUILD SUCCESS (lần đầu lỗi vì app 8081 đang khóa jar; đã dừng app, đóng gói lại rồi chạy lại app) | `docs/qa/P20B/mvn-package.txt` |

Ảnh chụp ở `docs/ui/screenshots/P20B/`:
- `products-1366.png`, `products-390.png`
- `detail-1366.png`, `detail-390.png`
- `home-1366.png`
- `cart-1366.png`

Tất cả đang hiển thị placeholder.

Log được lưu đuôi `.txt` vì `.gitignore` bỏ qua `*.log`.

## 6. File thay đổi

- Mới:
  - `scripts/process-product-images.py`
  - `docs/qa/P20B/` (script QA `product-images.qa.mjs`, kết quả, log)
  - `docs/ui/screenshots/P20B/`
  - báo cáo này
- Sửa:
  - `.gitignore` (BƯỚC 1: thêm `/assets-raw/`)
  - `docs/data/danh-sach-anh-san-pham.csv` (tạo ở BƯỚC 1, cập nhật trạng thái và thêm 2 cột ở BƯỚC 2)
- Không sửa Java, template, schema hay cơ chế upload Admin. `product-research.csv` không đổi.

## 7. Cần owner

1. **Ảnh lớn hơn:** ảnh gốc cần ít nhất 600×750, lý tưởng ≥ 800×1000, tỉ lệ dọc gần 4:5. Ảnh 150×200 thường là thumbnail lưu từ trang web.
2. **Đổi tên** `r0010.jpg` thành `r010.jpg` và `r0010-1.jpg` thành `r010-2.jpg`, nếu đúng là Bioderma Atoderm Lip Balm.
3. **Điền `nguon_anh` / `giay_phep`** cho các ảnh sẽ nạp, rồi chạy lại hai lệnh ở mục 4.
4. `src/main/resources/static/img/products/.gitkeep` đã bị xóa từ trước P20B (`git status` báo ` D`). Nếu commit khi thư mục còn rỗng, Git sẽ không giữ thư mục này. Tôi không khôi phục vì quy tắc không cho `git restore`.
