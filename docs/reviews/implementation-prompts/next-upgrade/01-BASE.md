# 01 · BASE · Revision và CI

**Phase 0 · P0/Vừa**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Xác lập revision có thể tái dựng, phân loại từng gate và chuyển hồ sơ phát hành cho owner.

**Điều kiện tiên quyết.** Có thể kiểm kê ngay khi được giao. BLOCKED_OWNER ở bước build sạch/nghiệm thu cho tới khi owner chọn SHA ứng viên. Trạng thái tại lúc soạn prompt: **Chờ giao BASE**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

13 WP có sổ cũ, QA-01 BLOCKED; HEAD hiện có CI đạt. pom, src, CI JDK21, fixture DB trống/legacy. Kiểm manifest và 9 xóa D41, dựng sạch, lập sổ từng skip. Rủi ro dùng nhầm artifact hoặc thiếu tài nguyên. Chỉ xác định bản nghiệm thu, không sửa nghiệp vụ.

Điểm vào để đối chiếu mã hoặc bằng chứng: [pom.xml](../../../../pom.xml) · [ci.yml](../../../../.github/workflows/ci.yml) · [baseline.json](../../../../docs/qa/NEXT-PLAN/baseline.json) · [QA-01-ket-qua.md](../../../../docs/reviews/QA-01-ket-qua.md)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Kiểm lại HEAD, branch, index, local/remote ref và CI bằng thao tác chỉ đọc. Dùng manifest NEXT-PLAN đối chiếu src/pom, các tệp mới và 9 đường dẫn D41. Tách diff tài liệu đang có khỏi revision nguồn. Ghi SHA ứng viên, chủ duyệt và SHA thực chạy.
2. Trên checkout sạch được duyệt, chọn JDK 21, ghi Maven/JDK, chạy test, package và khởi động JAR với profile dev trên schema thử độc lập. Kiểm Flyway từ trống và đường nâng legacy có checksum phù hợp; không repair V5 theo suy đoán.
3. Đối chiếu XML Surefire theo testcase và cờ fixture. WORDMARK có 231 đạt, 82 skip; 67 MariaDB, 9 Mongo, 3 migration local, 1 auth migration, 1 BSON, 1 P21. Chạy suite thích hợp hoặc giải thích lý do còn skip. Lượt tích hợp 27/27 là bộ riêng. So G13 và CI đúng SHA, chuyển quyết định QA-01/release cho owner.

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | Suite mặc định trên đúng SHA/JDK 21; báo tổng/pass/fail/error/skip. |
| Integration Test | MariaDB, Mongo, BSON, Flyway, G13 |
| Browser Test / E2E | smoke jar đúng SHA qua login, catalog, giỏ, checkout và FAQ. |
| Regression Test | G1–13, CSRF, role, owner, IPN. |

**Gate của BASE.** Owner chốt SHA và ma trận test/package/CI/skip. QA-01 chờ quyết định riêng. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `qa/base-revision` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Manifest → môi trường → biên bản. Giữ bản đang chạy nếu chưa tái lập được.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/BASE-ket-qua.md`, bằng chứng đã che ở `docs/qa/BASE/`, ảnh chọn lọc ở `docs/ui/screenshots/BASE/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
