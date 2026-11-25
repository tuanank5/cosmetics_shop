# P21 – Danh sách sản phẩm trên trang chủ (Được yêu thích nhất + Tất cả sản phẩm)

Trạng thái: **xong, chưa commit/push**. Đang chờ owner xem.

## 1. Phạm vi đã làm

- Hai khu mới nằm **sau chương 6, trước footer**, ngoài Swiper và cuộn bình thường như footer. Thứ tự:
  1. `#home-popular` "Được yêu thích nhất": 5 thẻ. Lưới 5 cột từ 1100px, 2 cột ở 380–1099px, 1 cột dưới 380px.
  2. `#home-all-products` "Tất cả sản phẩm": 12 sản phẩm đầu theo sắp xếp mặc định của `/products` (Mới nhất). Lưới 4 cột từ 1100px, 2 cột ở 380–1099px, 1 cột dưới 380px.
     - Bộ chọn sắp xếp gọn gồm *Mới nhất / Giá tăng / Giá giảm*. Đây là liên kết GET `/?sort=…#home-all-products` có `aria-current`; giá trị lạ rơi về `newest`.
     - Nút "Xem tất cả sản phẩm" dẫn tới `/products`, hoặc `/products?sort=…` khi đang chọn sắp xếp giá.
     - Trang chủ không có bộ lọc đầy đủ.
- Neo "Thông tin cửa hàng ↓" ở chương 6 đổi thành `#home-products`. `#home-footer` vẫn được giữ để không phá các neo và test cũ.
- Luồng carousel đã nghiệm thu không đổi (1→6, `releaseOnEdges`, các fallback). Chỉ thêm một nhánh trong `home-carousel.js`:
  - khi trang mở bằng hash trỏ **ra ngoài** carousel (`#home-products`, `#home-all-products`, `#home-footer`), Swiper chờ ở chương 6 và trang cuộn tới đúng khu đó;
  - nhờ vậy, bấm sắp xếp vẫn giữ người đọc ở danh sách, và cuộn lên lại về chương 6.
- Khi JS tắt hoặc bật reduced motion, hai khu xuất hiện sau 6 chương theo luồng cuộn thường.
- Lỗi đọc dữ liệu: `Bootstrap_Controller.doc(...)` bắt `DataAccessException`, ghi `log.warn` và trả danh sách rỗng. Khu tương ứng bị ẩn (`th:unless`), trang chủ vẫn render (giống P16).
- Không đổi nghiệp vụ hay schema. Không thêm migration, chỉ dùng truy vấn đọc.

## 2. Thành phần gợi ý dùng chung

- `service/GoiYSanPham_Service` (interface): `List<TheSanPham> goiY(Long maNguoiDungHoacNull, int soLuong)`.
  - Template chỉ biết `TheSanPham`, nên P24 có thể thay bản cài đặt cá nhân hóa mà không phải sửa template hay controller.
  - Controller truyền `maNguoiDung` từ `PhienNguoiDung`, hoặc `null` với Guest.
- `service/GoiYPhoBien_Service` là bản cài đặt v1: sản phẩm phổ biến nhất, giống nhau cho mọi người.
  - `@Transactional(readOnly = true)`, nhận `Clock` để test.
  - Lấy ứng viên kèm số liệu, xếp hạng, rồi đọc thẻ sản phẩm (`CuaHang_Repository.timTheTheoMa`) và giữ đúng thứ tự xếp hạng.
- `recommendation/DiemPhoBien_Policy` chứa công thức thuần và các hằng số.
- `repository/GoiYSanPham_Repository` dùng JdbcClient, chỉ đọc.

### Công thức (hằng số trong `DiemPhoBien_Policy`)

```
diem  = TRONG_SO_DA_BAN    (0.5) × chuanHoa(số lượng đã bán)
      + TRONG_SO_DANH_GIA  (0.3) × (bayes − 1) / 4
      + TRONG_SO_YEU_THICH (0.2) × chuanHoa(số lượt yêu thích)
bayes = (C × m + tổng sao) / (C + số đánh giá),   C = HE_SO_BAYES_C = 5
m     = tổng sao / số đánh giá của toàn cửa hàng trong kỳ
chuanHoa(x) = (x − min) / (max − min) trong tập ứng viên; max = min ⇒ 0
Kỳ tính: SO_NGAY_TINH = 90 ngày (so với UTC, vì ngày tạo đơn và đánh giá lưu theo UTC)
```

