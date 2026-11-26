# NAV-01 · Điều hướng và đường quay lại

Checkpoint: main, HEAD `6368b6723d425c23d8901a5ebc69bbaea3ac09b4` cộng working diff. G1/G3 đã đạt trước khi mở NAV-01. Không stage/commit/push. Chỉ làm một gói.

## Thay đổi

- `NguonDieuHuong.java` và `CuaHang_Controller.java` dựng đường về PDP từ danh sách nguồn nội bộ. Catalog giữ bộ lọc, sort/page/size; review pager và form SKU mang nguồn sang lần GET tiếp theo. Wishlist được dùng làm đích quay lại khi là Customer. URL tùy ý, token, state và callback không được lấy làm nguồn.
- `home/index.html`, `fragments/product-card.html`, `fragments/header.html`, `product/detail.html` và `home-carousel.js` phân biệt reset chương 1 với phục hồi. ID thẻ có vùng và mã sản phẩm, link quay lại là GET có anchor dùng được khi tắt JS. History mỗi entry chỉ lưu số chương và ID thẻ. Khi trang sống được BFCache trả lại, handler chỉ cập nhật trạng thái điều khiển carousel, không cuộn. Khi dựng lại trang, helper dùng anchor để đặt focus sau layout. Không đổi home-story.js.
- `DiaChiNguoiDung_Controller.java`, `order/checkout.html`, `auth/addresses.html` và `auth/address-form.html` giữ nguồn checkout qua lưu/hủy, lỗi form và danh sách. GET checkout đọc lại giỏ, địa chỉ thuộc chủ và tình trạng báo giá hiện hành. Không đưa giá trị form hay địa chỉ vào query quay về.
- `AdminDonHang_Controller.java`, `AdminSauMua_Controller.java`, `AdminPhieuGiamGia_Controller.java` và `Momo_Controller.java` giữ ngữ cảnh cho các link dashboard tới chi tiết đơn/trả/voucher và cho trạng thái danh sách đơn. Cập nhật template dashboard, order/list, order/detail, return/detail, admin/voucher/form cùng fragment nav-fields. Đường quay lại q/page của chi tiết tài khoản và bộ lọc review đã có từ baseline, được giữ lại.
- Cập nhật fixture trong BootstrapPagesTest, TrangChuPagesTest, GiaoDienPagesTest, AdminDashboardPagesTest, DonHangCOD_MariaDb_Test, XacThuc_MariaDb_Test và XacThucDaPhuongThuc_MariaDb_Test. Danh mục caller và fallback xem `docs/qa/NAV-01/routes.md`.

## Hợp đồng cho các gói sau

- Home: `home=opening` dành cho logo/Trang chủ. Anchor ổn định là `{region}-product-{id}` với ba vùng home-featured, home-popular và home-all-products. `from` là nguồn GET của thẻ PDP. UI-01C/UI-01P, WISH-01 và QA-01 bảo toàn các giá trị này khi đổi fragment hoặc kiểm Back.
- Checkout: `from=checkout` chỉ điều khiển đích GET của link và PRG. CART-01 giữ luồng báo giá theo giỏ hiện hành.
- Admin: `nav=dashboard` có kỳ hợp lệ. `listStatus` truyền danh sách trạng thái sang chi tiết, tách khỏi tham số cập nhật `status`. ORDER-01 giữ ngữ cảnh khi xử lý danh sách và phân trang.

## Kiểm chứng

JDK Temurin 21, Maven 3.9.15, profile dev. MariaDB chạy trên schema fixture riêng. Mongo để tắt trong lượt NAV-01. OAuth callback trong fixture, không gọi OAuth thật. Chrome headless dùng profile riêng và loopback.

- Lệnh: `mvn -o test`, `mvn -o -DskipTests package`, `powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/NAV-01/run-fixture.ps1 -Baseline` và chạy lại script bỏ `-Baseline`. Lượt baseline dùng JAR được lưu trước khi sửa tại `target/nav-01-before.jar`.
- Baseline tái hiện logo ở home giữ chương 4 và PDP luôn quay về `/products`. Sau sửa, logo từ home chương 4 và từ PDP đều mở chương 1. Nút Back của PDP mở đúng anchor popular, đưa focus về thẻ và giữ chương 6 sẵn cho khi cuộn ngược lên. Anchor mất thẻ đưa focus về tiêu đề vùng. Trace có path/hash, slide, scrollY, focus, loại điều hướng và `pageshow.persisted`. Xem `browser-before.json` và `browser-after.json`.
- Thử form login và saved request, checkout/trở về sau sửa địa chỉ, validation giữ input, các quyền sai chủ và CSRF, Back q/page Admin, callback LINK/REAUTH theo fixture AUTH-02. Trang direct link và target mới được kiểm độc lập với history entry cũ.
- Back về home dùng nhánh dựng lại trang, giữ vị trí cuộn 1.072px và focus đúng thẻ. Forward về PDP được Chrome trả từ BFCache (`pageshow.persisted=true`). Lượt này chưa quan sát home được trả từ BFCache. Từ mã, nhánh persisted của home chỉ gọi hàm cập nhật điều khiển. Tab mới, direct link và chế độ reduced motion đạt. Ở 390px với JavaScript tắt, Tab đưa focus tới Back, Enter mở đúng thẻ và không có cuộn ngang.
- Maven **230/289 đạt, 59 skip, 0 lỗi**. Package exit 0. Fixture baseline **1/1**, fixture sau sửa **30/30** kể cả LoginRedirectTest, CSRF/sai chủ và kiểm báo giá sau lưu địa chỉ. Chrome baseline/sau sửa exit 0. `git diff --check` không báo lỗi whitespace.

**NAV-01 DONE, G4 đạt trên checkpoint HEAD + working diff.**

Quay lui dự kiến nếu có tranh scroll: ngắt khôi phục JS trước, giữ link và anchor phía server rồi đo lại. Chưa chạy UI-01C. Báo cáo và bằng chứng lưu tại `docs/qa/NAV-01/` và `docs/ui/screenshots/NAV-01/`.
