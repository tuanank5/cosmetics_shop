# P12 – Nâng cấp giao diện phong cách sang trọng (Luxury Beauty), ưu tiên UI/UX

> **Cách dùng:** dán TOÀN BỘ file này vào Codex, kèm một dòng cuối: `Chỉ làm GIAI ĐOẠN U1.` Khi U1 xong và bạn đã xem kết quả, dán lại file với `Chỉ làm GIAI ĐOẠN U2.`, rồi U3, U4. Đừng cho Codex làm cả 4 giai đoạn trong một lần.
> Đặt file này tại `docs/prompts/P12-luxury-ui-upgrade.md` trong repo.

---

## 0. Vai trò và đầu vào

Bạn là kỹ sư frontend senior cho dự án BeautyShop (website bán mỹ phẩm đa thương hiệu, đồ án môn học). Nhiệm vụ: **nâng cấp toàn bộ giao diện lên phong cách sang trọng, dễ dùng, nút bấm đẹp và đủ lớn**, trong thời gian rất ngắn.

Trước khi làm, đọc theo thứ tự:
1. `AGENTS.md`, `docs/requirements/`, `docs/design/` (nếu có).
2. `docs/reviews/frontend-audit-2026-11.md` – báo cáo audit frontend (24 phát hiện F01–F24). Giai đoạn nào cũng phải đối chiếu các phát hiện liên quan, ghi rõ mã F nào đã xử lý.
3. Toàn bộ `src/main/resources/templates/**`, `static/css/**`, `static/js/**` hiện có.

**Mốc thời gian:** hôm nay 22/11/2026; feature freeze 29/11; demo 03/12/2026. Làm đến đâu phải **chạy được và commit được** đến đó.

## 1. Phạm vi và những điều CẤM

**Được sửa:** Thymeleaf templates, CSS, JavaScript thuần (progressive enhancement), favicon/SVG do bạn tự tạo, tài liệu `docs/ui/**`.

**Được sửa Java ở mức tối thiểu, chỉ khi cần cho UI** và phải ghi rõ lý do trong báo cáo:
- bổ sung view-model chỉ đọc (ví dụ thành tiền từng dòng, tạm tính giỏ hàng, số kết quả tìm kiếm) – **không đổi chính sách giá, ship, voucher**;
- một `@ControllerAdvice`/error view cho lỗi 400/403/404/409/413/500 (F02, F24).

**TUYỆT ĐỐI KHÔNG:**
- đổi logic nghiệp vụ, trạng thái đơn/thanh toán, quy tắc hủy/đổi trả, phí ship, voucher, tồn kho;
- đổi URL/route, tên field form (`name=`), `th:action`, method, hay bỏ CSRF;
- sửa migration Flyway, schema, entity, cấu hình bảo mật (trừ khi chỉ để cho phép trang lỗi);
- thêm framework (React/Vue/Tailwind/Bootstrap), thư viện JS, hoặc **bất kỳ CDN nào**; không build step;
- dùng `th:utext`, `innerHTML`, inline `style=`, inline `<script>`, inline event handler (`onclick=`…);
- bịa nội dung: đánh giá khách hàng, chứng nhận, giải thưởng, số liệu "đã bán", cam kết hoàn tiền, logo hoặc ảnh của thương hiệu thật;
- đổi tên class Java/package (kể cả các class có dấu gạch dưới).

Nếu một yêu cầu UI buộc phải chạm vào mục bị cấm: **dừng, nêu rõ, đề xuất phương án**, chờ tôi duyệt.

## 2. Hệ thống thiết kế (áp dụng cho mọi giai đoạn)

### 2.1. Tinh thần
Sang trọng, tinh tế, "yên tĩnh": nhiều khoảng trắng, chữ serif cho tiêu đề, đường kẻ mảnh, màu trầm. Không rực rỡ, không đổ bóng nặng, không gradient sặc sỡ, không animation phô trương, không carousel tự chạy.