- **Ứng viên** phải thỏa cả ba điều kiện:
  - sản phẩm đang bán, thuộc thương hiệu và danh mục đang hoạt động;
  - có ít nhất một SKU đang bán với `trong_kho − tam_giu > 0`;
  - (cùng điều kiện với `timNoiBat`).
- **Đã bán:** `SUM(chi_tiet_don_hang.so_luong)` của các đơn trong 90 ngày, trừ `CANCELLED`, `EXPIRED` và `PENDING_PAYMENT` (đơn MoMo chưa trả tiền chưa phải là đã bán).
- **Đánh giá:** chỉ tính đánh giá đang hiển thị (`hien_thi = TRUE`; bỏ đánh giá chờ duyệt) trong 90 ngày.
- **Yêu thích:** `san_pham_yeu_thich` không có cột ngày, nên đếm **toàn thời gian**. Không thêm cột vì task cấm đổi schema. Đây là sai lệch duy nhất so với "tính trên 90 ngày".
- **Chuẩn hóa Bayes:** dùng thang cố định `(B − 1)/4` (1★ → 0, 5★ → 1) thay vì min-max.
  - Lý do: điểm sao vốn có thang tuyệt đối. Min-max sẽ khuếch đại các chênh lệch rất nhỏ khi mọi sản phẩm đều quanh `m`.
  - Nếu cửa hàng chưa có đánh giá nào trong kỳ, thành phần này bằng 0 (`bayes = null`).
- **Hòa điểm:** sản phẩm có `ngay_tao` mới hơn đứng trước, sau đó đến `ma_san_pham` lớn hơn.
- **Cửa hàng mới** (không có bán, đánh giá hay yêu thích): trả về sản phẩm mới nhất (`timNoiBat`).

## 3. Top 5 trên `beautyshop_demo` (dữ liệu demo P20)

Kết quả từ `GoiYPhoBienDemoReport_Test` (chỉ đọc, bật bằng `P21_DEMO_REPORT=true`): 30 ứng viên, `m = 3.5625`. Bản gốc: `docs/qa/P21/popular-scores-demo.txt`.

| # | Sản phẩm | Đã bán (90 ngày) | Chuẩn hóa bán ×0.5 | Số ĐG / tổng sao | Bayes | ĐG 0–1 ×0.3 | Yêu thích | Chuẩn hóa YT ×0.2 | **Điểm** |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | demo-r026 | 16 | 1.0000 | 5 / 18 | 3.5813 | 0.6453 | 0 | 0 | **0.6936** |
| 2 | demo-r008 | 10 | 0.6250 | 2 / 10 | 3.9732 | 0.7433 | 0 | 0 | **0.5355** |
| 3 | demo-r021 | 10 | 0.6250 | 3 / 13 | 3.8516 | 0.7129 | 0 | 0 | **0.5264** |
| 4 | demo-r027 | 10 | 0.6250 | 1 / 4 | 3.6354 | 0.6589 | 0 | 0 | **0.5102** |
| 5 | demo-r029 | 9 | 0.5625 | 3 / 14 | 3.9766 | 0.7441 | 0 | 0 | **0.5045** |

- Danh sách trang chủ đúng thứ tự trên: `[demo-r026, demo-r008, demo-r021, demo-r027, demo-r029]`. Harness trình duyệt cũng thấy cùng thứ tự liên kết.
- Ví dụ về hiệu ứng Bayes: r008, r021 và r027 cùng bán 10 nhưng được xếp theo điểm sao đã kéo về `m`. r027 chỉ có 1 đánh giá 4★, nên Bayes 3.64 thấp hơn r008 (2 đánh giá, trung bình 5★, Bayes 3.97).
- Dữ liệu demo **không có lượt yêu thích nào**, nên thành phần 0.2 đang là 0 cho mọi sản phẩm. Thành phần này được kiểm bằng unit test và test MariaDB.

## 4. Kiểm thử

