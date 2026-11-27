# ADMIN-UX-BIZ-01 · Kết quả

## Phạm vi và working tree

- Bắt đầu từ `daea756053021310d9fd1c6ada9475564711fac6` trên `main`, theo dõi `origin/main`. Index ban đầu trống. Working tree có 44 tệp tracked đã sửa cùng các tệp chưa theo dõi từ BIZ-UX, UI-REDESIGN và hồ sơ trước đó.
- Dựng bản trước từ snapshot working tree ban đầu. Bản sau tiếp nối cùng thay đổi có sẵn. HEAD và index giữ nguyên, không commit, push, deploy hay dùng database đang chạy của người dùng.
- Rà soát README, SRS, Business Rules, ERD v5, thiết kế giao dịch, Decisions, báo cáo BIZ-UX và UI-REDESIGN. Tệp ảnh trong `docs/ui/references/` thuộc gói storefront cũ. Ảnh trước/sau dưới đây lấy từ các ứng dụng fixture chạy thật.

## Giao diện

| Khu vực | Phát hiện ở bản trước | Thay đổi |
|---|---|---|
| Footer | Link và giới thiệu nằm rải rác, chưa có bản đồ và cấu hình liên hệ. Admin dùng footer thương mại. | Footer charcoal `#0b0a0b`, chữ trắng ngà, icon champagne gold. Có thương hiệu, Khám phá, Hỗ trợ, địa chỉ liên hệ và iframe Google Maps 240 px. Link dự phòng giữ nguyên địa chỉ **12 Nguyễn Văn Bảo, Gò Vấp, Thành phố Hồ Chí Minh, Việt Nam**. Các màn Admin dùng footer nhỏ. Wishlist dùng chung Footer mới. |
| Danh sách đơn | Danh sách dạng dòng, chỉ có mã, tiền và trạng thái. | Bảng có tên người đặt, số **lượng sản phẩm** (tổng các dòng), ngày giờ Việt Nam, tổng VND, hai trạng thái độc lập và đích chi tiết. Truy vấn danh sách lấy tên và lượng hàng cùng dữ liệu trang. KPI toàn hệ thống lấy từ DB. Giữ các lọc cũ, sắp xếp và phân trang. Thêm `fromDate` và `toDate` theo ngày Việt Nam, gồm cả ngày kết thúc. |
| Chi tiết đơn | Thông tin giao hàng, tiền và sản phẩm trải dọc, các nút chủ yếu ghi nhãn trạng thái. | Breadcrumb giữ bộ lọc, trang và điểm quay lại. Card tổng quan, giao hàng, bảng sản phẩm, thanh toán và xử lý đơn. Giá, tên, mã, biến thể, số lượng và địa chỉ lấy từ snapshot. Ảnh SKU hiện hành với biểu tượng dự phòng; ảnh lỗi hay SKU ngừng bán vẫn mở được đơn. Giờ giao lấy từ trường giao thành công khi có. Các nút theo bước hợp lệ hiện hành, với xác nhận khi hủy COD hoặc xác nhận đã giao. |
| Catalog/SKU | Danh sách lặp thao tác, thiếu ngữ cảnh tồn, tình trạng khóa và nhóm biểu mẫu. | Bảng sản phẩm có ảnh đại diện từ SKU, ID, danh mục, thương hiệu và tổng số SKU. Bảng SKU có ảnh, giá, tồn, giữ, khả dụng, mã lô/HSD tham khảo và hành động theo trạng thái. Form chia năm nhóm và có preview ảnh chọn. Các bảng có vùng cuộn ngang riêng và tiêu đề cột. |