### 2.2. Design tokens (đặt ở `:root` trong `static/css/tokens.css`)
Giá trị dưới đây là **đề xuất khởi điểm**. Bạn phải tính contrast thật (WCAG 2.2) và chỉnh nếu không đạt, ghi bảng contrast vào `docs/ui/design-system.md`.

| Token | Gợi ý | Yêu cầu |
|---|---|---|
| `--color-bg` | `#FAF7F3` | nền trang (kem) |
| `--color-surface` | `#FFFFFF` | card, form |
| `--color-surface-alt` | `#F3ECE4` | khối phụ, nền hàng xen kẽ |
| `--color-ink` | `#2A1F1D` | chữ chính, ≥ 7:1 trên bg |
| `--color-muted` | `#6B5A55` | chữ phụ, ≥ 4.5:1 trên bg và surface |
| `--color-primary` | `#5E2F3E` (rượu vang/rosewood đậm) | nút chính; chữ trắng trên nền này ≥ 7:1 |
| `--color-primary-hover` | `#4A2330` | |
| `--color-accent` | `#B08D57` (champagne gold) | **chỉ dùng trang trí** (đường kẻ, viền, icon lớn, chữ trên nền tối); KHÔNG dùng làm chữ nhỏ trên nền sáng nếu < 4.5:1 |
| `--color-border` | `#E6DACE` | đường kẻ trang trí |
| `--color-control-border` | `#8A766F` | viền input/select/checkbox, **≥ 3:1** với nền kề (F04) |
| `--color-success` / `--color-error` / `--color-warning` | `#2F6B4F` / `#A12B3F` / `#8A5A00` | chữ ≥ 4.5:1; không dùng màu làm tín hiệu duy nhất (kèm icon/chữ) |
| `--color-footer-bg` | `#2A1F1D` | chữ kem trên nền này ≥ 7:1 |
| `--radius-sm/md/lg` | `4px / 8px / 14px` | nút dùng `--radius-sm`: góc vuông nhẹ tạo cảm giác cao cấp |
| `--space-1..8` | `4,8,12,16,24,32,48,72px` | thang khoảng cách duy nhất, không dùng số lẻ tùy tiện |
| `--shadow-soft` | `0 6px 24px rgba(42,31,29,.08)` | chỉ dùng cho card nổi/dropdown |
| `--focus-ring` | `3px solid #5E2F3E` + `outline-offset: 3px` | luôn thấy rõ trên mọi nền |

### 2.3. Chữ (typography)
- **Không tải font từ CDN.** Dùng font stack hệ thống, có fallback tốt:
  - tiêu đề: `"Cormorant Garamond", "Playfair Display", "Palatino Linotype", Palatino, Cambria, Georgia, serif`
  - nội dung/control: `"Be Vietnam Pro", system-ui, "Segoe UI", Roboto, sans-serif`
- Nếu muốn webfont: **chỉ** khi có file `.woff2` hợp lệ (giấy phép OFL, hỗ trợ tiếng Việt) đặt trong `static/fonts/` kèm `@font-face` + `font-display: swap`. **Không tự bịa file font.** Nếu không có, dùng stack hệ thống và nói rõ trong báo cáo.
- Cỡ chữ nội dung **≥ 16px** (tránh iOS tự zoom input). Thang: 14 / 16 / 18 / 22 / 28 / 36–56 (h1 dùng `clamp`).
- **Tiếng Việt có dấu:** `line-height` nội dung **≥ 1.55**, tiêu đề **≥ 1.2** (nhỏ hơn dễ cắt dấu). Không `overflow:hidden` làm cắt dấu.
- Số tiền VND dùng chung một formatter dạng `1.234.567 ₫` (F18), số dùng `font-variant-numeric: tabular-nums`.

### 2.4. NÚT BẤM (yêu cầu quan trọng nhất)
Tất cả nút/link dạng nút dùng chung hệ class `.btn`, không viết style nút riêng lẻ ở từng trang.

