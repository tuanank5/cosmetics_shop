> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P05 — Hồ sơ, địa chỉ và quản lý tài khoản Admin

**Chủ trì:** B


- Trang hồ sơ Customer, quản lý địa chỉ giao hàng người nhận, SĐT, flag mặc định. DTO validation; chỉ owner xem/sửa/xóa.
- Trang Admin xem danh sách user, khóa/mở tài khoản (không xóa user đã có order), số trang tìm kiếm.
- Không tạo REST public CRUD user/JWT/token cho MVP; nếu cần AJAX thì CSRF+auth/session+DTO rõ ràng.
- Migration với FK user, test IDOR và thao tác khóa, ngăn role escalation qua form.
**Done:** quản lý user/address an toàn, các quyền được test.

## Quy định thực hiện

Áp dụng [AGENTS.md](../../AGENTS.md) và chỉ làm trong phạm vi prompt này. Báo các trang hoặc API bị tác động, cách chạy thử và kết quả kiểm chứng.
