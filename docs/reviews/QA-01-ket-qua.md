# QA-01 · Nghiệm thu bản tích hợp BeautyShop

Ngày 26/11/2026. `main` tại `38fb1ca300ce9903db033da13ee4105588ea394a` cộng với diff G7–G12 và bản sửa HTML RAG. **Kiểm tra G13 local đã đạt. QA-01 còn BLOCKED ở CI của revision tích hợp và quyết định phát hành của owner.** [Kết quả sau bản sửa](#bản-sửa-html-rag-do-owner-giao-tiếp--26112026)

## Trạng thái Work Package

| Gate | Work Package | Trạng thái |
|---|---|---|
| G1–G6 | SEC-01, DATA-01, ARCH-01, NAV-01, UI-01C, UI-01P | Từng gói DONE tại `38fb1ca` |
| G7–G12 | CART-01, CAT-01, WISH-01, ORDER-01, REVIEW-01, DTO-01 | Từng gói DONE trên fixture, tại `38fb1ca + working diff` |
| G13 | QA-01 | G13 local đạt. BLOCKED ở CI của revision tích hợp và quyết định phát hành. |

Chi tiết và bằng chứng của từng gói có trong [sổ tiến độ](BEAUTYSHOP_IMPLEMENTATION_PROGRESS.md).

## Hồi quy theo gate

| Gate | Bằng chứng chạy lại trên bản tích hợp |
|---|---|
| G1 · Quyền/HTTP | `HttpLoi_MariaDb_Test`, `AdminServiceMethodSecurityTest`, `XacThuc_MariaDb_Test`. Customer đọc tài nguyên khác chủ trả 404 cả trong Chrome fixture. |
| G2 · Kho đánh giá | `MongoTat_MariaDb_Test` chạy khi MongoDB không lắng nghe. `MongoDanhGia_Store_Test` kiểm startup-off rồi phục hồi, dựng unique index trước ghi và chặn trùng. Browser REVIEW-01 phân biệt kho lỗi và kết quả rỗng lúc đọc và ghi. |
| G3 · Ánh xạ và migration | `XacThucMigration_MariaDb_Test` kiểm V5 và V6 lên V7, giữ ID và lịch sử. JPA validate khi khởi động. BSON mới đọc và ghi lại mẫu legacy có/không `_class` trên đích riêng. Chiều đọc bằng lớp cũ có bằng chứng lịch sử ARCH-01. |
| G4 · Điều hướng | NAV-01 Chrome kiểm home-popular → PDP → Back/Forward, tab mới, mục biến mất, giảm chuyển động và bàn phím khi JS tắt. DTO-01 kiểm checkout → thêm/sửa địa chỉ → checkout. Admin chi tiết đơn/đổi trả quay về đúng kỳ. |
| G5 · UI dùng chung | UI-01C Chrome chạy lại mật khẩu, menu, thẻ PDP, xác nhận Admin và mô phỏng zoom 200%. Dashboard hiển thị “Quản lý tài khoản”. Kiểm chi tiết đơn/đổi trả ở cả hai vai trò trên fixture. |
| G6 · Pager | `GiaoDienPagesTest`, `DanhGiaPagesTest`, Mongo fixture trùng timestamp. Chrome kiểm bộ lọc catalog, danh sách đơn và trang cuối kiểm duyệt. |
| G7 · Gộp giỏ | CART-01 Chrome hai tab, thông báo một lần và no-JS. `XacThuc_MariaDb_Test` và callback OAuth fixture kiểm gộp theo một ID và phiên. |
| G8 · Catalog | `DanhMucSanPhamThuongHieu_MariaDb_Test`, `AdminCatalogSecurityTest`, hồi quy SKU/voucher. Kiểm trạng thái, FK, hiển thị và URL xóa cũ. |
| G9 · Yêu thích | WISH-01 Chrome kiểm hai tab, timeout, phiên bị thu hồi, JavaScript tắt và trạng thái đồng bộ. MariaDB kiểm chủ, unique và một sản phẩm xuất hiện nhiều chỗ. |
| G10 · Đơn Admin | `AdminOrderList_MariaDb_Test` và ORDER-01 Chrome kiểm count, trạng thái hiệu lực, bộ lọc, trang và Back đúng dòng. Dashboard giữ nguồn và trạng thái lặp. |
| G11 · Kiểm duyệt | REVIEW-01 Chrome sáu nhóm, `AdminDanhGiaPagesTest`, SQL/Mongo thật. Duyệt dòng cuối, từ chối revision cũ và mất phản hồi POST không ghi lặp. |
| G12 · Projection | `DocView_MariaDb_Test` và `CheckoutIntegrityTest`. `OAuth2Callback_MariaDb_Test` kiểm model email xác nhận, người mua đăng nhập bằng ba phương thức và CSRF/role. |

## Kết quả lúc bàn giao

- Maven 3.9.15, JDK Temurin 21.0.13.1. `mvn -o -B -ntp compile` và `-DskipTests package` thành công. `mvn -o -B -ntp test` có 240 đạt, 70 skip, 0 lỗi trong 310 ca. Các bước này tái hiện phần Maven của `.github/workflows/ci.yml` trên máy local. Workflow từ xa chưa chạy trên working diff. [Compile](../qa/QA-01/ci-compile.txt) · [Test](../qa/QA-01/ci-test.txt) · [Package](../qa/QA-01/ci-package.txt)
- `run-data.ps1` dùng MariaDB 12.2.2 và MongoDB 6.0.8 trên localhost. Tích hợp có 309 đạt, 1 fail, 2 skip trong 312 ca. `ReviewBsonCompatibilityTest` và báo cáo demo P21 là hai ca skip ở lượt này. BSON chạy riêng đạt 1/1. `run-data.ps1 -RestoreOnly` chạy 5/5 ca dựng dữ liệu SQL và 1/1 ca dựng dữ liệu Mongo trước khi backup/restore. [Tích hợp](../qa/QA-01/integration.txt) · [BSON](../qa/QA-01/bson.txt) · [Phục hồi](../qa/QA-01/restore.txt)
- `run-browser.ps1` chạy đạt 6/6 nhóm trên JAR mới, gồm NAV-01 trong nhóm UI-01C. Dashboard đạt 24/24 tổ hợp của viewport 390/768/1366/1920 px, JS bật/tắt và kỳ 7/30/90 ngày. Viewport zoom 200% là mô phỏng 683×475 px với DPR 2. Ca hai tab review chờ tab hoạt động và autofocus hoàn tất rồi mới đo focus. [Nhóm trình duyệt](../qa/QA-01/browser-groups.json) · [Ma trận dashboard](../qa/QA-01/dashboard-matrix.json) · [Dashboard 1920 px](../ui/screenshots/QA-01/UI-01C-dashboard-1920-90d.webp) · [Bảng dashboard khi tắt JS](../ui/screenshots/QA-01/UI-01C-dashboard-390-table-nojs.webp)

## Dữ liệu sau giao dịch đã quan sát qua assertions

- P10 chạy lại trên fixture. Tranh SKU tồn 1 hoặc lượt voucher cuối chỉ một bên thành công. Khi checkout bị từ chối, transaction giữ giỏ bên thua và rollback thay đổi SKU khác/voucher. Hủy COD hợp lệ hoàn tồn một lần. Chi tiết đơn giữ giá và địa chỉ snapshot sau khi chỉnh dữ liệu hiện hành.
- MoMo mock giữ tồn và lượt khi đơn chờ. IPN hợp lệ chỉ chốt kho/lượt một lần. Retry sau lỗi giữ hoặc đổi mã lượt theo kết quả. Ca hết hạn giữ chỗ khi cổng chưa có kết quả, giải phóng sau xác minh, và xác nhận thành công muộn đi qua nhánh hoàn tiền. Test job mới gọi trực tiếp thân job với mock để kiểm con trỏ lô và xử lý tiếp khi một đơn lỗi. Nhịp scheduler 60 giây chưa được đo.
- P20C. Seed có một đơn paid `125.000 ₫`, một đơn chờ và một đơn hủy cách 12 ngày. Tổng đơn theo bảng/KPI là 2 ở kỳ 7 ngày, 3 ở kỳ 30/90 ngày. Doanh thu/AOV cùng `125.000 ₫` và một đơn đã giao. Hai SKU sắp hết, một khách mới, một đơn chờ ở cả ba kỳ. Tỉ lệ hủy là `0,0%` ở 7 ngày và `33,3%` ở 30/90 ngày. Dataset Chart.js khớp từng series dữ liệu bảng khi JS bật.
- P23. Review đọc/ghi MongoDB qua revision và khóa duy nhất. So count/checksum SQL khi phục hồi gồm 12 đơn, 5 lượt thanh toán, 1 dòng review lịch sử, 9 SKU và 4 voucher. BSON phục hồi khớp byte, metadata có unique index. UUID, SHA-256 ảnh fixture và tham chiếu ảnh của SKU cùng khớp trên bản sao.

## Đối chiếu lịch sử và sai khác

- Phiếu 73 ca vẫn nguyên vẹn. Phân loại lịch sử có 44 đạt, 2 đạt kèm ghi chú, 14 chưa chạy, 1 trống, 12 ghi nhận khác. [Đối chiếu từng mã](../qa/QA-01/legacy-matrix.md)
- P10 có các ca COD/MoMo mock và upload. P20C ghi 254 tổng, 10 skip ở lượt MariaDB. P23 ghi 251 tổng, 1 skip ở lượt có MongoDB và 10 skip khi MongoDB tắt. Các mốc dùng tập ca khác nhau. [P10](../testing/02-p10-results.md) · [P20C](../qa/P20C/mvn-test-mariadb.txt) · [P23](../qa/P23/mvn-test-mariadb-mongo.txt)
- Lượt tích hợp đầu gặp kỳ vọng chữ cũ `Còn 3` trong `TuVanRag_MariaDb_Test`. Truy vấn và model trả giá `178.000 ₫` cùng lượng khả dụng 3. HTML tại HEAD đã dùng `Tồn khả dụng: <strong>3</strong>`. Sửa kỳ vọng kiểm thử, giữ nguyên service/template. [Kết quả lượt đầu](../qa/QA-01/integration-shared-db.txt)
- Sau khi qua kiểm tra tồn, cùng test dừng ở ca yêu cầu HTML không chứa email đã nhập trong câu hỏi bị từ chối. Phản hồi của fixture vẫn chứa giá trị trong textarea `cauHoi`. Template và controller này giống HEAD. Từ mã nguồn, nhánh từ chối ở service trả lời trước bước gọi provider. Phép thử chưa đi đến assertion kiểm bộ đếm provider, và chưa có bằng chứng gửi giá trị ra ngoài phản hồi hiện tại. Giữ nguyên assertion bảo vệ dữ liệu. [Tích hợp](../qa/QA-01/integration.txt)

## Điểm chờ xác minh trước release

- OAuth/OTP/SMTP trong fixture theo mock hoặc kiểm tra phiên. Chưa chạy với tài khoản owner, OAuth/SMTP thật, MoMo Sandbox có merchant/HTTPS, Ollama thật, host công khai hay thiết bị vật lý. SHOP-12 vẫn ngoài phạm vi. Không có lần chạy tay mới được ghi vào phiếu owner.
- G13 còn mở vì ca kiểm HTML RAG mock vẫn đỏ. Gói sửa riêng cần xử lý việc điền lại dữ liệu nhạy cảm từ request bị từ chối trong `TuVan_Controller` và template, rồi chạy lại phép thử và gate. QA-01 dừng ở báo cáo và bằng chứng theo phạm vi đã giao.
- Khi thấy sai quyền, tiền, kho hoặc unique index trong môi trường phát hành, owner giữ bản hiện hành và truy về gate tương ứng. Backup và rollback của dữ liệu sản xuất cần runbook và bản sao được duyệt riêng.

Các file mới của checkpoint gồm `MomoReconciliationJobTest.java`, tài liệu/fixture/bằng chứng trong `docs/qa/QA-01/`, ảnh trong `docs/ui/screenshots/QA-01/` và báo cáo này. Các file sửa gồm `TuVanRag_MariaDb_Test.java`, sổ tiến độ và roadmap. QA-01 là gói cuối trong manifest. Owner quyết định phát hành.

## Checkpoint tiếp tục QA-01 · 26/11/2026

### RAG và dữ liệu cá nhân

`TuVanRag_MariaDb_Test#ragCoNguonGiaTonMoiVaFallbackKhiModelSaiHoacLoi` thất bại ở assertion kiểm dữ liệu đầu vào bị từ chối vắng mặt trong HTML. `POST /chat` bind câu hỏi vào `form`, gọi `TuVan_Service.hoi` và trả view `TuVanDa` với cùng form. Service kiểm mẫu email/số điện thoại hoặc nội dung ngoài phạm vi trước FAQ, embedding và chat. Thymeleaf `th:field="*{cauHoi}"` vẫn render lại nội dung request vào textarea. Escape của Thymeleaf giữ an toàn cú pháp HTML nhưng địa chỉ email vẫn có trong response.

BR19 giới hạn RAG truy cập dữ liệu cá nhân, còn gate bảo vệ của dự án yêu cầu phản hồi bị từ chối không chứa email đó. Kết quả HTML vi phạm gate này ngay cả khi trang trả về cùng trình duyệt. Bằng chứng hiện tại xác nhận phản chiếu trong HTTP response của fixture. Test độc lập mới dùng câu hỏi FAQ hợp lệ làm đối chứng và kiểm số request tới cả `/api/embed` và `/api/chat` khi chỉ thêm email tổng hợp vào cùng câu hỏi. Assertion HTML gốc giữ nguyên. [BR19](../requirements/02-business-rules.md) · [Luồng RAG](../deployment/05-chatbot-rag.md)

Đề nghị owner duyệt gói sửa nhỏ cho `POST /chat`. Service trả trạng thái từ chối có kiểu rõ ràng và controller làm sạch form trước render ở nhánh đó. Ca bị validation hoặc giới hạn tần suất cần kiểm riêng vì BindingResult cũng có thể giữ giá trị đã bị từ chối. Giữ validation, CSRF, rate limit và đường FAQ/RAG hợp lệ. Prompt QA-01 giới hạn thay đổi ở test và bằng chứng. [Phạm vi QA-01](implementation-prompts/13-QA-01.md)

### BSON và phục hồi

Lượt lỗi trong `target/qa-01-1e15606d46374fc998dbec12842e54f7/bson.raw.log` kết nối MongoDB fixture cổng 28008. Assertion dòng 29 kỳ vọng `arch_01_copy` nhưng nhận `beautyshop_test_mongo`, nên test dừng trước khi đọc tài liệu. Đây là lỗi chọn database trong harness. Lượt `-RestoreOnly` sau đó xác nhận BSON 1/1 và kiểm checksum/count SQL, checksum BSON, unique index và ảnh fixture trên bản sao. Integration ở lượt bàn giao vẫn 309 đạt, 1 fail, 2 skip. [BSON](../qa/QA-01/bson.txt) · [Phục hồi](../qa/QA-01/restore.txt)

Harness hiện truyền URI BSON riêng qua tham số JVM và giữ `MONGODB_TEST_URI` khớp với schema import. Tùy chọn `-EvidenceTag` ghi kết quả chạy lại vào thư mục riêng. Lần chạy đầu của checkpoint gặp lỗi Maven testCompile khi truy cập JAR Jackson trong cache ngoài workspace, trước khi test được thực thi. [Tóm tắt lượt đầu](../qa/QA-01/continuation-data/integration.txt)

### Trình duyệt

Ma trận bàn giao có sáu nhóm đạt. NAV-01 chạy bên trong UI-01C. CART-01, WISH-01, ORDER-01 và DTO-01 có JSON theo nhóm, dashboard có 24 tổ hợp. CAT-01 có bằng chứng ở G8, ngoài sáu nhóm chạy lại G13. Trình chạy bản bàn giao chuyển hướng ảnh và đa số JSON, nhưng REVIEW-01 ghép tên đường dẫn lúc chạy nên vẫn ghi JSON vào thư mục gói trước. Lượt tiếp tục chuyển hướng JSON REVIEW-01 vào QA-01 trước khi khởi chạy và giữ ảnh QA đã có. [Nhóm bàn giao](../qa/QA-01/browser-groups.json) · [Dashboard](../qa/QA-01/dashboard-matrix.json) · [CAT-01](CAT-01-ket-qua.md)

Lượt REVIEW-01 đầu dừng trước ca focus với hai bề rộng nút 184 và 87 px khi đo bằng cách giả lập độ trễ mạng. Lượt tiếp theo giữ POST bằng Chrome DevTools và bị timeout ở `Runtime.evaluate`. Lượt cuối chụp ngay trong sự kiện submit sau handler của ứng dụng, trước chuyển trang. Chrome 158 đạt 6/6 kiểm tra REVIEW-01. Nút đo 184 px trước, lúc và sau submit. Khi quay về tab cũ, `document.hasFocus()` đạt, POST nhận cảnh báo revision cũ, dòng vẫn “Đã ẩn” và nút của dòng nhận focus. Các assertion giữ nguyên. Năm nhóm trình duyệt còn lại và dashboard dùng bằng chứng bàn giao. [Lượt đầu](../qa/QA-01/continuation-browser/browser-groups.json) · [Lượt timeout](../qa/QA-01/continuation-browser-retry/browser-groups.json) · [Trace lượt đạt](../qa/QA-01/continuation-browser-final/REVIEW-01-browser-results.json)

### Kết quả G13 trước bản sửa HTML RAG

| Kiểm tra | Kết quả |
|---|---|
| JDK 21, Maven 3.9.15 offline | `compile` và `-DskipTests package` thành công. `test` có 240 đạt, 0 fail, 0 error, 71 skip trong 311 ca. SHA-256 JAR đóng gói lại khớp JAR dùng cho Chrome. [Compile](../qa/QA-01/continuation-data-retry/ci-compile.txt) · [Test](../qa/QA-01/continuation-data-retry/ci-test.txt) · [Package](../qa/QA-01/continuation-data-retry/ci-package.txt) · [SHA-256](../qa/QA-01/continuation-data-retry/jar-sha256.txt) |
| Tích hợp trên MariaDB/MongoDB riêng | 310 đạt, 1 fail, 0 error, 2 skip trong 313 ca. Lỗi duy nhất là HTML RAG tại dòng 100. Hai ca skip gồm BSON có lượt riêng và báo cáo demo P21 theo cờ chạy. Ca mới xác nhận 0 request embedding và 0 request chat khi gửi câu hỏi có email tổng hợp. Các assertion gốc nằm sau lỗi HTML vẫn chưa chạy ở lượt này. [Tích hợp](../qa/QA-01/continuation-data-retry/integration.txt) · [Đối chiếu](../qa/QA-01/continuation-data-retry/verification.txt) |
| Migration, BSON và phục hồi | Flyway áp dụng V1–V7 trên schema trống, V5→V7 và V6→V7 trên schema riêng. BSON roundtrip 1/1 và fixture backup 1/1. Restore cùng lượt khớp count/checksum của 5 bảng SQL, BSON có dữ liệu, unique index và ảnh fixture có tham chiếu SKU. Harness trả exit 1 do tích hợp RAG sau khi ghi bằng chứng phục hồi. [BSON](../qa/QA-01/continuation-data-retry/bson.txt) · [Backup](../qa/QA-01/continuation-data-retry/backup-fixture.txt) · [Restore](../qa/QA-01/continuation-data-retry/restore.txt) |
| Chrome headless | Sáu nhóm G13 đạt trên cùng JAR, gồm lượt mới chỉ chạy REVIEW-01. Dashboard 24/24 từ bàn giao. [Bàn giao](../qa/QA-01/browser-groups.json) · [REVIEW-01](../qa/QA-01/continuation-browser-final/browser-groups.json) |
| AUTH-02 | Ba suite được chỉ định đạt 12/12, gồm callback OAuth fixture 5/5, xác thực đa phương thức 5/5 và migration 2/2. Google/GitHub callback thật, Redis thật, SMTP thật và thao tác owner chưa xác minh. MoMo và Ollama có bằng chứng mock trong phạm vi QA; merchant MoMo Sandbox, Ollama thật, host công khai và thiết bị vật lý chưa có bằng chứng mới. [AUTH-02](AUTH-02-nghiem-thu-xac-thuc.md) · [Đối chiếu](../qa/QA-01/continuation-data-retry/verification.txt) |
| GitHub Actions | Maven CI từ xa thành công cho commit `38fb1ca`. Working diff chưa có revision trên GitHub. Workflow chạy theo push/PR. [Run Maven CI](https://github.com/tuanank5/cosmetics_shop/actions/runs/45777423089) · [Workflow](../../.github/workflows/ci.yml) |

File thay đổi trong lượt tiếp tục gồm `TuVanRag_MariaDb_Test.java`, `run-data.ps1`, `run-browser.ps1`, bằng chứng mới ở `docs/qa/QA-01/continuation-*/`, báo cáo này, sổ tiến độ và roadmap. Không thay đổi mã sản phẩm, Entity, migration hoặc cơ sở dữ liệu demo. Ảnh QA hiện có và bằng chứng các gói trước được giữ trong lượt tiếp tục.

Để đóng G13, owner cần duyệt gói sửa phần HTML RAG, chạy lại assertion bảo vệ và regression trên revision sửa, rồi đối chiếu CI GitHub của revision tích hợp được phép đưa lên. Owner quyết định phát hành. `git diff --check` đạt, index không có file staged và lượt tiếp tục không tạo commit, push hay merge.

## Bản sửa HTML RAG do owner giao tiếp · 26/11/2026

`TuVan_Service.TraLoi` báo trạng thái từ chối riêng. `TuVan_Controller` chỉ đưa câu hỏi vào model hiển thị sau khi validation, rate limit và bộ lọc nội dung đều chấp nhận. Textarea vẫn gửi trường `cauHoi`. Nó lấy giá trị hiển thị riêng với dữ liệu bind để tránh cả giá trị bị từ chối trong `BindingResult`. Nhánh lỗi giữ thông báo, nhãn truy cập và mã HTTP hiện có. Luồng hợp lệ vẫn hiện lại câu hỏi. [D35](../DECISIONS.md)

Kiểm thử bổ sung kiểm email và chuỗi số điện thoại tổng hợp trong phản hồi và bộ đếm mock. MockMvc kiểm câu hỏi quá dài, HTTP 429, `Retry-After`, giá trị hợp lệ và CSRF. Phần đóng gói được đối chiếu với JAR Chrome trước đó, chỉ cụm lớp `TuVan_Controller`, `TuVan_Service` và `TuVanDa.html` thay đổi. [Test tập trung](../qa/QA-01/rag-fix-maven/focused.txt) · [Đối chiếu JAR](../qa/QA-01/rag-fix-maven/jar-diff.txt)

| Kiểm tra sau bản sửa | Kết quả |
|---|---|
| Maven offline và JDK 21 | Test tập trung 4/4. Compile và package đạt. Test mặc định có 241 đạt, 71 skip, 0 fail/error trong 312 ca. [Compile](../qa/QA-01/rag-fix-maven/compile.txt) · [Test](../qa/QA-01/rag-fix-maven/test.txt) · [Package](../qa/QA-01/rag-fix-maven/package.txt) · [SHA-256 JAR](../qa/QA-01/rag-fix-maven/jar-sha256.txt) |
| Tích hợp MariaDB/MongoDB fixture | `run-data.ps1 -EvidenceTag rag-fix-data` trả exit 0. Trong 314 ca có 312 đạt, 2 skip, 0 fail/error. BSON skip trong suite chính rồi đạt riêng; báo cáo demo P21 vẫn là opt-in. RAG 3/3, FAQ 2/2, AUTH-02 12/12. Assertion HTML gốc và các assertion tiếp theo về bộ đếm chat, fallback và catalog đều đạt. Hai mẫu dữ liệu bị từ chối tạo 0 request embedding và 0 request chat ở phép thử độc lập có đối chứng. [Tích hợp](../qa/QA-01/rag-fix-data/integration.txt) · [RAG, FAQ và AUTH-02](../qa/QA-01/rag-fix-data/verification.txt) |
| Dữ liệu và phục hồi | Flyway V1–V7 trên schema mới và V5/V6 lên V7. BSON 1/1, dựng fixture backup 1/1, restore khớp count/checksum của 5 bảng SQL, BSON, unique index và ảnh fixture. [Migration](../qa/QA-01/rag-fix-data/verification.txt) · [BSON](../qa/QA-01/rag-fix-data/bson.txt) · [Backup](../qa/QA-01/rag-fix-data/backup-fixture.txt) · [Restore](../qa/QA-01/rag-fix-data/restore.txt) |
| Chrome headless trên JAR mới | `run-browser.ps1 -Groups RAG-HTML -EvidenceTag rag-fix-browser` trả exit 0. `/chat` đạt 8/8 tổ hợp ở 390/768/1366/1920 px với JS bật/tắt. Mỗi tổ hợp dùng form thực và kiểm FAQ hợp lệ cùng câu hỏi có dữ liệu bị từ chối. Fixture Chrome dùng chế độ FAQ; HTTP mock Ollama được kiểm trong integration. Bằng chứng của sáu nhóm trình duyệt G13 trước đó được giữ vì các lớp và tài nguyên của những nhóm đó khớp với JAR trước. [Trace Chrome](../qa/QA-01/rag-fix-browser/RAG-HTML-browser-results.json) |
| SHA từ xa và xác thực thật | Maven CI từ xa chỉ có bằng chứng thành công cho `38fb1ca`. Bản sửa hiện ở working diff, index trống. Google/GitHub OAuth, Redis, SMTP thật và kiểm tay của owner còn chờ môi trường/cấu hình tương ứng. MoMo Sandbox và Ollama thật vẫn có trạng thái riêng. [CI của HEAD](https://github.com/tuanank5/cosmetics_shop/actions/runs/45777423089) · [AUTH-02](AUTH-02-nghiem-thu-xac-thuc.md) |

File thay đổi của bản sửa gồm `TuVan_Controller.java`, `TuVan_Service.java`, `TuVanDa.html`, `TuVan_ControllerTest.java`, `TuVanRag_MariaDb_Test.java`, `run-browser.ps1`, `rag-html-checks.mjs`, quyết định D35, ba tài liệu trạng thái và bằng chứng tại `docs/qa/QA-01/rag-fix-*/`. `git diff --check` đạt. Lượt kiểm thử dùng database riêng. Điều kiện còn thiếu là kết quả GitHub Actions trên revision tích hợp được phép đưa lên và quyết định phát hành của owner.
