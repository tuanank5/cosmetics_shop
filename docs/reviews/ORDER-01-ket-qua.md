# ORDER-01 · Danh sách đơn Admin

Ngày 26/11/2026. Baseline `main` tại `38fb1ca300ce9903db033da13ee4105588ea394a`, cùng working diff của CART-01, CAT-01 và WISH-01. G1, G4, G6 đã đạt và G9 đã được tích hợp. Staging trống khi bắt đầu.

## Hợp đồng đã triển khai

- `GET /admin/orders` nhận `status` lặp từ dashboard và form, cùng `method`, `paymentStatus`, `q`, `sort`, `page`, `size`. `q` khớp phần đầu hoặc toàn bộ mã đơn hiển thị. Độ dài tối đa 80 ký tự, nhận chữ ASCII, số, `-`, `_`. `_` được khớp như ký tự thường. Các bộ lọc tùy chọn ngoài allowlist dùng giá trị mặc định. Trạng thái đơn không hợp lệ tiếp tục trả 400 theo binding enum.
- Trang bắt đầu từ 0. Mặc định 20 đơn, cho phép 10/20/50. Trang vượt giới hạn lùi về trang cuối. Mới nhất và cũ nhất theo thời điểm tạo với ID làm khóa phụ. Hai chiều tổng tiền dùng ID giảm dần để xử lý các đơn bằng tiền.
- MariaDB dùng cùng predicate đã bind cho count và trang trong transaction đọc `REPEATABLE_READ`. Kết quả có một dòng mỗi đơn. Trạng thái MoMo lấy trong `MOMO` và `MOMO_MOCK`, ưu tiên lượt `PAID` rồi ID lượt giảm dần. COD lấy đúng yêu cầu `COD-{id}` của cổng COD. Danh sách và chi tiết dùng chung biểu thức truy vấn tương ứng. KPI P20 dùng số toàn hệ thống, vùng kết quả lấy count theo lọc.
- Chi tiết và hai POST trạng thái/đối soát giữ bộ lọc qua các trường `list*` đã chuẩn hóa. Đường về danh sách dùng anchor của đơn và trang đã mở. Nếu thao tác làm đơn rời tập lọc, danh sách dùng bộ lọc cũ và trang hợp lệ. Đường về dashboard vẫn dùng kỳ đã chọn.
- Luồng `/orders` của Customer tiếp tục dùng truy vấn theo chủ sở hữu. Bộ lọc, pager và nhãn thanh toán hiệu lực bổ sung chỉ áp dụng cho Admin. Mã ghi trạng thái và lập đơn giữ nguyên. Phạm vi này dùng schema và index hiện hành.

## File trong checkpoint

- `BoLocDonHangAdmin.java`, `AdminDonHang_Controller.java`, `DonHang_Service.java`, `DonHang_Repository.java`, `ThanhToanMomo_Repository.java` triển khai bộ lọc, phân trang và quy tắc trạng thái chung.
- `NguonDieuHuong.java`, `Momo_Controller.java` và `fragments/nav-fields.html` giữ ngữ cảnh danh sách qua GET và PRG. `Momo_Controller` thuộc phạm vi này vì dùng cùng trang chi tiết và đường về từ form đối soát.
- `order/list.html`, `order/detail.html`, `admin.css` hiển thị bộ lọc, count, pager và anchor.
- `AdminOrderList_MariaDb_Test.java` dùng fixture nhiều lượt thanh toán. `Momo_MariaDb_Test.java` kiểm PRG đối soát. `OAuth2Callback_MariaDb_Test.java` kiểm quyền sở hữu đơn với các phiên cùng ID.
- Sổ triển khai, roadmap và bằng chứng trong `docs/qa/ORDER-01/` và `docs/ui/screenshots/ORDER-01/`.

## Kiểm chứng

- JDK 21, `mvn -o -B -ntp test` có 238/307 ca đạt, 69 skip theo profile, 0 lỗi. `mvn -o -B -ntp -DskipTests package` thành công. [Kết quả Maven](../qa/ORDER-01/maven-results.txt)
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/ORDER-01/run-fixture.ps1 -Integration` khởi tạo MariaDB 12.2 riêng, áp dụng Flyway rồi chạy fixture G10, P20 dashboard/KPI, COD, MoMo mock và xác thực. 37/37 ca đạt. Các URL danh sách, số kết quả, một dòng mỗi đơn, ưu tiên `PAID`, phân trang và PRG được kiểm từ MVC tới MariaDB. [Kết quả tích hợp](../qa/ORDER-01/integration-results.txt)
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/ORDER-01/run-fixture.ps1` chạy JAR profile `dev` và Chrome headless. Ba nhóm kiểm tra đạt ở chiều rộng 390/1280 px khi tắt JavaScript. Tab và Space thao tác checkbox, Tab và Enter gửi form. Trang 2 giữ bộ lọc, đường về đưa dòng đã mở vào vùng nhìn thấy, xóa lọc khôi phục 20 đơn/trang. [Kết quả trình duyệt](../qa/ORDER-01/browser-results.json) · [Bộ lọc mobile](../ui/screenshots/ORDER-01/mobile-filters.webp) · [Bộ lọc desktop](../ui/screenshots/ORDER-01/desktop-filtered.webp) · [Vị trí sau khi quay lại](../ui/screenshots/ORDER-01/mobile-return.webp)

## Giới hạn xác minh

- OAuth Google/GitHub dùng callback fixture. MoMo dùng mock, gồm đối soát và IPN trong các ca hồi quy. Chưa kiểm với merchant hay OAuth thật.
- G10 dùng MariaDB riêng cho dữ liệu đơn và thanh toán. Fixture trình duyệt để MongoDB ngoại tuyến, nên phần KPI đánh giá trên dashboard dùng trạng thái thiếu dữ liệu hiện hành.
- Giao diện responsive được kiểm trên viewport mô phỏng bằng Chrome headless. Chưa kiểm trên thiết bị vật lý.
- Chưa đo chi phí truy vấn với dữ liệu vận hành hoặc trang sâu trên tập lớn. Cần số đo riêng trước quyết định index.
- Nếu cần quay lui, hoàn nguyên phần truy vấn đọc, form và điều hướng ORDER-01 theo diff. Checkpoint không cần thao tác với dữ liệu.

Gói kế tiếp theo lịch là [REVIEW-01](implementation-prompts/11-REVIEW-01.md), chờ giao riêng. QA-01 dùng bộ lọc mới và kiểm Back từ trang chi tiết. DTO-01 cần giữ projection một dòng/đơn và cùng predicate của count.
