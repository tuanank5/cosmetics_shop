# P16 – Sản phẩm nổi bật trên trang chủ và ảnh sản phẩm

Ngày: 2026-11-23 · Trạng thái: chờ owner xem, **chưa commit/push**.

## Phạm vi

- **A.** Thêm dải "Sản phẩm nổi bật" trên trang chủ, ngay trên footer. Dải này dùng lại fragment `product-card`.
- **B.** Placeholder ảnh trông có chủ đích. Ảnh thật hiển thị được và ảnh lỗi/404 rơi về placeholder. Ảnh demo đặt trong JAR (`static/img/products/`) để không mất khi redeploy (mục B.2 của prompt cập nhật).

Không đổi schema (không có V6, không có cờ `noi_bat`), business logic, bảo mật, COD/MoMo, giỏ/checkout, route, CSRF hay tên field form. Không thêm dependency, CDN, ảnh hay dữ liệu bịa.

## File đã sửa / thêm

| File | Thay đổi |
|---|---|
| `src/main/java/com/beautyshop/repository/CuaHang_Repository.java` | Thêm `timNoiBat(Pageable)`: JPQL chỉ đọc, trả về projection sẵn có `TheSanPham`. |
| `src/main/java/com/beautyshop/service/CuaHang_Service.java` | Thêm `sanPhamNoiBat(int)`, giới hạn 1–8, `readOnly` theo lớp. |
| `src/main/java/com/beautyshop/controller/Bootstrap_Controller.java` | `GET /` đưa `sanPhamNoiBat` vào model. Service lấy qua `ObjectProvider`. Nếu thiếu service hoặc gặp `DataAccessException` thì trả về danh sách rỗng và section tự ẩn, trang chủ vẫn render. |
| `src/main/resources/templates/home/index.html` | Thêm `<section id="home-featured">` sau `#home-stage` và trước `#home-footer`. Ba thumbnail `<img>` 80×100 trong `.story-spotlight` được thay bằng monogram `.story-spotlight-mark` (`aria-hidden`). |
| `src/main/resources/static/css/storefront.css` | Style thẻ catalog đổi selector từ `.shop-catalog` sang `:is(.shop-catalog, .home-featured)`. Specificity không đổi. Thêm style placeholder, monogram và lưới nổi bật. |
| `src/test/java/com/beautyshop/controller/GiaoDienPagesTest.java` | +2 test: hiển thị và thứ tự section, đường dẫn PDP, escape, ảnh/placeholder; ẩn khi rỗng hoặc lỗi catalog. |
| `src/test/java/com/beautyshop/repository/CuaHangNoiBatRepositoryTest.java` | Test mới `@DataJpaTest` trên H2 chạy thật truy vấn `timNoiBat`. |
| `src/main/java/com/beautyshop/repository/GioHang_Repository.java` | Regex `MucGio.ANH_SKU` chấp nhận thêm `/img/products/<tên an toàn>.(webp\|jpg\|png)`. Mẫu `/uploads/sku/<uuid>.(jpg\|png)` giữ nguyên. |
| `src/main/resources/templates/cart/index.html` | `<object type>` trả `image/webp` cho file `.webp` (trước đây mọi file không phải `.png` đều khai báo là `image/jpeg`). |
| `src/main/resources/static/img/products/.gitkeep` | Thư mục ảnh demo trong JAR, hiện **chưa có ảnh**. |
| `docs/data/product-research.csv` | Thêm cột cuối `image_path`, để trống ở cả 32 dòng. Giữ CRLF và quoting. |
| `scripts/seed-demo.ps1` | Hàm `ImageSql` kiểm tra `image_path` rồi ghi vào `bien_the_san_pham.url_hinh_anh`. |
| `docs/data/README.md` | Hướng dẫn quy ước ảnh demo. |
| `src/test/java/com/beautyshop/repository/GioHangRepositoryTest.java` | +15 case: URL ảnh hợp lệ và các URL bị loại (`..`, chữ hoa, `.gif`, query, thư mục con, URL ngoài, `javascript:`…). |
| `src/test/java/com/beautyshop/controller/GioHangPagesTest.java` | +1 test: ảnh `.webp`/`.jpg` demo trong giỏ có MIME đúng. |

## Truy vấn nổi bật

`san_pham` không có cột "nổi bật", nên dải này lấy **tối đa 8 sản phẩm mới nhất** thỏa các điều kiện sau:

- sản phẩm, thương hiệu và danh mục đều `dang_hoat_dong = true`;
- có ít nhất một SKU active với `so_luong_trong_kho - so_luong_tam_giu > 0` (`HAVING max(...) > 0`);
- sắp xếp theo `ngay_tao desc, ma_san_pham desc`, giống sort `newest` của `/products`.

