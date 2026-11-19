> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P05 — Hồ sơ, địa chỉ và quản lý tài khoản Admin

**Chủ trì:** B


- Trang hồ sơ Customer, quản lý địa chỉ giao hàng người nhận, SĐT, flag mặc định. DTO validation; chỉ owner xem/sửa/xóa.
- Trang Admin xem danh sách user, khóa/mở tài khoản (không xóa user đã có order), số trang tìm kiếm.
- Không tạo REST public CRUD user/JWT/token cho MVP; nếu cần AJAX thì CSRF+auth/session+DTO rõ ràng.
- Migration với FK user, test IDOR và thao tác khóa, ngăn role escalation qua form.
**Done:** quản lý user/address an toàn, các quyền được test.

## Quy định thực hiện
1. Đọc `AGENTS.md`, `docs/DECISIONS.md`, `docs/requirements/01-srs.md`, `docs/requirements/02-business-rules.md`, `docs/design/02-erd.md` và code hiện tại trước khi sửa.
2. Chỉ làm task này; không tự bổ sung tính năng P2, không đổi stack/phân quyền/trạng thái mà không có duyệt thay đổi.
3. Làm theo package-by-feature, dùng DTO/validation/constructor injection/BigDecimal; thêm migration phiên bản và test thực tế khi cần.
4. Kết thúc: liệt kê changed files, API/pages, `mvn test`/package PASS hoặc blocker, cách chạy thử, vấn đề còn tồn. Không giả lập bằng chứng.
5. Không hardcode secret, không dùng giá từ client, không gắn cứng ảnh mỹ phẩm chưa rõ quyền.
