> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P11 — Voucher toàn đơn và lượt sử dụng an toàn

**Chủ trì:** B


- CRUD voucher Admin: code unique, FIXED/PERCENT, minSubtotal, maxDiscount, usageLimit, usedCount, reservedCount, startsAt/endsAt, active.
- Một voucher/Order. PERCENT bounded, FIXED bounded bởi subtotal; mọi giá BigDecimal. Phí ship 25k/miễn từ 500k **sau giảm**.
- Online checkout giữ một lượt atomic; PAID consume; hủy/expire release; COD consume ngay khi tạo order; không duplicate trên IPN hủy. Update có điều kiện/row lock, không vượt lượt khi concurrency.
- Contract với C: `quoteVoucher`, `holdVoucher`, `consumeHold`, `releaseHold` hoặc tên thống nhất. Tests limit 1 khi 2 checkout, expired, minimum, discounts, redundant release.
**Done:** voucher tính chính xác, không oversubscribe, E2E với OrderService.

## Quy định thực hiện
1. Đọc `AGENTS.md`, `docs/DECISIONS.md`, `docs/requirements/01-srs.md`, `docs/requirements/02-business-rules.md`, `docs/design/02-erd.md` và code hiện tại trước khi sửa.
2. Chỉ làm task này; không tự bổ sung tính năng P2, không đổi stack/phân quyền/trạng thái mà không có duyệt thay đổi.
3. Làm theo package-by-feature, dùng DTO/validation/constructor injection/BigDecimal; thêm migration phiên bản và test thực tế khi cần.
4. Kết thúc: liệt kê changed files, API/pages, `mvn test`/package PASS hoặc blocker, cách chạy thử, vấn đề còn tồn. Không giả lập bằng chứng.
5. Không hardcode secret, không dùng giá từ client, không gắn cứng ảnh mỹ phẩm chưa rõ quyền.