| Loại | Dùng cho | Kiểu |
|---|---|---|
| `.btn--primary` | hành động chính (Thêm vào giỏ, Đặt hàng, Đăng nhập, Lưu) | nền `--color-primary`, chữ trắng |
| `.btn--secondary` | hành động phụ (Xem chi tiết, Tiếp tục mua sắm) | viền 1.5px `--color-primary`, nền trong suốt |
| `.btn--ghost` / `.btn--link` | hành động nhẹ (Bỏ mã, Xem thêm) | chữ có gạch chân khi hover/focus |
| `.btn--danger` | hủy/xóa/khóa | viền và chữ `--color-error`, nền trong; hover nền đỏ nhạt; **luôn có bước xác nhận** |

**Kích thước (bắt buộc):**
- `.btn` mặc định: `min-height: 48px`, `min-width: 120px`, `padding-inline: 28px`, chữ 16px/600, `letter-spacing: .02em`, không xuống dòng giữa chữ ngắn.
- `.btn--lg` (CTA chính: Thêm vào giỏ, Đặt hàng, Thanh toán): `min-height: 56px`, chữ 17px.
- `.btn--sm`: tối thiểu `44px` cao trên cảm ứng; chỉ dùng trong hàng dày đặc của Admin. **Không có nút nào cao dưới 44px.**
- Mobile (≤ 640px): nút chính trong form/PDP/giỏ/checkout **rộng 100%**; nhóm nút xếp dọc.
- Khoảng cách giữa hai nút kề nhau **≥ 12px**; nút phá hủy không đặt sát nút chính.
- Icon-only button (nếu có) **≥ 44×44px** và có `aria-label` tiếng Việt cụ thể (ví dụ "Xóa Serum Vitamin C 30ml khỏi giỏ").

**Trạng thái (đủ cả 6):** mặc định, `:hover` (đổi nền/viền nhẹ, `transition` ≤ 180ms), `:focus-visible` (vòng focus rõ), `:active` (đậm hơn 1 bước), `:disabled`/`[aria-disabled=true]` (nhạt nhưng chữ vẫn đọc được, `cursor:not-allowed`), **trạng thái đang gửi** (`.is-loading`/`aria-busy=true`: hiện chữ "Đang xử lý…", chặn bấm lặp, giữ nguyên bề rộng để không nhảy layout) (F11).
Tôn trọng `prefers-reduced-motion`.

### 2.5. Form và control
- Input/select/textarea: `min-height: 48px`, chữ 16px, `padding: 12px 14px`, viền `--color-control-border`, `border-radius: --radius-sm`, nhãn **phía trên** ô, không dùng placeholder thay nhãn.
- Checkbox/radio: vùng bấm **≥ 44×44px** (bọc trong label, có thể dùng `accent-color`), không bị ép `height` của input văn bản (F23).
- Lỗi từng ô: có `id`, gắn `aria-describedby` + `aria-invalid="true"`, kèm icon/chữ "Lỗi:"; có **tóm tắt lỗi** đầu form (`role="alert"`, link nhảy tới ô lỗi) (F05).
- Gợi ý ngắn dưới ô (ví dụ mật khẩu ≥ 12 ký tự) đặt **trước** khi gửi.
- Ô mật khẩu: nút "Hiện/Ẩn" (JS tăng cường, vẫn dùng được khi tắt JS).
- Thông báo thành công/lỗi (flash): khối có icon + chữ, `role="status"` hoặc `role="alert"`.

### 2.6. Bố cục và responsive
- Container tối đa 1200px, lề ngang 16px (mobile) / 24px (tablet) / 32px (desktop). Lưới 4/8/12 cột bằng CSS Grid/Flex.
- Phải **không có cuộn ngang** ở 360×800, 390×844, 768×1024, 1366×768, 1920×1080.
- Điều hướng mobile: dùng `<details>`/`<summary>` hoặc nút bật tắt bằng JS tăng cường (không thư viện); **không giấu liên kết quan trọng**.
- Header: tên/logo chữ ở trái, ô tìm kiếm, menu chính, giỏ hàng có badge số lượng, khu tài khoản. Có **skip link** "Đến nội dung chính" (F06) và `aria-current="page"` cho mục hiện tại (F09).
- Footer nền tối, nhiều cột liên kết **thật** (Cửa hàng, Danh mục, Thương hiệu, Tài khoản, Đơn hàng, Tư vấn); chỉ đưa liên kết chính sách khi trang đó tồn tại, nếu chưa có thì ghi vào báo cáo thay vì bịa nội dung (F16).