Footer dùng các route đang phục vụ. Những mục gắn với tài khoản hiện theo quyền. Link bản đồ dùng [Google Maps URLs Search](https://developers.google.com/maps/documentation/urls/get-started#search-action).

### Cấu hình liên hệ

| Biến môi trường | Hiển thị khi hợp lệ |
|---|---|
| `BEAUTYSHOP_CONTACT_PHONE` | Số liên hệ và link `tel:` |
| `BEAUTYSHOP_CONTACT_EMAIL` | Địa chỉ email và link `mailto:` |
| `BEAUTYSHOP_ZALO_URL` | Trang Zalo HTTPS thuộc `zalo.me` |
| `BEAUTYSHOP_FACEBOOK_URL` | Trang Facebook HTTPS thuộc `facebook.com` |
| `BEAUTYSHOP_INSTAGRAM_URL` | Trang Instagram HTTPS thuộc `instagram.com` |

Owner cần cung cấp các giá trị chính thức. Footer hiện bỏ qua biến trống, sai định dạng hoặc URL mạng xã hội thiếu đường dẫn. Các link mạng xã hội mở tab mới với `rel="noopener noreferrer"`.

## Quy tắc ngừng hoạt động

- Trạng thái đơn chặn gồm `PENDING_PAYMENT`, `PENDING_CONFIRMATION`, `CONFIRMED`, `PACKING`, `SHIPPING`. Trạng thái chưa nhận diện cũng chặn. `DELIVERED` cần giao dịch `PAID` khớp phương thức và tổng tiền. `CANCELLED`, `EXPIRED` cần các nghĩa vụ liên quan đã chốt. Thanh toán `PENDING`/`REFUND_PENDING`, đơn đã hủy/hết hạn còn giao dịch `PAID`, lượt giữ hàng/voucher `HELD` và dòng đổi trả `PENDING`/`APPROVED`/`RECEIVED` đều chặn.
- Đếm mã đơn phân biệt qua các dòng liên quan, kể cả SKU đã tắt hoặc hết tồn. Áp dụng cả thương hiệu vì tắt thương hiệu loại sản phẩm khỏi storefront, giống danh mục.
- Kiểm lại trạng thái tại service cho cả form sửa, route trạng thái và route `/delete` cũ. Khi cờ sản phẩm đang bật, chuyển vào danh mục/thương hiệu đã tắt cũng phải qua kiểm tra đơn liên quan. Form báo cạnh cờ hoạt động hoặc trong alert. Thao tác riêng trả về trang danh sách với thông báo. Các thao tác ghi giữ kiểm role và CSRF.
- Checkout khóa ghi SKU theo thứ tự mã và dùng khóa đọc cho hàng cha khi lấy giá/tồn mới. Catalog Admin khóa ghi đúng hàng cần chỉnh ở `READ_COMMITTED`, rồi đếm đơn qua truy vấn đọc thông thường. Admin không lấy thêm khóa hàng đơn hay khóa từng SKU khi tắt cha. Fulfillment và đối soát giữ thứ tự khóa đơn → SKU. Thương hiệu và danh mục dùng khóa đọc khi sản phẩm đổi nhóm để tuần tự hóa với thao tác ngừng nhóm.
- Fulfillment và ẩn catalog là hai thao tác độc lập. Refund/đổi trả hợp lệ có thể phát sinh sau khi đã ẩn, và vẫn xử lý qua đơn lịch sử. Nếu cần thu hồi khẩn cấp vì an toàn, dùng quy trình riêng có quyền, audit, truy xuất SKU/lô, chặn đơn mới và hướng dẫn xử lý đơn đã đặt.

## Kiểm toán lô và phạm vi triển khai

| Câu hỏi | Hiện trạng xác minh trong mã |
|---|---|
| Mã lô lưu ở đâu? | `bien_the_san_pham.ma_lo_minh_hoa` và `han_su_dung_minh_hoa` trong V1. |
| Một SKU có nhiều lô? | Một cặp metadata tham khảo trên mỗi SKU. |
| Tồn kho theo cấp nào? | SKU, qua `so_luong_trong_kho` và `so_luong_tam_giu`. |
| HSD có được kiểm lúc bán? | Checkout kiểm hoạt động, giá, tồn khả dụng và thời hạn giữ MoMo, không dùng HSD minh họa. |
| Đơn lưu lô bán thực tế? | Dòng đơn chốt SKU, tên, giá và số lượng. Dấu trừ COD và lượt giữ tồn lưu theo đơn–SKU. |
| Nhập/xuất theo lô? | Điều chỉnh tồn và xuất/hoàn theo SKU. Đổi trả hiện chỉ chuyển trạng thái yêu cầu. |
| Có FEFO? | Chưa có cấp phát theo HSD. |
| Có chặn lô hết hạn? | Chưa có chính sách loại tồn hết hạn theo lô. |

SRS mục phạm vi và D13 ghi nhiều lô/FEFO ngoài MVP. Yêu cầu mới cho phép giữ kho theo SKU nếu còn thiếu kiểm chứng bất biến. Phần tồn theo lô, giao diện nhập lô thực và FEFO được bàn giao thiết kế ở checkpoint. Chưa tạo V8, chưa tạo lô từ metadata cũ. Cần đối chiếu tồn vật lý cùng tác động giao dịch trước khi chuyển SKU sang nguồn tồn theo lô. [Thiết kế và điều kiện kích hoạt](ADMIN-UX-BIZ-01-tiep-tuc.md)

## Tệp thay đổi

- Nghiệp vụ và truy vấn: `LienHeCuaHang`, `GiaoDien_Advice`, `NguonDieuHuong`, `BoLocDonHangAdmin`, `MucCatalogAdmin`, `BienTheSanPham`, `NgungHoatDongCatalog_Service`, `BienTheSanPham_Service`, `DanhMucSanPhamThuongHieu_Service`, `DonHangCatalog_Repository`, `DonHang_Repository`, `GioHang_Repository`, `SanPham_Repository` và `DanhMucSanPhamThuongHieu_Repository`.
- Điều hướng và action: `AdminDonHang_Controller`, `Momo_Controller`, `AdminBienTheSanPham_Controller` và `AdminDanhMucSanPhamThuongHieu_Controller`.
- Giao diện: `fragments/footer.html`, `fragments/admin-table.html`, `order/list.html`, `order/detail.html`, hai màn `admin/catalog`, hai màn `admin/variant`, `admin.css`, `storefront.css` và `app.js`.
- Test: `LienHeCuaHangTest`, `CatalogDonHang_MariaDb_Test`, `AdminOrderList_MariaDb_Test`, `BienTheSanPham_MariaDb_Test`, `DanhMucSanPhamThuongHieu_MariaDb_Test` và `DocView_MariaDb_Test`.
- Cấu hình và tài liệu: `.env.example`, SRS FR-02, Business Rules BR21, ERD và thiết kế giao dịch, Decisions D40, roadmap, hai báo cáo và thư mục QA. [Danh sách tệp nguồn so với snapshot đầu phiên](../qa/ADMIN-UX-BIZ-01/revision.json)

## Kiểm chứng

| Nhóm | Kết quả |
|---|---|
| `mvn test` | **PASS**. 243 đạt, 81 SKIPPED theo điều kiện môi trường, 0 lỗi. [Tóm tắt](../qa/ADMIN-UX-BIZ-01/maven-test.txt) |
| `mvn -DskipTests package` | **PASS**. [Tóm tắt](../qa/ADMIN-UX-BIZ-01/maven-package.txt) |
| MariaDB 12.2 / MongoDB 6.0 | **PASS 78/78** trên các suite đã chọn. Có ORM projection, COD, MoMo mock/IPN, state machine, voucher, giỏ, kho, catalog, đổi trả, Mongo review, RAG fixture và phân quyền. Flyway tạo schema trống và xác thực đủ V1–V7. [Integration](../qa/ADMIN-UX-BIZ-01/maven-integration.txt) · [Flyway](../qa/ADMIN-UX-BIZ-01/flyway.txt) |
| Ngừng hoạt động đồng thời | **PASS 16/16** trong suite catalog. Bốn cấp SKU, sản phẩm, danh mục, thương hiệu × COD/MoMo mock × checkout/Admin giữ khóa trước. Khi checkout thắng, ĐB có một đơn và lượng trừ/giữ đúng. Khi Admin thắng, checkout rollback, giữ nguyên tồn và không tạo đơn. |
| HTTP bản trước, bản sau và cấu hình liên hệ | **PASS**. Thật qua HTTP/PRG. Đơn fixture 165.000 ₫, một dòng, tồn 20 → 19 và một lượt voucher. Bản sau kiểm cấu trúc bảng, tên nhãn, link quay lại, ảnh SKU, chặn POST trực tiếp, role và CSRF. [Trước](../qa/ADMIN-UX-BIZ-01/before-http.json) · [Sau](../qa/ADMIN-UX-BIZ-01/after-http.json) · [Năm kênh liên hệ fixture](../qa/ADMIN-UX-BIZ-01/after-contact-http.json) |
| Độ tương phản | **PASS 12/12** cho các cặp màu chính, thấp nhất 5,56:1. [Số đo](../qa/ADMIN-UX-BIZ-01/contrast.json) |
| Browser trước/sau và reflow | **PASS 66/66** phép đo trên 6 màn/khối ở 320, 360, 390, 768, 1024, 1280, 1440, 1920 px và ba cấu hình tương đương reflow 200% bằng chiều rộng CSS giảm một nửa. Tab, Enter mở xác nhận, Escape trả focus, viền focus, tên bảng/cột trong cây accessibility, nút SKU bị khóa và preview ảnh đều đạt. [Trước](../qa/ADMIN-UX-BIZ-01/before-browser.json) · [Sau](../qa/ADMIN-UX-BIZ-01/after-browser.json) |
| `git diff --check` | **PASS**. Index không đổi, diff migration trống. [Kiểm Git](../qa/ADMIN-UX-BIZ-01/git-check.txt) |

Lượt tích hợp đầu phát hiện các kỳ vọng cũ cho phép tắt khi còn đơn, và một truy vấn đếm giữ kết nối khi lặp nhiều lần. Đã cập nhật kỳ vọng theo BR21, đóng vòng đời kết quả JDBC bằng `list()`, rồi chạy lại nhóm tích hợp trên datadir mới.

### Ảnh trước/sau

| Màn | Trước | Sau |
|---|---|---|
| Footer | [Trước](../ui/screenshots/ADMIN-UX-BIZ-01/before-footer.webp) | [Sau](../ui/screenshots/ADMIN-UX-BIZ-01/after-footer.webp) |
| Đơn hàng | [Trước](../ui/screenshots/ADMIN-UX-BIZ-01/before-orders.webp) | [Sau](../ui/screenshots/ADMIN-UX-BIZ-01/after-orders.webp) |
| Chi tiết đơn | [Trước](../ui/screenshots/ADMIN-UX-BIZ-01/before-detail.webp) | [Sau](../ui/screenshots/ADMIN-UX-BIZ-01/after-detail.webp) |
| Catalog | [Trước](../ui/screenshots/ADMIN-UX-BIZ-01/before-catalog.webp) | [Sau](../ui/screenshots/ADMIN-UX-BIZ-01/after-catalog.webp) |
| SKU | | [Bảng](../ui/screenshots/ADMIN-UX-BIZ-01/after-variants.webp) · [Form](../ui/screenshots/ADMIN-UX-BIZ-01/after-form.webp) |

### Phần còn mở khi release

- Chrome kiểm khung iframe, địa chỉ, lazy loading, kích thước và liên kết dự phòng trên fixture. Máy kiểm không mở được Google Maps bên ngoài, nên vị trí pin và tải bản đồ trực tiếp là **BLOCKED**. Ảnh Footer sau thể hiện trạng thái mạng ngoài không khả dụng. Sau khi có giá trị liên hệ chính thức cần kiểm tra đích các link trên mạng thật.
- Zoom native 200%, trình đọc màn hình có tiếng và MoMo Sandbox thật **SKIPPED** trong lượt này. Chrome dùng đo reflow tương đương và đọc cây accessibility. MoMo chạy với gateway mock và các bài test IPN/đối soát nội bộ.
- Quản lý lô/FEFO còn ở bước thiết kế. Đơn legacy đã giao mà thiếu thanh toán khớp và giao dịch `REFUND_PENDING` tiếp tục chặn đến khi đối soát. Quy trình hoàn tiền vận hành vẫn theo phạm vi hiện hành.
