> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P10 — Luxury UI, data seed, QA, deploy, báo cáo

**Chủ trì:** D / cả nhóm


- Đồng bộ kích thước ảnh (tỉ lệ nhất quán), fonts, colors, nav, cards, button 40–48px target trên mobile, form errors, skeleton/empty states. Không dùng font chưa được cấp phép.
- Tạo 30–50 sản phẩm có source_url/reviewed_at/ảnh có quyền theo `docs/data/README.md` rồi seed dev, không dán đại hình ảnh thương hiệu không có license.
- Chạy TC01..TC20 với evidence PASS/FAIL/BLOCKED, thay vì mô tả đã pass. Chuẩn bị screenshot, E2E COD bắt buộc và MoMo Sandbox nếu tích hợp thực sự khả dụng; nếu chưa có merchant/IPN HTTPS thì báo BLOCKED_EXTERNAL, pre-demo backup.
- Deploy lên môi trường free tier có thể đáp ứng HTTPS, MariaDB persistence, IPN hoặc báo giới hạn; AI fallback nếu provider unavailable. Không đưa secrets lên Git.
- Hoàn thiện báo cáo SDLC, slide 5–7, video 1–3 phút, kịch bản Q&A.
**Done:** package và demo trọn luồng, có bản test report và deployment note, ready 03/12.

## Quy định thực hiện

Áp dụng [AGENTS.md](../../AGENTS.md) và chỉ làm trong phạm vi prompt này. Báo các trang hoặc API bị tác động, cách chạy thử và kết quả kiểm chứng.