### 2.7. Nguyên tắc UX chung
- Mỗi trang có **một hành động chính** nổi bật nhất.
- Trạng thái rỗng (giỏ, đơn, yêu thích, kết quả tìm) có câu giải thích và nút đi tiếp ("Khám phá sản phẩm").
- Thao tác phá hủy/thu tiền (hủy đơn, xóa địa chỉ, xóa/ngừng sản phẩm, khóa tài khoản, xác nhận giao và thu tiền) cần **bước xác nhận** nêu rõ đối tượng và hệ quả (F10). Làm bằng `data-confirm` + JS tăng cường dùng `<dialog>` có quản lý focus; khi tắt JS vẫn gửi được form (fallback an toàn: trang xác nhận hoặc `confirm()` không bắt buộc).
- Không tự điều hướng khi đổi một control (F03).
- Có phản hồi rõ ràng khi thành công/thất bại, giữ lại dữ liệu người dùng đã nhập khi có lỗi.
- Tất cả chữ giao diện bằng **tiếng Việt có dấu**, giọng lịch sự, ngắn gọn.

### 2.8. Cấu trúc CSS/Template
- `static/css/`: `tokens.css` → `base.css` → `components.css` (btn, form, card, badge, alert, pagination, dialog) → `storefront.css` → `admin.css`. Giữ thứ tự nạp rõ ràng, không `!important`, bỏ token và khai báo thừa/ghi đè (F17).
- Dùng fragment Thymeleaf cho: `head` (meta, title, CSS), `header`, `footer`, `flash`, `field-error`, `pagination`, `product-card`, `money` (định dạng VND). Mỗi trang vẫn **một `<h1>`**, `lang="vi"`, có `<main id="main">`.
- JS: một file `static/js/app.js` (vanilla, `defer`), mỗi tính năng độc lập, có kiểm tra phần tử tồn tại, **mọi chức năng phải dùng được khi tắt JS**.

## 3. Các giai đoạn

### GIAI ĐOẠN U1 – Nền tảng (làm đầu tiên)
Mục tiêu: hệ thống thiết kế + khung dùng chung, để các giai đoạn sau chỉ lắp ghép.
1. Tạo `tokens.css`, `base.css`, `components.css`; tạo/đổi các fragment dùng chung (mục 2.8).
2. Hệ nút `.btn` đủ loại, kích thước, trạng thái (mục 2.4); form control + lỗi accessible (mục 2.5); alert/flash; badge trạng thái; dialog xác nhận.
3. Header (skip link, `aria-current`, badge giỏ), footer, layout mobile.
4. **Trang lỗi** `templates/error/{400,403,404,409,413,500}.html` + `error.html` mặc định: tiếng Việt, đúng layout, có nút "Về trang chủ"/"Xem sản phẩm", không lộ stack/phiên bản máy chủ (F02). Nếu cần Java: chỉ `@ControllerAdvice` cho `MaxUploadSizeExceededException` trả lại form với lỗi cạnh ô ảnh (F24) và tắt banner server nếu có cấu hình hợp lệ.
5. Favicon SVG tự tạo (chữ cái/monogram, màu primary) và khai báo trong head (F22).
6. Viết `docs/ui/design-system.md`: token, bảng contrast tính thật, hướng dẫn dùng nút/form/fragment, kèm ví dụ HTML ngắn.
7. Áp dụng ngay hệ mới lên **trang đăng nhập, đăng ký, 404** làm mẫu kiểm chứng.

Nghiệm thu U1: build được, ba trang mẫu đúng thiết kế, mọi nút ≥ 44px (mặc định 48px), contrast đạt, focus thấy rõ khi Tab, trang lỗi tiếng Việt, test cũ vẫn xanh.

