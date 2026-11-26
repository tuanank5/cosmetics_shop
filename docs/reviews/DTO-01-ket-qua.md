# DTO-01 · Projection cho catalog, địa chỉ và tài khoản

Ngày 26/11/2026. Baseline `main` tại `38fb1ca300ce9903db033da13ee4105588ea394a` cùng working diff G7–G11. G3 và các màn liên quan đã tích hợp. Staging trống khi bắt đầu.

## Kiểm kê và quyết định

| Màn | Trường thực dùng | Nguồn đọc và thay đổi |
|---|---|---|
| Sổ địa chỉ và checkout | Sổ địa chỉ dùng mã, người nhận, điện thoại, địa chỉ và cờ mặc định. Checkout dùng cùng dữ liệu để chọn địa chỉ, thiếu nhu cầu hiển thị điện thoại. | `DiaChiHienThi` đọc 5 cột theo ID chủ, sắp xếp theo mã. Trước đây JPA tải 6 cột gồm FK chủ. Dùng chung một projection cho hai caller. Form chỉnh sửa, cập nhật mặc định và snapshot lúc lập đơn tiếp tục kiểm quyền trên entity. |
| Danh sách catalog Admin | Mã, tên, slug và trạng thái hoạt động. | `MucCatalogAdmin` chọn 4 cột. Danh sách sản phẩm trước đây lấy 11 cột gồm mô tả, thành phần và hướng dẫn. Giữ thứ tự tên rồi mã. |
| Tùy chọn form catalog Admin | Mã, tên và cờ hoạt động để gắn nhãn với mục đã ngừng. | Dùng lại projection danh sách danh mục và thương hiệu. Bảng có 4 cột. Giữ tùy chọn ngừng hoạt động để form sửa tiếp tục hiện dữ liệu cũ. |
| Bảo mật Customer và chi tiết tài khoản Admin | Email, có mật khẩu, đã xác minh email, tên nhà cung cấp OAuth. Admin cần thêm họ tên, vai trò, cờ kích hoạt để hiển thị và xác định nút hỗ trợ. | Tách projection 3 và 6 trường. Câu truy vấn trả boolean có mật khẩu và đã xác minh. Truy vấn OAuth lấy riêng tên nhà cung cấp. `lienKetCho` giữ model key và chỉ chứa email xác nhận. Bằng chứng OAuth đầy đủ ở session, dùng bởi POST xác nhận. |
| Catalog công khai và home | Mã, slug, tên, hãng, giá, ảnh, khả dụng. | Giữ projection `TheSanPham` và truy vấn theo lô cho sao và tim. Bộ lọc công khai tiếp tục dùng `TuyChon`. |
| Chi tiết sản phẩm | Sản phẩm, hãng/danh mục, nội dung, SKU, giá, ảnh, số lượng khả dụng, lô và hạn minh họa. | Giữ `ChiTiet` và `EntityGraph` đang nạp hãng/danh mục. Trên fixture, thay 1 bằng 7 SKU vẫn dùng 2 SELECT JPA. SKU chọn và đường GET/PDP hiện hành giữ cùng hợp đồng. |
| Admin SKU | Tên sản phẩm, mã giao tiếp với form, SKU, ảnh, giá, tồn, tạm giữ, lô, hạn và trạng thái. | Giữ entity SSR vì danh sách SKU dùng gần hết dữ liệu. Trên fixture, 1 và 7 SKU đều dùng 3 SELECT JPA gồm kiểm phiên. GET form và ghi dùng hợp đồng hiện hành. |
| Danh sách tài khoản, đơn, review và P20 | Các trường, bộ lọc và trạng thái theo từng màn. | Giữ projection/record đang dùng, gồm `TomTat`, `DongDonAdmin` và các record review/KPI. |

## Kiểm chứng

