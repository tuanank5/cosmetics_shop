# P17 – Giỏ hàng tự cập nhật khi đổi số lượng

Ngày: 2026-11-23 · Trạng thái: chờ owner xem, **chưa commit/push**.

## Vấn đề

Trước P17, bấm +/− chỉ đổi số trong ô số lượng. "Thành tiền", "Tạm tính hàng hóa" và badge giỏ giữ nguyên cho tới khi người dùng bấm "Cập nhật".

## Cách làm

Đây là progressive enhancement, phía server không đổi gì.

- **Gửi yêu cầu:** `static/js/cart.js` (nạp `defer`, chỉ trên `/cart`) gửi chính form hiện có bằng `fetch(form.action, { method: 'POST', body: new FormData(form), credentials: 'same-origin' })`. Payload gồm `quantity` và `_csrf` như trước.
- **Nhận kết quả:** server vẫn trả `302 → /cart`. `fetch` đi theo redirect, nhận HTML `/cart` mới, phân tích bằng `DOMParser`, rồi dùng `replaceWith(document.importNode(...))` để thay **các vùng do server render**:
  - `.cart-prices` và `.cart-stock` của đúng dòng vừa đổi;
  - `.cart-summary dl` (tạm tính);
  - link giỏ trên header (badge và `aria-label`).

  Không dùng `innerHTML`, và JS không tự tính tiền.
- **Giữ focus:** ô số lượng, nút +/− và form **không bị thay**, nên focus và giá trị người dùng đang gõ giữ nguyên. Chỉ `value` và `max` của ô được đồng bộ từ server.
- **Gom thao tác:** debounce 350 ms. Mỗi dòng chỉ có một request tại một thời điểm. Nếu người dùng bấm tiếp trong lúc chờ, giá trị mới nhất được gửi sau khi request trước xong. Số thứ tự request (`seq`) được kiểm tra khi áp kết quả.
- **Trạng thái đang xử lý:** dòng có `aria-busy="true"`. Chữ "Đang cập nhật…" luôn chiếm chỗ (`visibility`), nên dòng không nhảy layout.
- **Thông báo cho trình đọc màn hình:** vùng `aria-live="polite"` đọc "Đã cập nhật số lượng SKU X: n. Thành tiền …. Tạm tính …." Flash "Đã cập nhật số lượng" không hiện trong luồng JS. Flash cũ còn trên trang bị gỡ sau lần cập nhật đầu tiên.
- **Số lượng 0 (− ở 1, gõ 0, Enter với 0):** dùng hộp thoại xác nhận có sẵn (`data-zero-confirm`).
  - Hủy: ô trả về giá trị server, không gửi request, focus về nút đang thao tác.
  - Xác nhận: đi luồng POST + redirect thường (giống nút "Xóa"), nên giỏ trống hiển thị đúng khối "Giỏ hàng đang trống".
- **Nút "Xóa":** giữ nguyên luồng hiện tại.
- **Lỗi:**
  - Server báo lỗi (flash `alert--error` trong HTML trả về): thông điệp hiển thị ở `#error-<variant>`, `aria-invalid="true"`, ô trả về giá trị server đang lưu.
  - Vượt `max` phía client: giữ thông báo sẵn có của `app.js` ("Nhập số nguyên từ 0 đến N."), không gửi request.
  - Lỗi mạng, response không OK (403 CSRF…) hoặc URL cuối không phải `/cart` (ví dụ phiên hết hạn chuyển về `/login`): gửi form theo cách thường (`HTMLFormElement.prototype.submit`) để người dùng không bị kẹt.
- **Chặn submit:** `cart.js` bắt `submit` ở capture phase trên `document`, nên chạy trước handler chung của `app.js`. Enter trong ô số vì vậy cũng dùng fetch. Với giá trị 0, sự kiện được để đi tiếp tới `app.js` để hiện hộp thoại xác nhận.
- **Không có JS** (hoặc trình duyệt thiếu `fetch`/`DOMParser`): nút "Cập nhật" hiển thị, nút +/− ẩn, POST + redirect hoạt động như cũ.

Không thêm endpoint, dependency hay CDN. Không có `th:utext`, `style=` hay script/handler inline. Không đổi service, repository, bảo mật hay schema.

## File đã sửa / thêm

| File | Thay đổi |
|---|---|
| `AGENTS.md` | Thêm mục "Báo cáo và bằng chứng" (quy tắc lưu bằng chứng trong repo). |
| `src/main/resources/static/js/cart.js` | **Mới**: toàn bộ logic tự cập nhật. |
| `src/main/resources/templates/cart/index.html` | Thêm `data-cart-line` trên `<li>`, `<span class="cart-progress" aria-hidden="true">Đang cập nhật…</span>`, vùng `<p class="visually-hidden" aria-live="polite" data-cart-live>` và `<script defer src="/js/cart.js">`. Form, field, action và CSRF giữ nguyên. |
| `src/main/resources/static/css/storefront.css` | Thêm 3 rule `.cart-progress`: giữ chỗ, chỉ hiện khi `aria-busy`, ẩn hẳn khi không có JS. |
| `src/test/java/com/beautyshop/controller/GioHangPagesTest.java` | +2 test: markup tăng cường và form không JS vẫn đủ; POST số lượng vẫn PRG về `/cart`, thiếu CSRF bị 403. |
| `docs/qa/P17/cart-auto-update.qa.mjs` | Script QA: Chrome headless qua DevTools protocol, chỉ dùng Node, không cần npm. |