### GIAI ĐOẠN U2 – Luồng mua hàng (storefront)
1. **Trang chủ:** hero sang trọng (CSS thuần, nền kem + đường kẻ vàng champagne, tiêu đề serif lớn, 1 CTA chính + 1 CTA phụ), khối danh mục, khối thương hiệu, "Sản phẩm mới/nổi bật" từ dữ liệu active, khối cam kết **chỉ nêu điều có thật trong hệ thống** (giao hàng, đổi trả trong 7 ngày nếu đúng theo tài liệu nghiệp vụ). Xóa chữ "dự án đang phát triển"/"tên thương hiệu tạm thời" khỏi giao diện (F20).
2. **Danh sách sản phẩm:** bộ lọc bên trái (desktop) / thu gọn bằng `<details>` (mobile), chip bộ lọc đang áp dụng có nút bỏ, số kết quả, sắp xếp, phân trang đủ lớn; thẻ sản phẩm (ảnh tỉ lệ 4:5, thương hiệu, tên, giá "từ", nhãn "Hết hàng"), hover tinh tế; nếu chưa có ảnh thì placeholder CSS thanh lịch (monogram + màu theo danh mục), **không dùng ảnh bịa**.
3. **Chi tiết sản phẩm:** hai cột; cột mua hàng dính (sticky) trên desktop; chọn biến thể bằng **nhóm nút/pill liên kết** (mỗi pill là link `?variant=` hoặc nút submit rõ ràng, `aria-current` cho lựa chọn hiện tại), **KHÔNG tự submit khi đổi select** (F03); bộ chọn số lượng +/− lớn (≥ 44px) tăng cường bằng JS; nút "Thêm vào giỏ" `.btn--lg`; tab/accordion `<details>` cho Thành phần, Cách dùng, Loại da; khối đánh giá; sản phẩm liên quan nếu có sẵn dữ liệu. Mobile: thanh "Thêm vào giỏ" cố định đáy màn hình, không che nội dung.
4. **Giỏ hàng:** dòng sản phẩm dạng card (ảnh/placeholder, tên + liên kết, biến thể, đơn giá, bộ chỉnh số lượng, **thành tiền từng dòng**, nút xóa có `aria-label` cụ thể), cột tóm tắt có **tạm tính** và ghi chú phí ship xác nhận tại checkout; CTA "Tiến hành thanh toán". Dùng đúng hệ control, không dùng style mặc định của trình duyệt (F08, F23).
5. **Checkout:** chỉ báo bước (Giỏ hàng → Giao hàng & thanh toán → Hoàn tất) bằng danh sách có `aria-current`; hai cột (form | tóm tắt đơn); chọn địa chỉ/phương thức thanh toán bằng radio card lớn; ô voucher với nút "Áp dụng" và **"Bỏ mã"** riêng; khi POST lỗi **không được hiển thị bản xem trước giảm giá sai** – hiển thị rõ "Mã chưa được áp dụng/hết hiệu lực, vui lòng tính lại" hoặc tính lại preview hợp lệ (F07); tiêu đề trung lập "Thanh toán/Đặt hàng", không chỉ ghi COD (F19); nút đặt hàng có trạng thái đang xử lý, chống bấm lặp (F11).
6. Đơn hàng cũ/trang kết quả MoMo: giữ nguyên thông điệp an toàn, chỉ nâng cấp bố cục; trang mock MoMo phải có nhãn "MÔ PHỎNG – không thu tiền thật" nổi bật.

Nghiệm thu U2: đi được Guest → xem sản phẩm → chọn SKU → thêm giỏ → đăng nhập → checkout COD trọn vẹn; giá trên giao diện đúng với backend; không cuộn ngang ở 5 viewport; không nút nào < 44px.