| Lệnh / harness | Kết quả | Bằng chứng |
|---|---|---|
| `mvn -o test` (JDK 21.0.13) | 219 test, 0 lỗi, 38 skip (các test MariaDB/demo chỉ chạy khi bật cờ) | `docs/qa/P21/mvn-test.log` |
| `mvn -o test` với `P01_MARIADB_TEST=true` trên DB trống `beautyshop_p21_it` (Flyway V1–V5), DB đã xóa sau khi chạy | 220 test, 0 lỗi, 1 skip (báo cáo demo) | `docs/qa/P21/mvn-test-mariadb.log` |
| `mvn -o -DskipTests package` | BUILD SUCCESS | `docs/qa/P21/mvn-package.log` |
| Bảng điểm trên `beautyshop_demo` | 5/5 | `docs/qa/P21/popular-scores-demo.txt` |
| Quét template/asset (`th:utext`, `style=`, handler, CDN) | không có | `docs/qa/P21/template-scan.txt` |
| QA trình duyệt trang chủ | **20/20 PASS**, 0 exception trên trang | `docs/qa/P21/qa-browser.log`, `docs/qa/P21/results.json` |
| Hồi quy giỏ hàng P17 | **41/41 PASS** | `docs/qa/P21/cart-regression/` |

Test mới:

- `recommendation/DiemPhoBienPolicyTest`:
  - bán nhiều nhưng đánh giá thấp bị sản phẩm cân bằng hơn vượt qua;
  - sản phẩm ít đánh giá bị kéo về `m`;
  - không có dữ liệu thì hòa điểm và xếp theo ngày tạo mới hơn;
  - tổng trọng số bằng 1 và mẫu số min-max bằng 0.
- `service/GoiYPhoBienServiceTest`: giữ thứ tự xếp hạng khi repository trả thẻ lộn xộn, cắt đúng số lượng, cửa hàng mới thì rơi về `timNoiBat`.
- `controller/TrangChuPagesTest` (thêm 3 test, sửa neo `#home-footer` thành `#home-products`):
  - hai khu nằm sau `</main>`, trước `#home-footer`, đúng thứ tự, đủ 5 và 12 thẻ, đúng liên kết sản phẩm;
  - liên kết sắp xếp và `aria-current` đúng; `sort` lạ rơi về `newest`;
  - danh sách rỗng hoặc lỗi dữ liệu thì khu đó ẩn và trang vẫn render.
- `integration/GoiYSanPham_MariaDb_Test` (MariaDB thật):
  - loại sản phẩm hết hàng và sản phẩm của thương hiệu đang tắt;
  - đã bán chỉ tính đơn không hủy trong 90 ngày;
  - bỏ đánh giá chờ duyệt và đánh giá quá 90 ngày;
  - đếm đúng lượt yêu thích; `timTheTheoMa` chỉ trả sản phẩm còn bán.
- `integration/GoiYPhoBienDemoReport_Test`: chỉ đọc, dùng để in bảng điểm cho báo cáo.

## 5. QA trình duyệt

Harness dùng Chrome headless qua DevTools protocol, chạy trên app 8081 với jar mới và `beautyshop_demo`. Kết quả: `docs/qa/P21/qa-browser.log`, `docs/qa/P21/results.json`.

| Kiểm tra | Kết quả |
|---|---|
| 360×800: fallback content-height, 1 cột | PASS, không tràn ngang, 5 + 12 thẻ, thứ tự stage → phổ biến → tất cả → footer |
| 390×844: fallback, 2 cột | PASS |
| 768×1024: carousel, 2 cột; cuộn bánh xe 1→6→sản phẩm→footer→6→1 | PASS / PASS |
| 1366×768: carousel, 5 + 4 cột; cuộn bánh xe như trên | PASS / PASS |
| 1920×1080: carousel, 5 + 4 cột; cuộn bánh xe như trên | PASS / PASS |
| 1366×768 zoom 200% (683 CSS px): fallback viewport, 2 cột | PASS |
| Neo "Thông tin cửa hàng ↓" ở chương 6 → `#home-products` | PASS (khu sản phẩm ở đỉnh màn hình) |
| Sắp xếp Giá tăng / Giá giảm / Mới nhất: URL `/?sort=…#home-all-products`, `aria-current`, giá đúng thứ tự, Swiper chờ ở chương 6, nút "Xem tất cả" mang theo `sort` | PASS ×3 |
| Cuộn lên từ danh sách đã sắp xếp về chương 6 | PASS |
| Tab từ chương 6 vào khu sản phẩm, không lọt vào chương ẩn (inert) | PASS (0 lần lọt) |
| Shift+Tab ra khỏi khu sản phẩm về chương 6 | PASS |
| Reduced motion: fallback, sản phẩm sau chương 6 theo luồng thường | PASS |
| JS tắt: sản phẩm sau 6 chương, trước footer, không tràn ngang | PASS |
| Hồi quy `/products?sort=priceAsc` | PASS |
| Thẻ phổ biến mở đúng trang chi tiết (`/products/demo-r026`) | PASS |
| Hồi quy giỏ hàng P17: 5 viewport, no-JS, +/−, gõ số, vượt tồn, Enter, xóa dòng | 41/41 PASS (`docs/qa/P21/cart-regression/`) |