Trường giá và ảnh tính giống `/products`: `giaTu = min(gia_ban)` và `urlHinhAnh = min(url_hinh_anh)` trên các SKU active. Đề bài yêu cầu 6–8 sản phẩm, nên giới hạn được đặt là 8. Nếu chỉ có ít hơn 8 sản phẩm hợp lệ thì dải hiện đúng số đó, và rỗng thì ẩn.

## Cách đặt section để không đụng carousel

- Section nằm **ngoài** `<main data-home-carousel>` và ngoài `#home-stage`. Swiper chỉ đếm 3 `.home-slide` (`home-carousel.js` yêu cầu `slides.length === 3`), và `mousewheel.eventsTarget` là `#home-stage`. Vì vậy dải mới thuộc luồng cuộn native giống footer trước đây, và **không có thay đổi JS nào**.
- Link "Thông tin cửa hàng ↓" của chương 3 vẫn trỏ `#home-footer`. Anchor này nằm ngay sau dải nổi bật, sát footer.
- Section có `aria-labelledby` (landmark region) và `h2` serif. Trang vẫn chỉ có một `<h1>`.

## Ảnh sản phẩm

- Luồng hiện tại đã đúng nên không sửa: thẻ dùng `the.urlHinhAnh`, ảnh SKU phục vụ qua `GET /uploads/sku/{filename}` (`AnhBienTheSanPham_Controller`, permitAll ở `Security_Config`), và `app.js` bắt `error` hoặc `naturalWidth === 0` để ẩn `<img>` và hiện `[data-catalog-placeholder]`. Fragment không phát ra `src=""` khi URL rỗng.
- Placeholder thẻ (trong catalog và trang chủ) có nền `--color-surface-alt`, khung gold mảnh lùi vào 12px, monogram "BeautyShop" serif màu gold và nhãn "CHƯA CÓ ẢNH SẢN PHẨM" nhỏ. Thay đổi này áp dụng cả cho `/products`, theo yêu cầu "placeholder thẻ sản phẩm".
- Thumbnail trong 3 chương trước đây là SVG 1600×1000 cắt `cover` về 64×80, chỉ còn một ô gần như đen. Nay chúng là ô monogram "BS" (nền `--color-surface-alt`, viền gold). Ảnh nền `nocturne/form/light.svg` của từng chương giữ nguyên. Ở viewport ≤ 340px, monogram này ẩn đi để spotlight xếp dọc không bị chật.

## Ảnh demo trong JAR (B.2)

- **Vị trí:** `src/main/resources/static/img/products/`, công khai qua `/img/**` (đã `permitAll`). Không thêm route mới, cơ chế upload của Admin không đổi.
- **Cột ảnh:** `san_pham` không có cột ảnh, nên seed chỉ ghi `bien_the_san_pham.url_hinh_anh`. Thẻ sản phẩm, PDP và giỏ hàng đều đọc ảnh từ SKU.
- **Kiểm tra trong `seed-demo.ps1`:**
  - Ô trống thì ghi `NULL`.
  - Đường dẫn phải khớp `^/img/products/[a-z0-9](?:[a-z0-9-]{0,98}[a-z0-9])?\.(?:webp|jpg|png)$` (phân biệt hoa thường).
  - File phải tồn tại trong thư mục ảnh và có kích thước ≤ 300 KB.
  - Vi phạm bất kỳ điều kiện nào thì seed dừng với lỗi rõ ràng, tránh âm thầm ghi URL hỏng.
- **Chạy lại seed:** `ON DUPLICATE KEY UPDATE` chỉ cập nhật `url_hinh_anh` khi giá trị đang `NULL` hoặc đang là `/img/products/...`. Ảnh Admin đã upload (`/uploads/sku/...`) không bị đè. Giá và tồn của SKU đã có vẫn không đổi, giống trước.
- **Rà các nơi hiển thị ảnh:**

  | Nơi | Cách lấy ảnh | Kết quả với `/img/products/...` |
  |---|---|---|
  | Thẻ sản phẩm (`product-card`) | `th:src` | đã hiển thị được, không cần sửa |
  | PDP (`product/detail`) | `th:src` | đã hiển thị được, không cần sửa |
  | Danh sách/form SKU Admin | `th:src` | đã hiển thị được, không cần sửa |
  | Giỏ hàng | lọc qua regex `GioHang_Repository.MucGio` | **đã sửa**: trước đây bị loại về `null` |
  | Wishlist, chi tiết đơn | — | không hiển thị ảnh |

- **Admin thay hoặc xóa ảnh của SKU đang dùng ảnh demo:** `AnhBienTheSanPham_Service.xoa` chỉ xóa URL có tiền tố `/uploads/sku/`, nên file trong JAR không bị đụng tới.
- **Kiểm offline hàm `ImageSql`** (PowerShell, không chạm DB): ô trống → `NULL`; `/img/products/r001.webp` → được nhận; file không tồn tại, file 301 KB, `../`, chữ hoa, `/uploads/sku/...` và chuỗi có `'` → đều bị dừng. CSV sau khi sửa đọc bằng `Import-Csv` ra 32 dòng và có cột `image_path`. Seed script parse không lỗi.