## Kiểm thử thật

### Maven (JDK 21.0.13 Temurin, không bật `P01_MARIADB_TEST`)

| Lệnh | Kết quả | Log |
|---|---|---|
| `mvn test` | **BUILD SUCCESS. Tests run 176, Failures 0, Errors 0, Skipped 34.** 34 test bị bỏ qua là test tích hợp MariaDB gắn cờ. | `docs/qa/P17/mvn-test.log` |
| `mvn -DskipTests package` | **BUILD SUCCESS** | `docs/qa/P17/mvn-package.log` |

### Trình duyệt (Chrome headless + app dev thật + MariaDB `beautyshop_demo`)

Cổng 8080 đang có app owner chạy từ IntelliJ nên tôi không đụng tới. QA chạy một instance riêng ở **8081** bằng `mvn spring-boot:run -Dspring-boot.run.profiles=dev --server.port=8081`, đọc `DB_*` từ biến môi trường user. Instance này đã được dừng sau khi kiểm.

Kết quả: **41/41 PASS** (`docs/qa/P17/results.json`, `docs/qa/P17/qa-run.log`).

Mỗi kịch bản chạy cho **Guest (giỏ session)** và **Customer (giỏ DB)**. Giỏ gồm 3 SKU: DEMO-001, DEMO-002 và DEMO-004 (tồn 1).

| Kiểm tra | Guest | Customer |
|---|---|---|
| Nút "Cập nhật" ẩn khi có JS | PASS | PASS |
| + 1 lần → số lượng, thành tiền, tạm tính, badge đổi; đúng 1 request | PASS | PASS |
| `aria-live` đọc đúng mẫu câu | PASS | PASS |
| Focus giữ trên nút + sau cập nhật | PASS | PASS |
| + 3 lần nhanh → **1 request**, ô số phản hồi ngay, giá trị cuối đúng | PASS | PASS |
| Dòng khác không đổi | PASS | PASS |
| Khớp server sau khi tải lại (số lượng, thành tiền, tạm tính, badge) | PASS ×2 | PASS ×2 |
| − về 1 | PASS | PASS |
| − ở 1 → hộp thoại; Hủy → về 1, 0 request, focus về nút − | PASS | PASS |
| Gõ số + Tab → lưu | PASS | PASS |
| Gõ vượt tồn (client `max`) → thông báo, 0 request | PASS | PASS |
| Vượt tồn phía server (nâng `max` giả lập) → lỗi trong `#error-<id>`, ô về giá trị server, `max` đồng bộ lại | PASS | PASS |
| + vô hiệu khi đạt tồn khả dụng (DEMO-004) | PASS | PASS |
| Bàn phím: Enter trên + → cập nhật, focus giữ | PASS | PASS |
| Enter trong ô số → cập nhật bằng fetch, không tải lại trang, focus giữ ở ô | PASS | PASS |
| − ở 1 → Xác nhận → dòng bị xóa (POST + redirect) | PASS | PASS |

| Khác | Kết quả |
|---|---|
| 360×800, 390×844, 768×1024, 1366×768, 1920×1080 | PASS: không cuộn ngang; chiều cao dòng và vùng điều khiển không đổi khi `aria-busy` |
| Tắt JS | PASS: nút "Cập nhật" hiện, nút +/− ẩn; gửi form → `/cart`, số lượng mới được lưu, flash "Đã cập nhật số lượng." |
| Đăng nhập Customer | PASS |

Ảnh chọn lọc nằm ở `docs/ui/screenshots/P17/`:
- `guest-after-plus-1366.png`
- `guest-busy-390.png`
- `guest-remove-confirm-1366.png`
- `guest-server-error-1366.png`
- `nojs-after-update-1366.png`

## Chưa kiểm chứng / ghi chú

- **Nhánh fallback do mạng hỏng, phiên hết hạn hoặc 403 CSRF** chưa được giả lập trên trình duyệt. Code đã xử lý; phía server, test MockMvc xác nhận POST thiếu CSRF trả 403.
- Chưa chạy trình đọc màn hình thật. Mới kiểm nội dung vùng `aria-live`.
- Kịch bản "vượt tồn phía server" nâng `max` bằng script để đi tới server. Trong thực tế, tình huống này xảy ra khi tồn giảm trong lúc trang đang mở.
- Nếu một dòng biến mất phía server giữa chừng (SKU bị gỡ khỏi giỏ), `cart.js` thay cả khối giỏ bằng bản server. Các form mới khi đó hoạt động theo kiểu không JS. Trường hợp này chưa được kiểm trên trình duyệt.
- **Dữ liệu QA còn lại trong `beautyshop_demo`:**
  - Mỗi lần chạy script tạo một tài khoản Customer `qa-p17-<ngẫu nhiên>@example.test`. Mật khẩu ngẫu nhiên, không ghi ra file. Script đã chạy 2 lần nên có 2 tài khoản.
  - Có thể còn dòng trong giỏ của các tài khoản này.
  - Không có thay đổi giá hay tồn. Không tạo đơn hàng.
- **Không phải thay đổi của P17:** `src/main/resources/application-dev.properties` đang có một dòng `/` thừa ở cuối file. Thay đổi này có sẵn trong working tree và tôi đã giữ nguyên.
