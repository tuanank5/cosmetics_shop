# 16 · PERF · Đo ảnh, SQL và cache có điều kiện

**Phase 7 · P2/Vừa**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Đo đúng điểm tốn chi phí và chỉ thay ảnh, truy vấn, index hoặc cache khi số trước/sau chứng minh ích lợi.

**Điều kiện tiên quyết.** Sau BASE đo trên fixture đại diện. Sửa chỉ khi xác lập được chi phí và lợi ích; module mới cần có dữ liệu của chính module đó. Trạng thái tại lúc soạn prompt: **Chờ BASE**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

Có batch, DTO, eager/lazy và thumbnails; thiếu benchmark lớn. AnhBienTheSanPham_Controller, thẻ/Home/PDP, repo catalog, giỏ, Admin/attempt/audit/chat, Redis. Chỉ index/cache/header ở điểm có số đo. Rủi ro stale hoặc làm chậm ghi. Giữ pipeline ảnh, không Cloudinary/CDN/dọn ảnh/tìm gần đúng.

Điểm vào để đối chiếu mã hoặc bằng chứng: [AdminDanhMucSanPhamThuongHieu_Controller.java](../../../../src/main/java/com/beautyshop/controller/admin/AdminDanhMucSanPhamThuongHieu_Controller.java) · [AnhBienTheSanPham_Controller.java](../../../../src/main/java/com/beautyshop/controller/AnhBienTheSanPham_Controller.java) · [product-card.html](../../../../src/main/resources/templates/fragments/product-card.html) · [DTO-01-ket-qua.md](../../../../docs/reviews/DTO-01-ket-qua.md)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Chốt fixture, SHA, JDK, phần cứng, độ ấm và kích thước. Đo số SQL, EXPLAIN data/count, rows/bytes, p50/p95 tại danh mục/hãng tải toàn bộ và demDonDangXuLy, nhiều SKU, giỏ 1/10/100 dòng, Admin, attempt/audit/hội thoại khi có. Với Mongo history dùng explain và index thực của Mongo. Ghi số trước khi sửa.
2. Đo ảnh Home, card ngoài viewport và PDP trên desktop/mobile: bytes, LCP, CLS, priority, kích thước, cache/revalidation và URL mới khi thay UUID. Giữ hero/PDP ưu tiên đúng, thumbnail/WebP/local, chỉ sửa header/lazy/tỉ lệ nếu có bằng chứng.
3. Nếu số đo đủ, thử projection/batch/index đúng predicate trước; chỉ xét cache dữ liệu công khai có key, TTL hữu hạn, evict sau commit, cache miss/Redis lỗi đọc DB. Đo lại cùng fixture cả đường ghi và cache lạnh. Giá/tồn/quyền/thanh toán luôn kiểm server. Hết WP ở số đo nếu lợi ích không rõ.

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | Key/TTL và ưu tiên ảnh nếu đổi |
| Integration Test | EXPLAIN, SQL count/bytes/p95, cold/warm, Redis lỗi/rollback |
| Browser Test / E2E | waterfall, LCP/CLS/304/ảnh mới |
| Regression Test | giá, tồn, quyền, thứ tự/count và fallback. |

**Gate của PERF.** Bảng trước/sau cùng fixture. Kết quả đo có thể đủ để kết thúc mà không sửa code. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `perf/measured-hotspots` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Baseline → từng thay đổi có số đo → đo lại. Bypass cache, trả header cũ, đánh giá bỏ index qua migration mới.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/PERF-ket-qua.md`, bằng chứng đã che ở `docs/qa/PERF/`, ảnh chọn lọc ở `docs/ui/screenshots/PERF/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