Ảnh chụp (`docs/ui/screenshots/P21/`):
- `home-products-1366x768.png`, `home-products-768x1024.png`, `home-products-390x844.png`
- `home-all-products-1366x768.png`, `home-all-products-768x1024.png`, `home-all-products-390x844.png`
- `home-sort-priceAsc-1366.png`
- `home-products-keyboard-focus.png`
- `home-products-nojs-1366.png`

Ghi chú về harness: lượt chạy đầu có 2 FAIL giả ở 1366 và 1920, do đo vị trí `#home-advisor` trong khi Swiper dịch chuyển slide bằng transform. Phép đo đã đổi sang đáy `#home-stage` cộng thứ tự DOM. Hàm `go()` giờ luôn tải lại toàn trang, vì điều hướng lại đúng URL có hash không phát `loadEventFired`. Cả hai đều là lỗi của harness, không phải lỗi ứng dụng.

## 6. File thay đổi

Mới:
- `src/main/java/com/beautyshop/recommendation/DiemPhoBien_Policy.java`
- `src/main/java/com/beautyshop/repository/GoiYSanPham_Repository.java`
- `src/main/java/com/beautyshop/service/GoiYSanPham_Service.java`
- `src/main/java/com/beautyshop/service/GoiYPhoBien_Service.java`
- `src/test/java/com/beautyshop/recommendation/DiemPhoBienPolicyTest.java`
- `src/test/java/com/beautyshop/service/GoiYPhoBienServiceTest.java`
- `src/test/java/com/beautyshop/integration/GoiYSanPham_MariaDb_Test.java`
- `src/test/java/com/beautyshop/integration/GoiYPhoBienDemoReport_Test.java`
- `docs/qa/P21/home-products.qa.mjs` và `docs/qa/P21/cart-regression/cart-auto-update.qa.mjs`

Sửa (chỉ phần P21; các thay đổi P19/P20 chưa commit trong cùng file được giữ nguyên):
- `controller/Bootstrap_Controller.java`: thêm `sanPhamPhoBien`, `tatCaSanPham`, `sapXepTrangChu` và tham số `sort`.
- `repository/CuaHang_Repository.java`: thêm `timTheTheoMa` và `TheSanPhamCoMa`.
- `templates/home/index.html`: thêm hai khu và đổi neo chương 6.
- `static/css/storefront.css`: thêm khối `/* P21 … */` ở cuối file.
- `static/js/home-carousel.js`: thêm nhánh xử lý hash ngoài carousel.
- `test/.../controller/TrangChuPagesTest.java`

## 7. Chưa xác minh / ghi chú

- Lượt yêu thích tính toàn thời gian vì bảng không có cột ngày. Muốn giới hạn 90 ngày thì cần migration mới, việc này cần owner duyệt.
- Dữ liệu demo không có lượt yêu thích nên thành phần 0.2 chưa thể hiện trên trang thật.
- Danh sách phổ biến được tính lại mỗi lần tải trang chủ (một truy vấn tổng hợp trên khoảng 30 sản phẩm). Nếu catalog lớn, P24 nên thêm cache.
- Hồi quy giỏ hàng tạo một tài khoản khách QA dùng một lần trong `beautyshop_demo` (mật khẩu ngẫu nhiên chỉ giữ trong bộ nhớ), như ở P17/P18.
- Ảnh sản phẩm chưa có (placeholder). Ảnh sẽ được thêm ở P20B.
- `.gitignore` có `*.log`, nên các file `*.log` trong `docs/qa/P21/` không được Git theo dõi. `results.json`, `.txt` và ảnh vẫn được theo dõi. Cần owner quyết định.
- Chưa commit/push.
