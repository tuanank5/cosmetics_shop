> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P11 — Voucher toàn đơn và lượt sử dụng an toàn

**Chủ trì:** B


- CRUD voucher Admin: code unique, FIXED/PERCENT, minSubtotal, maxDiscount, usageLimit, usedCount, reservedCount, startsAt/endsAt, active.
- Một voucher/Order. PERCENT bounded, FIXED bounded bởi subtotal; mọi giá BigDecimal. Phí ship 25k/miễn từ 500k **sau giảm**.
- Online checkout giữ một lượt atomic; PAID consume; hủy/expire release; COD consume ngay khi tạo order; không duplicate trên IPN hủy. Update có điều kiện/row lock, không vượt lượt khi concurrency.
- Contract với C: `quoteVoucher`, `holdVoucher`, `consumeHold`, `releaseHold` hoặc tên thống nhất. Tests limit 1 khi 2 checkout, expired, minimum, discounts, redundant release.
**Done:** voucher tính chính xác, không oversubscribe, E2E với OrderService.

## Quy định thực hiện

Áp dụng [AGENTS.md](../../AGENTS.md) và chỉ làm trong phạm vi prompt này. Báo các trang hoặc API bị tác động, cách chạy thử và kết quả kiểm chứng.
