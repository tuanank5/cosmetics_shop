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

Áp dụng [AGENTS.md](../../AGENTS.md) và chỉ làm trong phạm vi prompt này. Báo các trang hoặc API bị tác động, cách chạy thử và kết quả kiểm chứng.
