# WISH-01 · Yêu thích tại chỗ

Baseline `main` và `origin/main` tại `38fb1ca300ce9903db033da13ee4105588ea394a` cộng diff CART-01/CAT-01 đã nghiệm thu. G2, G4, G5 và G8 đã đạt. Phần thay đổi thực hiện trong lượt này chỉ thuộc WISH-01.

## Thay đổi

- `YeuThich_Repository.java` và `SauMua_Service.java` đọc membership theo Customer và tập ID. Home gom ba dải trong một tập distinct, catalog đọc theo trang, PDP đọc theo ID sản phẩm và wishlist dùng các mục hiện có từ query của chủ. Cặp ghi thêm/bỏ giữ khóa unique và đọc lại trạng thái thực trong transaction. Nếu sản phẩm mất điều kiện hiển thị, bổ sung kết quả xung đột khi thêm. Mục đã lưu vẫn có thể bỏ.
- `SauMua_Controller.java` giữ hai POST cũ theo trạng thái đích, có PRG về origin cho form và JSON tối thiểu khi gửi `X-Wishlist-Partial: 1`. `GET /account/wishlist/state` dùng để đọc lại sau sự cố và đồng bộ khi tab quay về. Giữ ROLE_CUSTOMER, kiểm phiên hiện hành và CSRF của POST.
- `NguonDieuHuong.java`, `Bootstrap_Controller.java`, `CuaHang_Controller.java` và `ThongBaoGopGio.java` dựng đích GET từ allowlist, giữ query/pager, biến thể, anchor và thông báo gộp giỏ đang chờ qua GET trung gian. Guest đến login theo saved request, quay lại nguồn và chủ động chọn thao tác. POST form dùng đích dựng từ cùng hợp đồng. URL đích bỏ tham số ngoài allowlist.
- `fragments/wishlist.html`, `fragments/product-card.html`, `home/index.html`, `product/{list,detail}.html`, `auth/wishlist.html` và `storefront.css` dùng tim cùng trạng thái, giữ thẻ và link độc lập. Trạng thái lưu có tim đầy đỏ, `aria-pressed` và nhãn Bỏ yêu thích. Catalog có thêm ID ổn định `catalog-product-{id}`.
- `wishlist.js` khóa các form trùng ID trong lúc ghi, cập nhật từ phản hồi server và đọc lại sau mất phản hồi với tối đa một GET theo sản phẩm. Không gửi lại POST. Các tab nhận tín hiệu kiểm tra hoặc đọc lại khi focus/Back. Khi mục cuối bị bỏ, giữ trang và đưa focus về tiêu đề. Membership không nằm trong browser storage.
- Cập nhật `GiaoDienPagesTest.java`, `SauMua_MariaDb_Test.java` và `OAuth2Callback_MariaDb_Test.java`. Thêm fixture MariaDB/MongoDB và Chrome tại `docs/qa/WISH-01/`.

## Hợp đồng cho các gói sau

DTO-01 giữ ID sản phẩm dạng `Long` ở projection và chuỗi ID trong JSON membership để tránh mất độ chính xác trong JS. QA-01 có thể chạy lại các ca G9 từ fixture. REVIEW-01 tái sử dụng cơ chế PRG/CSRF riêng của review và không phụ thuộc vào JSON wishlist.

Khi phải quay lui enhancement, các form server vẫn đặt trạng thái định trước và dùng PRG về cùng nguồn. Giữ cùng bảng và ràng buộc unique. Wishlist chưa phân trang theo phạm vi danh sách nhỏ.

## Nghiệm thu G9

- Temurin 21.0.13.1 và Maven 3.9.15. `mvn -o test` đạt **238/305**, 67 skip theo profile, 0 lỗi. `mvn -o -DskipTests package` thành công. [Log Maven](../qa/WISH-01/maven-results.txt)
- `run-fixture.ps1 -Integration` đạt **97/97** trên MariaDB và MongoDB độc lập, gồm kiểm quyền, CSRF, unique, hai chủ, sản phẩm/SKU vừa tắt trả 409 và bỏ được mục cũ. Hai phiên đã bị thu hồi đều bị chặn, phiên mới đọc và thao tác trên cùng tài khoản. Form Login và callback Google/GitHub mô phỏng thấy cùng wishlist theo ID nội bộ. [Log tích hợp](../qa/WISH-01/integration-results.txt)
- `run-fixture.ps1` kiểm Chrome 158 tại 390 CSS px. Guest quay đúng bộ lọc/anchor qua login, giữ thông báo gộp giỏ và không tự lưu. Form no-JS lưu và bỏ về đúng catalog. Ba thẻ Home đồng bộ, bấm liên tiếp chỉ gửi một POST. PDP giữ SKU, hai tab và Back đọc lại trạng thái. Khi chặn phản hồi sau khi server ghi, một GET đối soát trạng thái mà không gửi lại POST. Bỏ mục cuối giữ trang và đưa focus về tiêu đề. [Kết quả Chrome](../qa/WISH-01/browser-results.json) · [Ảnh PDP](../ui/screenshots/WISH-01/pdp-390-saved.webp)
- `git diff --check` đạt. Giữ diff CART-01/CAT-01, ngoài điểm chuyển tiếp notice CART-01 đã mô tả. Staged diff trống. Chưa kiểm OAuth nhà cung cấp thật hoặc thiết bị vật lý.