### GIAI ĐOẠN U3 – Tài khoản, đơn hàng, tư vấn
1. **Khu tài khoản:** điều hướng dạng tab/sidebar nhất quán (Hồ sơ, Địa chỉ, Đơn hàng, Yêu thích, Đổi trả); form địa chỉ dùng textarea cho địa chỉ dài, checkbox "đặt làm mặc định" đúng kích thước (F23); xóa địa chỉ có xác nhận.
2. **Danh sách đơn:** card đơn (mã, ngày đặt, huy hiệu trạng thái, tổng tiền, nút "Xem chi tiết"); **chi tiết đơn:** tiến trình trạng thái dạng timeline đơn giản, tách rõ "trạng thái đơn" và "trạng thái thanh toán", nút Hủy đơn có xác nhận; đánh giá và đổi trả có bố cục rõ, khi không còn mục hợp lệ thì **vô hiệu hóa/ẩn nút gửi** (giữ kiểm tra server).
3. **Yêu thích:** thẻ sản phẩm có ảnh/placeholder, giá hiện hành, nút "Thêm vào giỏ", "Bỏ thích".
4. **Tư vấn (chat):** bố cục hỏi–đáp dạng bong bóng cho **một lượt**, **giữ lại câu hỏi** sau khi gửi/lỗi (F12), trạng thái đang xử lý, hiển thị nguồn FAQ và nhãn "Câu trả lời tham khảo", xử lý thông báo giới hạn tần suất (429) thân thiện; không streaming, không lưu lịch sử.
5. Đăng nhập/đăng ký: hoàn thiện nút Hiện/Ẩn mật khẩu, gợi ý mật khẩu, liên kết qua lại, giữ lại email khi sai.

Nghiệm thu U3: mọi trang sau đăng nhập cùng ngôn ngữ thiết kế, form lỗi đọc được bằng bàn phím và trình đọc màn hình, trạng thái rỗng đều có CTA.

### GIAI ĐOẠN U4 – Quản trị (Admin)
Ưu tiên **mật độ thông tin và thao tác chính xác**, không cầu kỳ.
1. Khung Admin riêng: sidebar (desktop) / menu gập (mobile), `aria-current`, tiêu đề trang rõ, breadcrumb ngắn.
2. Danh sách (danh mục, thương hiệu, sản phẩm, SKU, người dùng, voucher, đơn, đánh giá, đổi trả): dùng `<table>` có `<caption>`, `<th scope>` hoặc card xếp dọc trên mobile; cột trạng thái dùng huy hiệu (chữ + màu); hàng thao tác dùng `.btn--sm` (≥ 44px), nút phá hủy dùng `.btn--danger` + xác nhận nêu rõ tên đối tượng (F10).
3. Chi tiết đơn Admin: nút chuyển trạng thái tiếp theo nổi bật; "Xác nhận đã giao và thu tiền" là hành động nhạy cảm → xác nhận có mã đơn và số tiền.
4. Form sản phẩm/SKU/voucher: nhóm bằng `fieldset`/`legend`, hướng dẫn ngắn, **giá SKU hiển thị bằng formatter VND chung** (F18), ô upload ảnh có xem trước, giới hạn 2MB/2048px nêu rõ và lỗi hiển thị cạnh ô (F24).
5. Dashboard đơn: các ô thống kê đọc rõ; thêm bộ lọc trạng thái đơn nếu làm được mà không đổi backend lớn (nếu cần Java, **hỏi tôi trước**; không làm F13 đầy đủ trong đợt này).

Nghiệm thu U4: Admin thực hiện được toàn bộ luồng hiện có (CRUD catalog, SKU + ảnh, voucher, đơn, đánh giá, đổi trả, người dùng) với giao diện mới, mọi thao tác nhạy cảm có xác nhận.

## 4. Kiểm tra bắt buộc ở CUỐI MỖI GIAI ĐOẠN

