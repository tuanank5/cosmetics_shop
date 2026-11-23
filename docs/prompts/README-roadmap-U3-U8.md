# BeautyShop – Roadmap bổ sung sau P12/U2

> Những file này là **prompt thực hiện trong tương lai**. Chưa có file nào chứng minh giai đoạn U3–U8 đã được triển khai hoặc có test PASS.

## Gốc và mở rộng

- **U1–U4 là phạm vi ban đầu của tài liệu P12 do Claude viết.** U3 và U4 ở đây là chi tiết hóa các giai đoạn đã nêu, giữ nguyên định hướng Quiet Luxury và hợp đồng Thymeleaf/Spring MVC.
- **U5–U8 là phần mở rộng do trợ lý đề xuất**, không phải phần Claude đã yêu cầu trong P12 gốc; được tách thành QA, tối ưu, bảo mật, release/demo để không mở rộng scope UI bất ngờ.
- U2 là `P12-U2-luxury-storefront.md` đã tạo trong lần trước, làm từng lát cắt U2.1–U2.5; U3 không viết lại U2.5 và U4 phải test view dùng chung.

| Stage | File | Mục tiêu | Lát cắt khởi đầu |
|---|---|---|---|
| U3 | `P12-U3-luxury-customer.md` | Tài khoản, địa chỉ, đơn/đánh giá/đổi trả, wishlist, chat, auth UX | U3.1 |
| U4 | `P12-U4-luxury-admin.md` | Điều hướng Admin, danh sách, form SKU/ảnh/voucher, đơn, dashboard | U4.1 |
| U5 | `P12-U5-qa-e2e-visual.md` | QA E2E, browser responsive, axe, visual regression, test DB | U5.1 |
| U6 | `P12-U6-performance-seo-polish.md` | Baseline hiệu năng, media, cache, SEO đúng dữ liệu, polish | U6.1 |
| U7 | `P13-U7-security-hardening.md` | Audit/hardening 20 kiểm soát bảo mật | U7.1 (audit) |
| U8 | `P14-U8-release-demo.md` | Build/release, clone, demo 7–10 phút, deployment plan | U8.1 (read-only) |

## Quy tắc ưu tiên cho deadline 03/12/2026

1. **F01/catalog trống và luồng COD P0 phải kiểm chứng sớm**, không chờ làm xong toàn U4.
2. Hoàn thành hoặc chốt blocker U1, U2, U3, U4; tránh làm cùng lúc nhiều thay đổi chung `header`, `tokens`, `order/detail`.
3. U5 smoke/QA chạy song song theo lát cắt sau mỗi U.x; không đợi đến cuối.
4. U7 security P0 nên được audit sớm; không xem bảo mật là bước trang trí cuối.
5. U6 performance/SEO nâng cao là ưu tiên sau các luồng mua hàng; U8 local demo trước, deployment public chỉ sau gate bảo mật/hạ tầng.
6. Mỗi lần chỉ giao **một lát cắt**, không tự commit, không ghi DB hiện dùng, minh bạch skipped tests.

## Prompt mẫu cho Codex

```text
Đọc AGENTS.md, README.md, docs/prompts/P12-luxury-ui-upgrade.md,
docs/prompts/P12-U2-luxury-storefront.md,
docs/reviews/frontend-audit-2026-11.md,
và docs/prompts/P12-U3-luxury-customer.md.

CHỈ LÀM U3.1.
Bảo toàn URL/form/CSRF/role/ownership; JDK 21; không chỉnh database demo.
Không commit/push. Báo cáo file đã sửa, test PASS/FAIL/SKIPPED,
các hạng mục S/H/B/T/NOT VERIFIED rồi DỪNG.
```

Các file được viết để chép vào `docs/prompts/` trong repo. Sau mỗi lát cắt, đối chiếu lại mã nguồn hiện tại trước khi làm tiếp.
