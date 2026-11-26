# REVIEW-01 · Kiểm duyệt đánh giá

Ngày 26/11/2026. Baseline `main` tại `38fb1ca300ce9903db033da13ee4105588ea394a` cùng working diff G7–G10. Staging trống. Điều kiện G2, G5, G6 và G9 đã được tích hợp. Báo cáo này dùng JAR baseline của ORDER-01 và fixture riêng cho REVIEW-01.

## Luồng kiểm duyệt

- Giữ POST/Redirect/GET. POST lưu mã đánh giá vừa xử lý trong flash của phiên. GET dùng cùng bộ lọc trạng thái, số sao, sản phẩm và pager từ G6. Khi kết quả lùi trang, GET chuyển về URL của trang hợp lệ và chuyển tiếp đúng flash. Khi MongoDB lỗi, giữ bộ lọc và trang yêu cầu để Admin có thể tải lại.
- Dòng còn trong trang nhận lại focus tại nút hành động. Nếu dòng rời trang sau khi lọc, lùi trang hoặc MongoDB không phản hồi, focus chuyển đến tiêu đề kết quả. Focus gốc của HTML hoạt động với JavaScript tắt. Vùng flash dùng chung được dành chỗ, nút và badge có kích thước ổn định. Số kết quả đến từ trang MongoDB và trường hợp kho không phản hồi có thông báo riêng.
- Duyệt vẫn so revision ở MongoDB. Xung đột trả về bản hiện tại theo bộ lọc, Admin xem lại trước khi duyệt. Ẩn giữ điều kiện hiện hành. Nếu kết quả ghi chưa xác định, thông báo yêu cầu tải lại để kiểm tra. Cả thao tác ghi và khôi phục khi mất phản hồi dùng luồng gốc của trình duyệt.
- Luồng này dùng tài khoản `ROLE_ADMIN` và CSRF hiện hành. Quyền viết review vẫn theo ID người mua trên MariaDB. Nội dung, revision, điều kiện duyệt và trạng thái chỉ đọc/ghi từ MongoDB.

## Bằng chứng

- [Trace baseline](../qa/REVIEW-01/browser-baseline.json) đo tại 390 px trên Chrome. Khi duyệt trong Tất cả, dòng đầu đổi từ y=605 thành y=706, nút đổi từ 173 px thành 149 px, focus trở về `BODY`. Trong Chờ duyệt, số kết quả về đúng 20 nhưng URL còn `page=1`. Dòng biến mất khi ẩn dưới lọc Đang hiện là thay đổi hợp lệ của tập kết quả.
- Sau triển khai trên cùng fixture, dòng đầu giữ y=732 và vị trí y=279 trong viewport qua cả ba thời điểm trước gửi, đang xử lý và sau GET. Nút giữ 184 px, badge giữ 116 px và focus về lại nút. Ẩn dòng duy nhất dưới lọc Đang hiện trả đúng 0 và focus về tiêu đề. Với JavaScript tắt, duyệt dòng cuối đưa URL `page=1` về `page=0`, hiển thị 20 đánh giá, giữ flash và focus tiêu đề. [Trace sau triển khai](../qa/REVIEW-01/browser-results.json) · [Desktop](../ui/screenshots/REVIEW-01/desktop-reviews.webp) · [Mobile sau khi clamp](../ui/screenshots/REVIEW-01/mobile-clamped.webp)
- Hai tab Admin kiểm revision cũ bị từ chối và nút hiển thị bản hiện tại. Khi Chrome bỏ phản hồi POST sau ghi, GET đọc lại revision tăng đúng một lần. Back về bộ lọc giữ cùng revision. Khi dừng MongoDB fixture, POST báo cần kiểm tra lại và GET hiển thị tạm thời chưa tải được. [Kho không phản hồi](../ui/screenshots/REVIEW-01/desktop-unavailable.webp)
- [Maven](../qa/REVIEW-01/maven-results.txt) dùng JDK 21. `mvn -o -B -ntp test` có 239/308 ca đạt, 69 bỏ qua theo profile và 0 lỗi. `mvn -o -B -ntp -DskipTests package` thành công.
- [Tích hợp](../qa/REVIEW-01/integration-results.txt) dùng `powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/REVIEW-01/run-fixture.ps1 -Integration`. MariaDB 12.2 và MongoDB 6.0 chỉ lắng nghe localhost với dữ liệu mới. Trong 34 ca, 33 đạt và ca backup tùy chọn của DATA-01 được bỏ qua. Bộ kiểm tra gồm MVC/quyền, sao và nội dung trên MongoDB, kho chưa sẵn sàng hoặc phục hồi, khách sửa nội dung giữa hai lần đọc và callback OAuth fixture. Chạy cùng script với `-Baseline` để đo JAR baseline hoặc không đối số để đo JAR hiện hành.

## File thay đổi

- `AdminSauMua_Controller.java`, `SauMua_Service.java`, `admin/user/reviews.html` và `admin.css` triển khai URL canonical, focus và trạng thái lỗi.
- `AdminDanhGiaPagesTest.java`, `SauMuaDanhGiaServiceTest.java`, `SauMua_MariaDb_Test.java` và `OAuth2Callback_MariaDb_Test.java` kiểm flash, lỗi kho, bản chỉnh sửa và `ROLE_CUSTOMER`. Test thứ ba chạy bằng lớp con `SauMua_MongoDanhGia_Test` với MongoDB thật. Test OAuth dùng cùng ID nội bộ và kiểm `403` từ HTTP.
- `docs/qa/REVIEW-01/`, `docs/ui/screenshots/REVIEW-01/`, sổ tiến độ và roadmap ghi lại bằng chứng G11.

## Giới hạn và bàn giao

- Callback Google/GitHub dùng fixture. Chrome headless mô phỏng viewport 390/1280 px. Thao tác với thiết bị vật lý và tài khoản thật chưa được xác minh.
- Khi cần quay lui, có thể hoàn nguyên phần điều hướng, template và CSS của gói. Quy trình kiểm duyệt hiện hành tiếp tục dùng cùng dữ liệu và revision.
- [DTO-01](implementation-prompts/12-DTO-01.md) là gói tiếp theo. QA-01 cần kiểm URL sau clamp, focus, hai tab, mất phản hồi và phân biệt kho lỗi với bộ lọc rỗng.