1. Dùng **JDK 21** (`mvn -version` phải hiện Java 21). Chạy `mvn test` và báo **số test chạy/thành công/bỏ qua thật**. Không bật `P01_MARIADB_TEST` trên DB đang dùng. Nếu test cũ fail vì đổi chữ giao diện có chủ đích thì cập nhật test và nêu rõ; nếu fail vì lý do khác thì sửa lỗi.
2. Chạy app với dữ liệu demo (`scripts/seed-demo.ps1` trên **DB demo riêng**, không dùng DB production).
3. Nếu có trình duyệt: chụp màn hình các trang đã làm ở **360×800, 390×844, 768×1024, 1366×768, 1920×1080**, lưu `docs/ui/screenshots/<giai-đoạn>/…` kèm URL, vai trò, viewport, thời gian; kiểm không cuộn ngang, Tab qua toàn trang, zoom 200%, chạy axe-core/Lighthouse nếu có.
4. **Nếu không có trình duyệt:** ghi rõ "chưa kiểm chứng bằng trình duyệt", KHÔNG khẳng định đã đẹp/đạt AA; thay bằng `docs/ui/QA-checklist.md` để người thật kiểm tra.
5. Rà soát tự động bằng lệnh (grep): không còn `th:utext`, `style=`, `onclick=`, `<script>` inline, URL CDN, mỗi trang đúng một `<h1>`.
6. Commit nhỏ, rõ ràng, ví dụ `feat(ui): button system and design tokens`, `fix(ui): cart line totals (F08)`.

## 5. Báo cáo cuối mỗi giai đoạn
Tạo/cập nhật `docs/reviews/ui-upgrade-2026-11.md` gồm:
- Danh sách file đã tạo/sửa (nhóm theo templates, CSS, JS, Java, docs).
- Bảng đối chiếu **mã F (F01–F24) → trạng thái** (Đã xử lý / Một phần / Chưa / Ngoài phạm vi) kèm bằng chứng và nhãn S (source) / H (HTTP) / B (browser).
- Kết quả test thật (số lượng, lệnh đã chạy), phần **chưa kiểm chứng**.
- Những việc phải hỏi/duyệt (chạm Java, nội dung chính sách, ảnh, font).
- Hướng dẫn chạy lại.

## 6. Tiêu chí nghiệm thu tổng (Definition of Done)
- [ ] Mọi nút/link dạng nút dùng hệ `.btn`; mặc định cao ≥ 48px, CTA chính ≥ 56px, không nút nào < 44px; mobile CTA chính rộng 100%.
- [ ] Đủ trạng thái hover/focus-visible/active/disabled/đang xử lý cho nút và control.
- [ ] Viền control ≥ 3:1, chữ thường ≥ 4.5:1 (đã tính, ghi trong tài liệu).
- [ ] Form: nhãn, lỗi gắn `aria-describedby`, tóm tắt lỗi, giữ dữ liệu khi lỗi.
- [ ] Skip link, `aria-current`, một `<h1>`/trang, `lang="vi"`, Tab/Shift+Tab dùng được toàn site.
- [ ] Không có điều hướng bất ngờ khi đổi select; mọi thao tác phá hủy/thu tiền có xác nhận.
- [ ] Giỏ/checkout rõ ràng: thành tiền, tạm tính, voucher có Áp dụng/Bỏ mã, trạng thái đang xử lý.
- [ ] Trang lỗi 400/403/404/409/413/500 tiếng Việt, có lối thoát, không lộ phiên bản server.
- [ ] Trang chủ không còn chữ "đang phát triển"; không có nội dung bịa.
- [ ] Không có cuộn ngang ở 5 viewport (nếu đã kiểm chứng) hoặc ghi rõ chưa kiểm chứng.
- [ ] Không thêm framework/CDN/inline script; JS tắt vẫn dùng được các luồng chính.
- [ ] `mvn test` xanh với JDK 21; không thay đổi nghiệp vụ, URL, tên field, CSRF.

## 7. Ngoài phạm vi (không làm trong P12)
Phân trang/lọc đầy đủ cho mọi danh sách Admin (F13), CRUD FAQ (F15), tối ưu ảnh nhiều kích thước (F14), cache static (F21), triển khai host công khai, tích hợp MoMo/Ollama thật, đổi tên class Java.

---
**Bắt đầu bằng cách đọc các tài liệu ở mục 0, liệt kê ngắn gọn kế hoạch cho giai đoạn được yêu cầu, rồi thực hiện từng bước nhỏ, build sau mỗi bước.**
