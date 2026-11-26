# SEC-01 — HTTP/error, nghiệm thu G1

Ngày 26/11/2026. **DONE trong phạm vi fixture SEC-01**.
Checkpoint: main, HEAD 6368b6723d425c23d8901a5ebc69bbaea3ac09b4 + working diff; chưa commit/push.
Working tree/index ban đầu sạch; AUTH-02 đã nằm trong HEAD. Lượt tiếp tục bảo toàn toàn bộ diff SEC-01 trước đó.
Chỉ triển khai SEC-01; 12 Work Package còn lại chưa chạy.

## Thay đổi và lý do

- Thêm src/main/resources/templates/fragments/error.html: shell chung kiểu 404, tiêu đề/thông báo cố định theo nguyên nhân, mã lỗi, main có tabindex và hành động HTML thuần.
- Chuyển error.html và error/{400,403,404,409,413,500}.html sang fragment. Thêm error/429.html với thông báo chờ rồi thử lại.
- Admin có liên kết /admin/dashboard qua sec:authorize; Guest không có liên kết này.
- GiaoDienPagesTest: mở rộng status 429 và test liên kết Admin/Guest.
- Thêm HttpLoi_MariaDb_Test: HTTP client thật tới Tomcat random port, login thực bằng BCrypt/session và fixture SQL riêng; filter ghi REQUEST/ERROR dispatch chỉ nằm trong test.
- Thêm MomoHttpContractTest: malformed/duplicate JSON, kích thước body, conflict và throttle vẫn giữ body IPN rỗng; Retry-After đúng.
- docs/qa/SEC-01/run-mariadb.ps1: database trống riêng cổng 33374, datadir UUID trong target, upload riêng; tự dừng đúng process DB sau test. -Ui giữ server tối đa 15 phút để kiểm trình duyệt; tạo ui-finished.signal trong cùng thư mục QA để kết thúc sớm.
- Cập nhật sổ triển khai và roadmap. File test mới là phần kiểm thử HTTP trực tiếp thuộc SEC-01; không mở rộng nghiệp vụ.

Không thay Security_Config, principal, ownership predicate, AnhTaiLen_Advice, handler IPN, service nghiệp vụ, schema, migration hoặc dependency. Không có contract backend mới để downstream phải đổi. Trace header/filter và route /products/sec01-* chỉ tồn tại trong test, không vào JAR production.

## Lệnh và kết quả

Maven mặc định trên máy chọn JDK 25; mọi lệnh bên dưới đặt JAVA_HOME sang Temurin 21.0.13.1 trước khi chạy. Maven 3.9.15, profile dev.

| Lượt | Lệnh | Kết quả |
|---|---|---|
| Baseline | mvn -o compile | Exit 0 |
| Baseline | mvn -o test | 284 tổng, 226 đạt, 58 skip, 0 failure/error |
| Baseline | mvn -o -DskipTests package | Exit 0 |
| Baseline COD | script database riêng | 4/4 đạt |
| Sau sửa cuối | mvn -o test | **288 tổng, 229 đạt, 59 skip, 0 failure/error** |
| Sau sửa cuối | mvn -o -DskipTests package | **Exit 0** |
| MariaDB cuối | powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/SEC-01/run-mariadb.ps1 | **21/21 đạt, 0 skip/failure/error** |
| Diff | git diff --check; kiểm diff tracked/untracked | Không có lỗi whitespace; chỉ file SEC-01 và tiến độ/roadmap |

21 ca tích hợp gồm COD 4, HTTP thật 1 (nhiều assertion ma trận), MoMo 3, xác thực đa phương thức 5, xác thực 8. Không cộng các nhóm thành số ca duy nhất nếu có trùng suite.
59 skip ở lượt mặc định là các suite có điều kiện môi trường. Xem skipped-suites.json để biết tên và nguyên nhân. Không tính skip là đạt.

## Ma trận G1 và bằng chứng

