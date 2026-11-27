# 12 · AI-OPS · Bảo vệ và thống kê /chat

**Phase 5 · P1/Vừa**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Bảo vệ các nhánh đang tồn tại trong /chat và đưa trạng thái, số request và số lần gọi provider lên /admin/ai.

**Điều kiện tiên quyết.** Sau BASE có thể kiểm guard và làm số liệu bằng mock. Phần ghi audit reindex chờ hợp đồng AUDIT. Trạng thái tại lúc soạn prompt: **Chờ BASE**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

Có FAQ/RAG, giới hạn và reindex, thiếu thống kê người dùng/provider. TuVan_Controller/Form, TuVan_Service, KhoTriThuc_Service, RagProvider, OllamaRag_Config, AdminTuVan_Controller và admin/chat. Chỉ guard/đo/kiểm. Rủi ro PII vào provider hoặc metric. Giữ /chat và thuật toán, chỉ mock provider.

Điểm vào để đối chiếu mã hoặc bằng chứng: [TuVan_Service.java](../../../../src/main/java/com/beautyshop/service/TuVan_Service.java) · [KhoTriThuc_Service.java](../../../../src/main/java/com/beautyshop/service/KhoTriThuc_Service.java) · [AdminTuVan_Controller.java](../../../../src/main/java/com/beautyshop/controller/admin/AdminTuVan_Controller.java) · [CHAT-ROUTE-01-ket-qua.md](../../../../docs/reviews/CHAT-ROUTE-01-ket-qua.md)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Kiểm biên /chat từ TuVan_Form, TuVan_Controller, TuVan_Service, KhoTriThuc_Service tới RagProvider và OllamaRag_Config. Giữ guard PII và kích thước trước mọi provider/lịch sử, phân biệt dữ liệu câu hỏi với nội dung nguồn được phép index. Kiểm nguồn bẩn trước embedding.
2. Giữ threshold/worker/timeout/FAQ khi AI tắt hoặc lỗi, parser chọn đoạn và kết quả văn bản. Chỉ tách guard/parse khi có caller hoặc test cụ thể. Đếm một kết quả người dùng và tách số embedding query/index/chat; đo timeout, từ chối, 429, lỗi, thành công và độ trễ. Không lưu câu hỏi để thống kê.
3. Trên /admin/ai giữ reindex, hiển thị enable, chỉ mục và lần kết nối gần nhất có timestamp hoặc chưa kiểm. Audit khi reindex theo hợp đồng. Kiểm bằng mock và HTTP mock cho lỗi/thành công/timeout/đếm call; không thăm dò provider định kỳ hay mở AI-LIVE.

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | Guard/parse/timeout và count |
| Integration Test | mock HTTP, lỗi, worker bận, embedding query/index khác chat |
| Browser Test / E2E | FAQ tắt AI, Admin reindex, trạng thái cuối |
| Regression Test | D35/D41, không phản chiếu câu từ chối. |

**Gate của AI-OPS.** Từ chối dùng 0 provider call, counters phân biệt request/call, kết quả văn bản và fallback cũ. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `feat/ai-operations` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Guard/test → counter → Admin. Gỡ thay đổi quan sát, giữ FAQ/RAG hiện hành.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/AI-OPS-ket-qua.md`, bằng chứng đã che ở `docs/qa/AI-OPS/`, ảnh chọn lọc ở `docs/ui/screenshots/AI-OPS/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
