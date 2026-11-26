# UI-01P · Phân trang GET

Checkpoint `main` tại `6368b6723d425c23d8901a5ebc69bbaea3ac09b4` cộng working diff. Đối chiếu G4 và G5 trong sổ triển khai và giữ các thay đổi đã có. Diff staged trống.

## Thay đổi

- `dto/PhanTrang.java`, `fragments/ui.html` và `components.css` cung cấp một pager GET. Có Đầu/Trước, số hiện tại và lân cận, dấu … khi thiếu trang giữa, Sau/Cuối, cùng chỉ báo `aria-current`. Ẩn điều hướng không có đích và ẩn cả thanh với 0 hoặc 1 trang. Cửa sổ ±2 ở desktop, ±1 ở mobile.
- `NguonDieuHuong.java`, `CuaHang_Controller.java`, `AdminNguoiDung_Controller.java`, `AdminSauMua_Controller.java` và bốn template `product/{list,detail}.html`, `admin/user/{list,reviews}.html` áp dụng pager. Link giữ query theo từng màn, gồm `filterPanel` của G5, và review PDP dùng riêng `reviewPage` + `variant` + `#danh-gia`. Form lọc GET quay về trang đầu; trang đích giữ context path khi render Thymeleaf.
- `SauMua_Service.java`, `DanhGiaMongo_Repository.java` và `MongoDanhGia_Store.java` dùng chung đọc lại tối đa một lần nếu page vượt sau khi có count. Lỗi kho vẫn hiển thị tạm thời không khả dụng. Truy vấn công khai bổ sung thứ tự `ngayTao DESC, _id DESC` như Admin. SQL catalog và tài khoản tiếp tục lọc, count và sắp xếp ở DB. Số mục mỗi trang giữ theo hợp đồng 12 mặc định và cap 48 cho catalog, 15 cho tài khoản, 10 cho review PDP, 20 cho review Admin.
- Cập nhật `GiaoDienPagesTest`, `AdminDanhGiaPagesTest`, `XacThuc_MariaDb_Test`, `MongoDanhGiaStoreNghiTest`, `MongoDanhGia_Store_Test` và `SauMuaDanhGiaServiceTest`. Thêm fixture QA riêng cho Chrome, SQL và MongoDB.

## Hợp đồng bàn giao

- ORDER-01 dùng `PhanTrang.tu(Page, IntFunction<String>)`, đặt record vào model và render `fragments/ui :: pagination(pager, label)`. `PhanTrang.doc` giữ nguyên predicate khi đọc trang gần nhất. Số trong URL bắt đầu ở 0, số hiển thị bắt đầu ở 1. Trình tạo link nhận route nội bộ và bộ lọc có kiểu hoặc allowlist.
- REVIEW-01 giữ `status/stars/product/page` qua PRG và giữ `trang.khongTaiDuoc` cho kết quả lỗi Mongo. PDP tách `reviewPage` khỏi `page` của nguồn catalog. Các link catalog vẫn thuộc `[data-catalog-filter-pages]`.
- Danh sách nhỏ, bảng dashboard, giỏ, wishlist, đổi trả, voucher, SKU và audit giữ cách đọc hiện hành. Phân trang đơn Admin nằm trong ORDER-01. Đo tải và độ hữu ích trước khi mở rộng sang các danh sách còn lại.

## Kiểm thử và quay lui

JDK Temurin 21.0.13.1, Maven 3.9.15. Fixture khởi tạo MariaDB và MongoDB cục bộ độc lập.

- `mvn -o test` đạt **235/295**, 60 skip theo profile, 0 lỗi. `mvn -o -DskipTests package` thành công. [Log Maven](../qa/UI-01P/maven-results.txt)
- `run-fixture.ps1 -Integration` chạy các suite MockMvc và MariaDB/MongoDB với **90/92** đạt, 2 skip. Hai ca skip thuộc fixture sao lưu và khởi động lại Mongo của DATA-01. Bài test timestamp trùng kiểm 22 đánh giá công khai qua ba trang và 21 đánh giá lọc Admin qua hai trang. Count loại đúng bản chờ duyệt, sản phẩm khác và sai số sao theo từng bộ lọc. [Log tích hợp](../qa/UI-01P/integration-results.txt)
- `run-fixture.ps1` chạy Chrome headless tại 390 và 1366 CSS px với catalog 9 trang, 43 đánh giá fixture cùng thời điểm và tài khoản Admin tạo riêng. Quan sát pager ẩn ở 0/1 trang, cửa sổ số đúng ở desktop/mobile, Trang trước bằng Tab/Enter khi tắt JS, Back về trang cũ, filter/sort/size và `filterPanel` đúng qua link. PDP giữ SKU 10 và neo đánh giá khi đổi trang. Review PDP/Admin đọc lại trang cuối khi yêu cầu trang 99. Account list ẩn pager khi lọc còn một tài khoản. [Kết quả trình duyệt](../qa/UI-01P/browser-results.json) · [Ảnh pager mobile](../ui/screenshots/UI-01P/catalog-390-nojs.webp)
- `git diff --check` đạt. Không stage, commit hoặc push.

Nếu cần quay lui, phục hồi riêng fragment và caller pager trước, giữ allowlist URL G4/G5. Kiểm lại count, trạng thái outage và đường về đúng bộ lọc sau khi phục hồi giao diện.

Kiểm chứng trình duyệt dùng tài khoản và dữ liệu fixture. Chưa chạy trên thiết bị thật hoặc đo hiệu năng deep-page với dữ liệu vận hành.
