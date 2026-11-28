# 12-AI-OPS · Kết quả trên fixture ngày 29/11/2026

**IN_PROGRESS · AI-OPS-FINAL sẵn sàng để owner review.** Local QA ghi nhận **278 ca PASS**, **0 FAIL**, **117 ca NOT RUN**. Tiến độ dự án 9/19 DONE. Branch `feat/ai-operations` xuất phát từ `fa99f2963d631203b08e50dde3fb4f5329f92212` sau PR #21. Candidate nằm trong index của checkout riêng. PR, CI và nghiệm thu owner còn chờ.

## Phạm vi và finding đã sửa

| Điểm vào | Hành vi đã kiểm và thay đổi |
|---|---|
| Guest hoặc tài khoản dùng `POST /chat` | Giữ CSRF, tối đa 500 ký tự, giới hạn 6 câu/phút/phiên và 30 câu/phút/tiến trình. Guard kiểm nội dung ngay tại service trước embedding. Bổ sung mẫu email/điện thoại, mã xác thực, token, khai báo thông tin cá nhân, chỉ dẫn ngoài phạm vi và markup sau chuẩn hóa Unicode. Nhánh từ chối, lỗi biểu mẫu và 429 giữ ô soạn trống theo D35. |
| FAQ và RAG | Trước đây lọc URL, vẫn có thể chuyển tiêu đề hoặc đoạn bẩn đến embedding. Kiểm cả tiêu đề, câu trả lời và trường catalog công khai trước khi chia đoạn. Nguồn HTTPS ngoài cần URL đã duyệt chính xác trong `app.ai.trusted-faq-urls`. Danh sách mặc định rỗng. Nguồn đang hoạt động được đọc lại khi chọn trích dẫn. |
| Đường tới provider | Đếm một kết quả khi POST kết thúc. Tại HTTP client của Ollama, đếm riêng embedding truy vấn, embedding lập chỉ mục và chat. Mỗi dòng có độ trễ trung bình, tổng và phân loại kết quả. FAQ trả thành công sau lỗi AI vẫn tính là một phản hồi thành công cho khách. Bận worker và hết thời gian chờ tác vụ có bộ đếm riêng. |
| `/admin/ai` | Hiển thị cấu hình bật/tắt, khả năng dùng provider, số đoạn, thời điểm lập chỉ mục, số liệu và thời điểm thực nhận phản hồi HTTP. Khi mới khởi động hiện “chưa kiểm tra”. Timeout socket trước phản hồi chỉ cập nhật kết quả lần gọi. Việc xem trang đọc trạng thái trong tiến trình. |
| Admin reindex | `POST /admin/ai/reindex` kiểm ROLE_ADMIN và CSRF. Hợp đồng AUDIT ghi actor, correlation và số đoạn trước/sau cho `AI_REINDEX` trên `AI_INDEX/1`. SUCCESS cùng transaction, FAILED sau rollback. Chỉ mục mới được công bố sau commit. |
| Câu trả lời và điều hướng | Giữ FAQ khi AI tắt/lỗi, topK 6, threshold 0,55, tối đa 4 đoạn để chọn, parser chữ số, 2 worker và hạn 12/30 giây. Giá và tồn theo SKU đọc từ SQL. D41 tiếp tục trả 404 cho GET/POST có quyền tới route cũ. |

## Kết quả thực chạy