| Điều kiện | Quan sát đã kiểm chứng |
|---|---|
| Hai Customer cùng build, GET /orders/{id} | Chủ đơn 200; Customer khác 404; ID vắng 404. Nội dung main của hai lỗi bằng nhau. REQUEST 404 → ERROR /error 404, không redirect che lỗi. |
| Cùng route, sai role/CSRF | Admin đọc route Customer 403; POST cancel thiếu CSRF 403 cho cả ID thật/vắng; POST đủ CSRF khác chủ/vắng cùng 404; Guest 302 về login. |
| Địa chỉ | GET edit và POST edit/delete khác chủ/vắng đều 404 khi Customer có CSRF hợp lệ. |
| Review item | GET/POST khác chủ/vắng 404; không chạm store nội dung khi ownership thất bại. |
| Đổi trả | GET form theo đơn, POST tạo và GET chi tiết return khác chủ/vắng 404. |
| Thanh toán mock/retry | GET/POST mock và POST retry khác chủ/vắng 404. MoMo mock suite giữ kiểm chữ ký, số tiền, replay/idempotency và CSRF mock. |
| HTML/JSON | HTML không lộ SQL/class/exception; JSON lỗi vẫn 404 và JSON, không bị đổi thành HTML. |
| 400/403/404/409/413/429/500/503 | Resolver/render giữ nguyên status; 429 có nội dung riêng. |
| Ngoài MVC | Filter fixture ném exception trước MVC; client nhận 500 và trang an toàn, không lộ chuỗi exception. |
| Upload | Multipart thực >2 MB với Admin + CSRF hợp lệ trả 413, lỗi ảnh tại form SKU. AnhTaiLenAdviceTest giữ ca không được đọc dữ liệu Admin khi không có role. |
| Phiên | Phiên tăng version bị đưa về /login?inactive; Guest không session về login. XacThuc suite giữ logout/khóa/mở không phục hồi phiên cũ. |
| Reset/auth baseline | Cùng phản hồi xin reset cho email có/không tồn tại; CSRF vẫn bắt buộc. Suite AUTH-02 giữ NULL password/OAuth-only/link/reset/version. |
| IPN | Unit contract: malformed/duplicate JSON 400, quá cỡ 413, conflict 409, throttle 429 + Retry-After: 1, body rỗng. HMAC/401 và IPN thành công 204/replay kiểm riêng trong MariaDB MoMo mock. |

Trace đã lọc tại docs/qa/SEC-01/http-dispatch-trace.txt, gồm mã UUID đối chiếu, phase, method, route ẩn numeric ID, status; không chứa cookie, credential, token, email hay body.
Ví dụ 404 REQUEST và ERROR dùng cùng correlation ID. Với exception trước MVC, dòng REQUEST ghi status mặc định 200 tại lúc filter unwind, sau đó ERROR ghi 500; HTTP client xác nhận cuối cùng 500. Không diễn giải dòng REQUEST đó thành phản hồi thành công.

## Giao diện

Chrome native UI trên fixture Tomcat:
- Bàn phím: Tab tới “Đến nội dung chính”, Enter tới #main, Tab tới “Xem sản phẩm”; focus viền rõ.
- JavaScript tắt trong DevTools, reload trang 404; nội dung và nút còn đầy đủ, bấm “Xem sản phẩm” tới /products.
- 390px: tiêu đề/thông báo xuống dòng, hai nút xếp dọc, không bị cắt.
- 768px và 937px: trang 429 đọc được, hành động hiển thị; thông báo nguyên nhân rõ.
- Lần nhập 1366 bị DevTools giới hạn còn 937; chỉ ghi nhận 937, không tuyên bố đã kiểm 1366. Chưa kiểm riêng browser zoom.
- Chrome có thao tác đồng thời từ người dùng nên dừng kiểm cửa sổ sau các ca trên, không tác động tab đang dùng của owner. JavaScript bị tắt chỉ trong tab fixture khi DevTools còn mở.

Ảnh chọn lọc:
docs/ui/screenshots/SEC-01/404-keyboard.png
docs/ui/screenshots/SEC-01/404-mobile-nojs.png
docs/ui/screenshots/SEC-01/429-tablet-nojs.png
docs/ui/screenshots/SEC-01/429-937-nojs.png

## Giới hạn, mock và bàn giao

- **Chưa tái hiện 403 của phiên owner DH-05**; giữ nguyên phiếu/quan sát của owner. Kết luận 404 ở trên chỉ dựa fixture hợp lệ và trace của build này.
- MariaDB và HTTP/Tomcat là thật, database fixture trống và độc lập. Mongo cố ý không khả dụng ở loopback cổng 1. OTP mock trong COD/auth; Redis/SMTP mock trong suite xác thực đa phương thức. Không gửi thư, không gọi OAuth provider thật, không thanh toán thật.
- Không có thay đổi dữ liệu owner nên không dump/restore dữ liệu vận hành. Các gate phục hồi SQL/Mongo của DATA-01/ARCH-01 giữ nguyên.
- Server/DB fixture đã dừng sau test. Các log Maven .log được Git ignore nhưng vẫn nằm trong repository; summary.json, skipped-suites.json và trace .txt là bằng chứng gọn có thể đưa vào commit.
- G1 đạt trong phạm vi SEC-01; không phải chứng nhận release hoặc OAuth/SMTP/Redis production.
- Prompt đề xuất tiếp theo: docs/reviews/implementation-prompts/02-DATA-01.md, sau khi được owner giao riêng. Không tự chạy.

Bằng chứng: docs/qa/SEC-01/summary.json; skipped-suites.json; http-dispatch-trace.txt; baseline-compile.log; baseline-test.log; baseline-package.log; final-test.log; final-package.log; mvn-mariadb.log.