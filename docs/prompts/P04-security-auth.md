> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P04 — Form Login + Session Security / Customer

**Chủ trì:** B


- Thay `BootstrapUserDetailsService` bằng UserRepository-backed UserDetailsService; bcrypt, login/logout bằng Form Login + Session; CSRF bật.
- Register customer bằng DTO validation, email UNIQUE, không cho client chọn ADMIN; admin bootstrap qua thao tác an toàn cấu hình ngoài code hoặc migration có quy trình (không password demo cố định).
- Guest được `GET /cart`/`POST /cart/**` trong Session; `/checkout/**`, `/orders/**`, `/account/**` chỉ CUSTOMER; `/admin/**` chỉ ADMIN; đường đọc catalog public.
- Khi login, merge giỏ Session -> DB **đúng một lần**, bảo đảm không vượt tồn, tách logic CartService với P06; thống nhất contract trước sửa.
- Chống IDOR: service kiểm sở hữu order; CSRF test và logout; không JWT/web token tự tạo.
- Route public: `POST /payment/momo/ipn` exempt CSRF **riêng route** (chỉ sau khi có HMAC verify ở P07); `GET /payment/momo/return` public; `/uploads/**` chỉ cho ảnh công khai đã kiểm MIME và tên an toàn. Không miễn CSRF toàn ứng dụng.
**Done:** Guest / Customer / Admin đúng quyền; thử login/logout/register/403/redirect, test account ownership.

## Quy định thực hiện
1. Đọc `AGENTS.md`, `docs/DECISIONS.md`, `docs/requirements/01-srs.md`, `docs/requirements/02-business-rules.md`, `docs/design/02-erd.md` và code hiện tại trước khi sửa.
2. Chỉ làm task này; không tự bổ sung tính năng P2, không đổi stack/phân quyền/trạng thái mà không có duyệt thay đổi.
3. Làm theo package-by-feature, dùng DTO/validation/constructor injection/BigDecimal; thêm migration phiên bản và test thực tế khi cần.
4. Kết thúc: liệt kê changed files, API/pages, `mvn test`/package PASS hoặc blocker, cách chạy thử, vấn đề còn tồn. Không giả lập bằng chứng.
5. Không hardcode secret, không dùng giá từ client, không gắn cứng ảnh mỹ phẩm chưa rõ quyền.