- `DocView_MariaDb_Test` chạy trước và sau thay đổi trên MariaDB 12.2 với cùng seed. Hibernate `StatementInspector` đếm SELECT trên request đồng bộ. Fixture so từ 1 lên 7 dòng/SKU và chạy với `open-in-view=false`. Số SELECT JPA ổn định ở các màn đo. 13 SHA-256 HTML khớp sau chuẩn hóa CRLF và giá trị CSRF. Số đo bao gồm truy vấn kiểm phiên và giới hạn ở Hibernate. [Đối chiếu](../qa/DTO-01/comparison.txt) · [Trước](../qa/DTO-01/query-before.txt) · [Sau](../qa/DTO-01/query-after.txt)
- `OAuth2Callback_MariaDb_Test` kiểm xác nhận liên kết sau callback. Model `lienKetCho` là chuỗi email, JSON của riêng giá trị này cũng chỉ là chuỗi. HTML giữ đích POST xác nhận. Subject, tên hồ sơ OAuth và hash không cần cho view. Các luồng reauth, xác nhận và thu hồi phiên tiếp tục chạy qua test tích hợp.
- JDK 21 chạy `mvn -o -B -ntp test` với 239/309 ca đạt, 70 bỏ qua theo profile và 0 lỗi. `mvn -o -B -ntp -DskipTests package` thành công. [Maven](../qa/DTO-01/maven-results.txt)
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/DTO-01/run-fixture.ps1` dùng MariaDB mới. Tích hợp 144/144, gồm catalog/SKU, địa chỉ/checkout, COD, quyền và `CheckoutIntegrityTest`. Phép đo riêng đạt 1/1 mỗi lượt trước và sau. [Tích hợp](../qa/DTO-01/integration-results.txt)
- Chrome đạt 2/2 nhóm với `run-fixture.ps1 -Browser`, JAR dev, MariaDB riêng và JavaScript tắt. Viewport 390 px kiểm Tab/Enter, quyền sở hữu và đường quay lại checkout. Viewport 1280 px kiểm địa chỉ mặc định, báo giá và bố cục không tràn. [Chrome](../qa/DTO-01/browser-results.json) · [Địa chỉ mobile](../ui/screenshots/DTO-01/mobile-owned-address.webp) · [Báo giá desktop](../ui/screenshots/DTO-01/desktop-quote.webp)

## File và giới hạn

- `DiaChiHienThi.java`, `DiaChiNguoiDung_Repository.java`, `DiaChiNguoiDung_Service.java`, `Checkout_Controller.java` và `CheckoutPagesTest.java` chuyển phần đọc sổ địa chỉ và checkout.
- `MucCatalogAdmin.java`, `DanhMuc_Repository.java`, `ThuongHieu_Repository.java`, `SanPham_Repository.java`, `DanhMucSanPhamThuongHieu_Repository.java` và `DanhMucSanPhamThuongHieu_Service.java` thu hẹp danh sách và tùy chọn quản trị.
- `NguoiDung_Repository.java`, `DanhTinhOAuth_Repository.java`, `BaoMatTaiKhoan_Controller.java`, `AdminBaoMatTaiKhoan_Controller.java`, `MatKhau_Service.java` và `auth/security.html` thu hẹp dữ liệu đọc của hai màn tài khoản và dùng cùng quy tắc xác định điều kiện hỗ trợ.
- `DocView_MariaDb_Test.java`, `OAuth2Callback_MariaDb_Test.java`, `docs/qa/DTO-01/` và `docs/ui/screenshots/DTO-01/` ghi hợp đồng và bằng chứng G12. Sổ tiến độ và roadmap ghi checkpoint.

Chưa đo độ trễ trên catalog vận hành hay xác thực với nhà cung cấp OAuth thật. Fixture MariaDB chạy với MongoDB ngoại tuyến để giới hạn phép đo ở phần đọc SQL của gói này. Ca hồi quy MongoDB của REVIEW-01 đã chạy riêng ở G11. Có thể quay lui theo từng lát đọc và phần view tương ứng, dùng cùng schema. Gói theo lịch tiếp theo là [QA-01](implementation-prompts/13-QA-01.md), chờ giao riêng.