## Kiểm thử thật (JDK 21.0.13, Temurin)

| Lệnh | Kết quả |
|---|---|
| `mvn test` (không bật `P01_MARIADB_TEST`) | **BUILD SUCCESS. Tests run 174, Failures 0, Errors 0, Skipped 34.** 34 test bị bỏ qua là các test tích hợp MariaDB gắn cờ. Trước P16 là 157 test. |
| `mvn -DskipTests package` | **BUILD SUCCESS**, tạo `target/beautyshop-0.0.1-SNAPSHOT.jar` |

### Kiểm tra trình duyệt (headless Chrome qua CDP)

Môi trường không có `DB_PASSWORD`, nên **chưa chạy được app dev với MariaDB đã seed**. Thay vào đó:

1. Render `GET /` bằng MockMvc với 8 sản phẩm fixture: thẻ 1 có ảnh hợp lệ, thẻ 2 có ảnh trả 404, các thẻ còn lại không có ảnh. Test tạm dùng cho việc này đã xóa.
2. Phục vụ HTML đó cùng `src/main/resources/static` thật (CSS, JS, Swiper, GSAP) qua server tĩnh cục bộ.

| Viewport | Cuộn ngang | Carousel | Cột lưới | Ảnh thật / ảnh 404 |
|---|---|---|---|---|
| 360×800 | không | bật | 1 | hiện / placeholder |
| 390×844 | không | bật | 1 | hiện / placeholder |
| 768×1024 | không | bật | 2 | hiện / placeholder |
| 1366×768 | không | bật | 4 | hiện / placeholder |
| 1920×1080 | không | bật | 4 | hiện / placeholder |
| 1366×768 zoom 200% (683×384) | không | fallback native (`viewport-or-css`, đã có trước P16) | 2 | hiện / placeholder |

- **Luồng wheel** ở cả 5 viewport (`slide@scrollY`): `2@0 3@0 3@400 3@800 3@1200 3@800 3@400 3@0 2@0 1@0`. Tức là 1→2→3→cuộn native xuống dải nổi bật và footer→lên lại 3→2→1, giữ nguyên hành vi cũ.
- Ở mọi viewport, dải nổi bật nằm trước `.site-footer`. Focus vào link thẻ đầu tiên cho kết quả `/products/san-pham-0`, nhìn thấy được.
- **Tắt JS** (390px): dải hiện đủ 8 thẻ, link hoạt động, không cuộn ngang.
- **Reduced motion** (1366px): carousel fallback `reduced-motion`, dải vẫn hiện 8 thẻ, 4 cột.
- Rà template trang chủ: không có `th:utext`, `style=`, script inline, handler inline hay CDN. URL `http://` duy nhất là namespace `xmlns:th`.

## Chưa kiểm chứng / giới hạn

- **Chưa xác minh trên MariaDB thật với dữ liệu demo đã seed**, do thiếu `DB_PASSWORD` trong môi trường. JPQL `timNoiBat` mới chỉ chạy thật trên H2 (MODE mặc định, schema sinh từ entity). Owner nên chạy `.\scripts\p00.ps1 -Task run`, mở `/` và xác nhận dải hiện 6–8 sản phẩm demo, đồng thời link PDP đúng.
- **Chưa có ảnh sản phẩm thật:** `static/img/products/` và `uploads/` đều trống, mọi `image_path` đang để trống, và CSV ghi nguồn ảnh là "Minh họa CSS". Vì vậy toàn bộ thẻ sẽ hiện placeholder. Ảnh "hợp lệ" trong lần kiểm thử chỉ là một ảnh chụp màn hình có sẵn trong `docs/`, dùng thay cho ảnh sản phẩm. Khi nhóm có ảnh được phép dùng: bỏ file vào `static/img/products/`, điền `image_path` và chạy lại `seed-demo.ps1`.
- Chưa kiểm chứng `seed-demo.ps1` trên MariaDB thật, cũng như việc Spring trả `Content-Type: image/webp` cho file `.webp` trong JAR.
- **Khi tắt JS**, ảnh trả 404 không tự chuyển sang placeholder, vì cơ chế này dựa vào `app.js` (đã có từ trước, không đổi). Ô ảnh khi đó hiện nền `#151518` trống, không có icon vỡ vì `alt=""`.
- Chưa chạy lại regression trình duyệt cho `/products` filter/sort/paging và checkout/voucher. Phần này chỉ được phủ bởi các test MockMvc hiện có, và toàn bộ đều PASS. Thay đổi CSS duy nhất ảnh hưởng `/products` là style placeholder.