| Gate | Kết quả và assertion |
|---|---|
| JDK 21 Maven test | 395 ca được phát hiện, 261 PASS, 134 SKIP, 0 lỗi. Các test controller kiểm từ chối do kích thước/tần suất, giữ văn bản FAQ, escape và D35. Unit guard kiểm nguồn catalog, URL cho phép, Unicode và kích thước. Log [test.txt](../qa/AI-OPS/verified/test.txt). |
| MariaDB 12.2.2, Flyway và JPA | Schema riêng rỗng lên V11, JPA validate. 6 AUDIT + 5 FAQ + 6 RAG = 17 PASS. Kiểm thứ tự SUCCESS/FAILED/FAILED và actor sau reindex, rollback chủ động và provider 503. Guest chuyển tới login, Customer bị 403, Admin thiếu CSRF bị 403. Log [integration.txt](../qa/AI-OPS/verified/integration.txt). |
| Provider HTTP mock | Sau đối chứng có embedding và chat, bốn POST bị guard từ chối tạo **0 lượt gọi thêm**. Bốn câu hợp lệ tạo **4 request, 4 embedding truy vấn, 4 chat**. Reindex chỉ tăng embedding index. Query 429 dừng trước chat. HTTP 5xx, 429, 504, socket đọc quá hạn và hai worker bận đều có assertion. Marker nguồn bẩn vắng trong payload, snapshot metrics vắng câu hỏi và các giá trị cá nhân fixture. |
| WebKit qua HTTP và session thật trên loopback | Mobile 390×844 dùng FAQ khi AI tắt rồi đăng nhập Admin kiểm trạng thái chưa kiểm tra. Desktop 1280×800 đăng nhập, bấm reindex và thấy thời điểm phản hồi từ mock. Hai lượt đã nằm trong 17 test fixture. |
| MongoDB 6.0.8 | Fixture riêng khởi động, nhận kết nối và còn chạy khi bộ integration kết thúc. AI-OPS lưu metrics/chỉ mục trong bộ nhớ. Gate này kiểm môi trường ứng dụng, chưa kiểm nghiệp vụ lưu lịch sử chat. [Trích log Mongo](../qa/AI-OPS/verified/mongo.txt). |
| Maven package | `-DskipTests package` thành công trên cùng nguồn. [Log package](../qa/AI-OPS/verified/package.txt). |

Đối chiếu Surefire theo lớp và tên ca cho thấy cả 17 ca fixture đều SKIP ở Maven mặc định. Tổng duy nhất là 261 + 17 = 278 PASS. Danh sách 117 ca còn NOT RUN và kết quả theo gate nằm trong [verification.json](../qa/AI-OPS/verification.json).

Lượt fixture đầu có 16 ca Java đạt nhưng MongoDB bị Windows từ chối bind cổng, nên gate môi trường FAIL. Harness đã bổ sung kiểm tra cổng loopback, chờ Mongo lắng nghe và kiểm tiến trình cuối lượt. Fixture cuối dùng datadir MariaDB/MongoDB mới. [Log lượt đầu](../qa/AI-OPS/initial/integration.txt).

## Dấu vết nguồn và cách tái kiểm

Đối chiếu byte của 364 đầu vào giữa checkout bàn giao, bản chạy Maven test/package và bản chạy integration. SHA-256 tổng hợp `ea82bfa20ddffbfb1cb5f7a95151d4856a8b27117ce3a0f57d7d981cb7ee989e`. Danh sách gồm `pom.xml`, toàn bộ `src/` và hai script fixture. Manifest chứa SHA-256 của từng file thay đổi ngoài chính manifest. [Nguồn đã test](../qa/AI-OPS/source.json) · [Manifest](../qa/AI-OPS/manifest.sha256).

Trên Windows, đặt `JAVA_HOME` về JDK 21, chọn Maven repo có dependency và thư mục playwright-core đã cài WebKit. Từ checkout riêng, dùng lệnh dưới với thư mục bằng chứng mới để giữ bản log đã bàn giao.

```powershell
mvn.cmd test
if ($LASTEXITCODE) { throw 'Maven test failed' }
mvn.cmd -DskipTests package
if ($LASTEXITCODE) { throw 'Maven package failed' }
$evidence = New-Item -ItemType Directory ("target/ai-ops-qa-" + [guid]::NewGuid())
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/AI-OPS/run-fixture.ps1 -Checkout . -MavenRepo $env:MAVEN_REPO -PlaywrightCore $env:BASE_PLAYWRIGHT -EvidenceDirectory $evidence.FullName
if ($LASTEXITCODE) { throw 'Fixture failed' }
```

## Giới hạn nghiệm thu

- Provider chỉ chạy bằng HTTP mock trên loopback. AI-LIVE, provider bên ngoài, tải thực tế, production và manual UAT đều NOT RUN.
- Metrics chỉ gồm bộ đếm hữu hạn, độ trễ và thời điểm theo từng tiến trình. Restart đặt lại thống kê và chỉ mục. Phản hồi HTTP, kể cả HTTP lỗi, chỉ chứng minh đã có phản hồi tại thời điểm ghi nhận. Guard theo mẫu chưa chứng minh phát hiện mọi dữ liệu cá nhân trong văn bản tự do. Nội dung FAQ ngoài vẫn cần người phụ trách duyệt.
- Gói này dùng schema V1–V11 hiện hành. Nghiệp vụ Mongo cho AI-HISTORY và chính sách retention thuộc WP riêng.
