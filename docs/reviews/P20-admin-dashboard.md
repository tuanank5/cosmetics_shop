# P20 — Admin Dashboard, điều hướng sau đăng nhập, icon tài khoản trên header

Trạng thái: **đã làm xong code, test và QA storefront; phần QA trình duyệt của Admin đang chờ owner đăng nhập** (xem §8). Chưa commit/push.

## 1. Phạm vi đã làm

| Hạng mục | Kết quả |
|---|---|
| Điều hướng sau đăng nhập | `ROLE_ADMIN` → `/admin/dashboard`. Nếu bị chặn ở một URL `/admin/**` hợp lệ cùng scheme/host/port thì quay lại URL đó, đã bỏ tham số đánh dấu `continue`. URL ngoài site hoặc ngoài `/admin/`, có `..`, `//`, `%2e/%2f/%5c` hay `\` đều bị bỏ qua. Luồng Customer giữ nguyên (gộp giỏ, saved request mặc định của Spring). |
| `/admin`, `/admin/` | Chuyển hướng tới `/admin/dashboard`. |
| Menu Admin | "Tổng quan" là mục đầu tiên, có `aria-current="page"` khi đang ở Dashboard. |
| Icon tài khoản | Nằm trái nhất trong header, trước logo và "Menu cửa hàng", trên cả storefront lẫn Admin. Guest thấy `<a href="/login">` chứa SVG `aria-hidden` và nhãn ẩn "Đăng nhập". Khi đã đăng nhập: `<form method="post" action="/logout">` có CSRF, chứa `<button>` với nhãn ẩn "Đăng xuất". Kích thước 44×44, có `:focus-visible`. Nhãn hiện dạng tooltip khi hover/focus. Nút chữ "Đăng xuất" cũ trong menu vẫn giữ. |
| `/admin/dashboard` | Bộ lọc kỳ, 6 thẻ KPI, 6 biểu đồ kèm bảng dữ liệu, 8 đơn mới nhất, SKU sắp hết, 3 bộ đếm việc cần xem. |
| Lớp phân quyền thứ hai | Thêm `@PreAuthorize("hasRole('ADMIN')")` (xem §5). |
| Seed demo | `scripts/seed-demo-orders.ps1` (xem §6). |

## 2. Định nghĩa số liệu

Mọi kỳ tính theo **ngày lịch Asia/Ho_Chi_Minh** (+7, không có giờ mùa hè). Kỳ `[từ, đến]` gồm cả hai đầu. Server đổi mốc 00:00 giờ VN sang UTC vì các cột thời gian đơn, thanh toán, đánh giá, đổi trả và voucher đều lưu UTC (`UTC_TIMESTAMP(6)`). Gom nhóm theo ngày dùng `CAST(TIMESTAMPADD(HOUR, 7, cột) AS DATE)`. Riêng `nguoi_dung.ngay_tao` dùng mặc định `CURRENT_TIMESTAMP` (giờ máy chủ DB, hiện là Asia/Saigon), nên khách mới được lọc theo mốc giờ địa phương.

**Kỳ trước** là khoảng có cùng số ngày, nằm ngay trước kỳ đang xem. Ví dụ 30 ngày gần nhất so với 30 ngày trước đó.

| KPI | Định nghĩa |
|---|---|
| **Doanh thu thực thu** | Tổng `don_hang.tong_thanh_toan` của các **đơn đã thu tiền**. Đơn đã thu tiền là đơn có lần thanh toán `PAID` với `so_tien = tong_thanh_toan` và không ở trạng thái `CANCELLED`/`EXPIRED`. Doanh thu được gán vào ngày thu (`MIN(ngay_thanh_toan)`): COD là ngày giao thành công, MoMo là lúc IPN báo thành công. Thanh toán đang hoàn (`REFUND_PENDING`/`REFUNDED`) không phải `PAID` nên không tính. Định nghĩa này giống tiền lệ `DonHang_Repository.tongQuan` (thanh toán PAID khớp tổng đơn) và nguyên tắc COD chỉ ghi nhận thu khi giao. Đổi trả không tự hoàn tiền theo business rules nên không trừ. Thẻ có chú thích ngắn về định nghĩa này. |
| **Số đơn** | Số đơn tạo trong kỳ (theo `ngay_tao`), trừ `PENDING_PAYMENT` (MoMo chưa trả) và `EXPIRED` (giữ hàng hết hạn). Đơn đã hủy vẫn tính. |
| **Giá trị đơn trung bình** | Doanh thu thực thu chia số đơn đã thu tiền trong kỳ, làm tròn HALF_UP tới đồng. |
| **Khách hàng mới** | Số tài khoản `vai_tro = 'CUSTOMER'` tạo trong kỳ. |
| **Tỉ lệ hủy** | Số đơn `CANCELLED` chia Số đơn (cùng định nghĩa như trên), 1 chữ số thập phân. So với kỳ trước theo **điểm phần trăm**. Mũi tên tăng có màu "xấu", nhưng chữ "Tăng/Giảm" luôn đi kèm. |
| **Đơn đang chờ xử lý** | Số hiện tại, không theo kỳ: các đơn `PENDING_CONFIRMATION` + `CONFIRMED` + `PACKING`. Bấm vào sẽ mở `/admin/orders?status=…` đã lọc sẵn. |

Phần trăm thay đổi = (kỳ này − kỳ trước) / kỳ trước × 100. Khi kỳ trước bằng 0, thẻ hiện "Kỳ trước chưa có dữ liệu để so sánh" thay vì chia cho 0. Mỗi thẻ có mũi tên ▲/▼/■ (`aria-hidden`) và chữ "Tăng x% / Giảm x% / Không đổi", nên không chỉ dựa vào màu.

Biểu đồ:

1. **Doanh thu và số đơn theo ngày.** Đường là doanh thu thực thu theo ngày thu. Cột là Số đơn theo ngày tạo. Ngày không có dữ liệu vẫn có dòng với giá trị 0.
2. **Đơn theo trạng thái.** Mọi đơn tạo trong kỳ, kể cả chờ thanh toán và hết hạn, để thấy toàn cảnh. Đủ 8 trạng thái, có cả cột tổng giá trị.
3. **Top 5 sản phẩm.** Xếp theo số lượng trong các đơn đã thu tiền trong kỳ. Doanh thu dòng = `don_gia × so_luong`, tính trước voucher và phí ship.
4. **Doanh thu theo danh mục.** Doanh thu dòng (trước voucher và phí ship) của các đơn đã thu tiền.
5. **Phương thức thanh toán.** Số đơn đã thu tiền và doanh thu thực thu, tách COD và MoMo.
6. **Phân bố số sao.** Số đánh giá tạo trong kỳ, gồm cả đã hiển thị lẫn đang chờ duyệt (`hien_thi = FALSE`).

Danh sách nhanh:

- **8 đơn mới nhất** theo `ngay_tao`, giờ hiển thị đổi sang giờ VN.
- **SKU sắp hết**: SKU đang bán (SKU và sản phẩm đều active) có khả dụng (`trong_kho − tam_giu`) ≤ ngưỡng. Ngưỡng mặc định 5, đổi được trong khoảng 0–1000. Tối đa 20 dòng, mỗi dòng có link tới trang sửa SKU.
- **Đánh giá chờ duyệt** (`hien_thi = FALSE`).
- **Đổi trả `PENDING`.**
- **Voucher đang bật hết hạn trong 7 ngày** tới (`now < ket_thuc_luc ≤ now + 7 ngày`, UTC).

## 3. Bộ lọc và kiểm tra đầu vào

`GET /admin/dashboard?range=7d|30d|90d` (mặc định `30d`), hoặc `?from=yyyy-MM-dd&to=yyyy-MM-dd`.

Các trường hợp bị từ chối:

- sai định dạng ngày;
- chỉ nhập một đầu;
- `from > to`;
- `to` ở tương lai (theo ngày VN);
- kỳ dài hơn 366 ngày tính cả hai đầu;
- `range` lạ;
- `nguong` ngoài khoảng 0–1000.

Mọi lỗi đều trả **200**, hiện thông báo tiếng Việt (`role="alert"`) và dùng kỳ 30 ngày, không bao giờ 500.

## 4. Kỹ thuật

- **`ThongKe_Repository` (JdbcClient, chỉ đọc).** Dùng CTE `don_da_thu` chung cho mọi số liệu doanh thu. Mỗi lần tải trang chạy **13 truy vấn gom nhóm**, không có N+1:
  - doanh thu kỳ này và kỳ trước trong một truy vấn;
  - số đơn và số hủy của hai kỳ;
  - khách mới;
  - 4 bộ đếm hiện tại trong một truy vấn;
  - doanh thu theo ngày;
  - đơn theo ngày;
  - đơn theo trạng thái;
  - top sản phẩm;
  - danh mục;
  - phương thức thanh toán;
  - số sao;
  - đơn mới;
  - SKU sắp hết.
- **`ThongKe_Service`.** Có `@Service`, `@Transactional(readOnly = true)` và `@PreAuthorize("hasRole('ADMIN')")` ở cấp lớp. Service nhận `Clock`, nên test cố định được ngày hiện tại.
- **Template** `templates/admin/dashboard.html`:
  - Mỗi biểu đồ có `<h2>`, đoạn mô tả, `canvas role="img"` với `aria-label` và `aria-describedby`.
  - Mỗi biểu đồ có `<details class="chart-table" open>` chứa bảng dữ liệu. Các ô có `data-value` là số thô.
  - Khi trống thì hiện câu thông báo bằng chữ.
  - Không có `style=`, handler `on*`, `th:utext`, script inline hay CDN (`docs/qa/P20/template-scan.txt`).
- **Dữ liệu cho biểu đồ.** `static/js/dashboard.js` đọc dữ liệu **từ chính bảng** (`data-value`), vẽ biểu đồ, và chỉ đóng `<details>` sau khi vẽ thành công. Tắt JS hoặc không tải được Chart.js thì bảng vẫn mở sẵn.
- **Chart.js 4.6.1 lưu cục bộ**, chỉ nạp ở trang Dashboard (`<script defer th:src>`):
  - `static/js/vendor/chart-4.6.1.umd.min.js`: SHA-256 `87364cc2b2a76a6684929e7d6b80b18a897794db38400caff1fcf1791c6925d2`, 210.082 byte, giữ nguyên từ `package/dist/chart.umd.min.js`.
  - `static/js/vendor/chart-4.6.1.LICENSE.md` (MIT): SHA-256 `41a84aa2caba645f966a18d9c2056b73e6d3a81d80bc0046bc0011a2634d4cce`.
  - Tarball npm `chart.js-4.6.1.tgz`: SHA-256 `9345dacdf4bb4c5d40321f4b9aafab6fe676d37a534fc0bc2d17c4c4e29c4f00`. Integrity `sha512-hJyniucsFmq//EvXYvJ6c6mahPoI3kaxdjVH8awTaJ/fDNWeSqqqArixMJWkWNN6fcYvPU3HneSANFNYtRi61g==` khớp registry.npmjs.org. Chi tiết ở `static/js/vendor/chart-4.6.1.provenance.md`.
- **Màu biểu đồ.** Vàng `#9c6b1f` và xanh `#2a78d6`, kết quả validator **ALL CHECKS PASS** ở chế độ sáng, gồm mù màu và tương phản ≥ 3:1 (`docs/qa/P20/palette-validation.txt`). Mỗi biểu đồ chỉ có **một chuỗi dữ liệu**, nên màu không mang nghĩa phân loại. Nhãn trục, tooltip và bảng mang đầy đủ thông tin. Màu thương hiệu `#7A5F33` (không đạt chroma floor cho dữ liệu) chỉ dùng cho nhấn giao diện: nhãn, viền thẻ, kỳ đang chọn.
- **Sai khác có chủ ý so với prompt.** Prompt gợi ý "line + bar" chung một biểu đồ. Hai đại lượng khác thang đo (tiền và số đơn) thì cần hai trục y, và biểu đồ hai trục dễ đọc sai. Tôi tách thành **hai biểu đồ nhỏ xếp chồng, dùng chung trục ngày**: đường doanh thu ở trên, cột số đơn ở dưới. Cả hai dùng chung một bảng dữ liệu.
- **Lọc đơn chờ xử lý.** `AdminDonHang_Controller` nhận thêm tham số tùy chọn `status` (lặp được), lọc trên danh sách sẵn có của `danhSachAdmin()`. Trang hiện dòng "Đang lọc N đơn: …" và link "Xem tất cả đơn".
- **Header.**
  - Thêm `div.header-start` bọc icon và logo.
  - Ở chế độ carousel trang chủ, lưới đổi thành `auto minmax(0,1fr) auto` (icon | menu | giỏ), logo vẫn căn giữa tuyệt đối.
- **Chỉ mục đề xuất** (chưa tạo, vì không đổi schema V1–V5): `lan_thanh_toan(trang_thai, ngay_thanh_toan, ma_don_hang)`, `don_hang(ngay_tao)`, `don_hang(trang_thai)`, `danh_gia(ngay_tao)`, `danh_gia(hien_thi)`, `yeu_cau_doi_tra(trang_thai)`, `phieu_giam_gia(dang_hoat_dong, ket_thuc_luc)`. Với quy mô demo (vài trăm đơn) thì chưa cần. Khi dữ liệu lớn, nên tạo bằng một migration V6 sau khi owner duyệt.

## 5. Phân quyền hai lớp

- **Lớp 1:** `Security_Config` vẫn yêu cầu `/admin/**` có `hasRole('ADMIN')`.
- **Lớp 2:** `@PreAuthorize("hasRole('ADMIN')")`.
  - Gắn ở **cấp lớp** cho các service chỉ Admin dùng: `ThongKe_Service`, `DanhMucSanPhamThuongHieu_Service`.
  - Gắn ở **cấp method** cho các method chỉ Admin dùng trong service dùng chung, để Customer không bị chặn nhầm:
    - `BienTheSanPham_Service`: `sanPham`, `danhSach`, `chiTiet`, `luu`, `ngungHoatDong`. `anhCongKhai` công khai, không gắn.
    - `NguoiDung_Service`: `timNguoiDung`, `datTrangThai`.
    - `PhieuGiamGia_Service`: `danhSachAdmin`, `docFormAdmin`, `luuAdmin`, `xoaAdmin`.
    - `DonHang_Service`: `danhSachAdmin`, `tongQuanAdmin`, `chiTietAdmin`, `chuyenAdmin`. `chiTietCustomer` gọi `chiTietAdmin` nội bộ (self-invocation không qua proxy) nên Customer không bị ảnh hưởng. Test tích hợp MariaDB của luồng Customer xác nhận điều này.
    - `SauMua_Service`: `danhSachDanhGiaAdmin`, `datHienThi`, `chuyenDoiTra`.
    - `TuVan_Service`: `trangThai`, `napLai`.
- `AnhTaiLen_Advice` chỉ gọi service SKU khi đã xác nhận `ROLE_ADMIN`, nên không phát sinh lỗi mới.

## 6. Script seed `scripts/seed-demo-orders.ps1`

Ràng buộc đầu vào và cách đánh dấu:

- Chỉ nhận database khớp `^beautyshop_demo(?:_[a-z0-9]+)?$`. Kiểm tra cả ở `ValidatePattern` lẫn trong thân script.
- Mật khẩu DB lấy từ `DB_PASSWORD`, truyền qua `MYSQL_PWD`, không in ra.
- Tạo 5–8 khách `demo-khach-N@example.test` (mặc định 6) với ngày tạo trải từ 4 đến 150 ngày trước.
- `mat_khau_bam` là chuỗi ngẫu nhiên dạng bcrypt sinh bằng `RandomNumberGenerator`. Giá trị này không in, không ghi file, và không ai biết mật khẩu gốc, nên các tài khoản demo không đăng nhập được.
- Tạo 80–150 đơn (mặc định 120) trong 90 ngày gần nhất. Giá lấy từ `gia_ban` hiện hành, SKU lấy từ catalog demo.
- Không áp voucher. Phí ship theo `ShippingFee_Policy`.
- Đánh dấu demo:
  - mã đơn `DEMO-ORD-NNN`, đồng thời là khóa chống chạy trùng;
  - `ten_nguoi_nhan`, `dia_chi_khi_dat` và nội dung đánh giá bắt đầu bằng "[DEMO]". Bảng `don_hang` không có cột ghi chú, nên không ghi "[DEMO]" vào ghi chú được.
- Script tái hiện đủ bản ghi như luồng ứng dụng, trong **một transaction**:

| Luồng | Bản ghi |
|---|---|
| COD | `don_hang` → `chi_tiet_don_hang` → `ghi_nhan_tru_kho_cod` và trừ `so_luong_trong_kho` → `lan_thanh_toan` `COD-<id>`. Chưa giao thì `UNPAID`. `DELIVERED` thì `PAID` kèm `ngay_thanh_toan` bằng lúc giao. `SHIPPING`/`DELIVERED` có `van_chuyen`, `DELIVERED` có thêm `don_hang.ngay_giao_thanh_cong`. `CANCELLED` (trước PACKING) trả lại kho, bản ghi V2 vẫn giữ. |
| MoMo (`MOMO_MOCK`) | `tam_giu_ton_kho`. Thanh toán `PAID` (có `ma_giao_dich_cong` `DEMO-TX-NNN`) thì giữ kho chuyển `CONSUMED` và trừ tồn. Thanh toán `FAILED` thì giữ kho `RELEASED`, đơn `EXPIRED`, tồn không đổi (giống `ThanhToanMomo_Service.ketThucDon`). Không tạo đơn MoMo bị hủy sau khi đã trả, vì trường hợp đó cần luồng hoàn tiền. |
| Đánh giá | Chỉ cho dòng hàng của đơn `DELIVERED` thuộc chính người mua, tối đa một lần mỗi dòng, sau ngày giao và trước hiện tại. Khoảng 75% hiển thị, còn lại chờ duyệt. |

Về tồn kho: kế hoạch được lập từ tồn khả dụng, mỗi SKU giữ lại tối thiểu 2. CHECK của schema chặn mọi sai lệch, và lỗi bất kỳ làm rollback toàn bộ. Chạy lại khi đã có `DEMO-ORD-%` thì script chỉ in thông báo và dừng.

Kết quả trên `beautyshop_demo` (`docs/qa/P20/seed-demo-orders.log`, `seed-verify.txt`):

- Tạo **120 đơn**: DELIVERED 88, CANCELLED 10, EXPIRED 7, PENDING_CONFIRMATION 7, CONFIRMED 4, PACKING 3, SHIPPING 1.
- **71 đánh giá** (52 hiển thị, 19 chờ duyệt) và 6 khách.
- Lần chạy thứ hai: "already exist (120). Nothing to do". Database `beautyshop` bị từ chối.
- 11 phép kiểm tra chéo đều bằng 0:
  - mỗi đơn có đúng 1 lần thanh toán;
  - tổng tiền và phí ship đúng quy tắc;
  - DELIVERED có PAID và vận chuyển;
  - không có PAID ở đơn hủy hoặc hết hạn;
  - mọi dòng COD có bản ghi V2;
  - mọi dòng MoMo có giữ kho đúng trạng thái;
  - mọi đánh giá đúng quy tắc;
  - **sổ kho khớp**: tồn = tồn ban đầu − tổng dòng các đơn đã trừ kho;
  - không có tồn âm, không có tạm giữ > tồn.

## 7. Test và kết quả

Lệnh đã chạy (JDK Temurin 21.0.13):

| Lệnh | Kết quả | Log |
|---|---|---|
| `mvn -o test` | **208 test, 0 lỗi, 36 skip** (test MariaDB, bật bằng cờ) | `docs/qa/P20/mvn-test.log` |
| `mvn -o test` với `P01_MARIADB_TEST=true` trên DB trống mới `beautyshop_p20_it` (Flyway V1–V5), xóa DB sau khi chạy | **209 test, 0 lỗi, 0 skip** | `docs/qa/P20/mvn-test-mariadb.log` |
| `mvn -o -DskipTests package` | BUILD SUCCESS, `target/beautyshop-0.0.1-SNAPSHOT.jar` | `docs/qa/P20/mvn-package.log` |

Test mới:

- **`AdminDashboardPagesTest`** (MockMvc, gồm `Security_Config` thật và `ThongKe_Service` thật):
  - Guest bị chuyển tới `/login`, CUSTOMER nhận 403 ở `/admin/dashboard` và `/admin`;
  - ADMIN nhận 200, `/admin` chuyển hướng tới `/admin/dashboard`;
  - trang có KPI, mũi tên và chữ, link lọc đơn, link sửa SKU, `<details open>`, script cục bộ, nút logout có CSRF, logo sáng; không có `style=`/`onclick`;
  - 6 kiểu bộ lọc sai đều hiện thông báo và trả 200.
- **`LoginRedirectTest`** (form login thật, CSRF và session):
  - Admin đăng nhập vào `/admin/dashboard`;
  - Admin bị chặn ở `/admin/orders/5` thì quay lại `/admin/orders/5`;
  - URL ngoài `/admin` hoặc khác host bị bỏ qua;
  - Customer giữ luồng cũ, kể cả vẫn gộp giỏ khách;
  - 12 trường hợp URL độc hại hoặc biên.
- **`ThongKeServiceTest`** (H2):
  - doanh thu loại đơn hủy, thanh toán FAILED/REFUNDED và đơn chờ thanh toán;
  - so sánh kỳ trước (+100%, điểm %);
  - mốc giờ VN (17:30 UTC tính vào ngày hôm sau);
  - kỳ trống;
  - 6 lỗi bộ lọc, trong đó 366 ngày vẫn hợp lệ.
- **`AdminServiceMethodSecurityTest`**:
  - CUSTOMER gọi `ThongKe_Service` hoặc `NguoiDung_Service.timNguoiDung/datTrangThai` thì bị `AccessDeniedException`, khách chưa đăng nhập cũng bị từ chối;
  - method dùng chung (`hoSo`) không bị chặn; ADMIN gọi được.
- **`ThongKe_MariaDb_Test`**: chạy thật các truy vấn trên MariaDB (CTE, `TIMESTAMPADD`/`CAST`, `LIMIT` tham số, mốc giờ VN, loại đơn hủy), kiểm tra Customer bị chặn và trang Dashboard render được.

Test cũ được cập nhật theo hành vi mới đã duyệt trong prompt:

- `XacThuc_MariaDb_Test`: Admin đăng nhập giờ vào `/admin/dashboard` thay vì `/`.
- `DonHangCOD_MariaDb_Test`: gọi `tongQuanAdmin()` trong ngữ cảnh Admin tạm thời.

**Lỗi đã phát hiện và sửa nhờ test MariaDB:**

- `ThongKe_Repository` từng có hai constructor không đánh dấu `@Autowired`. Test slice không phát hiện, nhưng ứng dụng thật sẽ không khởi động được. Tôi bỏ constructor phụ và để độ lệch +7 thành hằng số.

## 8. QA trình duyệt

Ứng dụng chạy từ jar trên `http://localhost:8081`, profile `dev`, DB `beautyshop_demo` sau khi seed. Harness là `docs/qa/P20/admin-dashboard.qa.mjs` (Chrome DevTools Protocol, không dùng npm).

**Storefront: 39/39 PASS, 0 exception trên trang** (`docs/qa/P20/qa-storefront.log`, `results-storefront.json`):

- **Guest:** đo ở `/`, `/products`, `/login` với các độ rộng 340, 390, 768, 1099, 1366, 1920:
  - icon là link `/login`, 44×44, nằm trái logo và menu;
  - không chồng lên logo, menu hay giỏ; bấm trúng (`elementFromPoint`);
  - SVG có `aria-hidden`; không có cuộn ngang.
  - Trang chủ chạy ở chế độ carousel ở độ rộng ≥ 768.
- **Bàn phím:** Tab lần 2 (sau skip link) vào icon, có viền focus và nhãn hiện ra. Nhấn Enter mở `/login`.
- **Customer** đăng ký qua `/register` như người dùng thường (mật khẩu ngẫu nhiên, chỉ trong bộ nhớ):
  - sau đăng nhập không bị đưa vào `/admin`;
  - nút logout là form POST có `_csrf` ở 15 tổ hợp trang × độ rộng;
  - Tab rồi Enter thì đăng xuất về `/?logout`, header trở lại icon Guest.
- **Ghi chú:**
  - Tài khoản QA này (`qa-p20-…@example.test`) vẫn còn trong `beautyshop_demo` và được tính là 1 "khách mới".
  - Ở 340 và 390px trên trang chủ, carousel chuyển sang fallback `content-height` (hành vi có từ P18) và `scrollIntoView` tới chương 1. Vì vậy khi vừa tải trang, header nằm trên khung nhìn 108px. Icon vẫn tới được khi cuộn lên hoặc nhấn Tab. Harness ghi `initialScrollY` và đo từ đầu trang. Không phải lỗi do P20 tạo ra.

**Admin: CHƯA kiểm chứng trên trình duyệt.**

Môi trường này đã chặn việc nâng quyền một tài khoản thử nghiệm lên ADMIN (P19). Prompt P20 yêu cầu không vòng qua, nên tôi **không tạo hay nâng quyền tài khoản nào**. Harness có chế độ `admin`:

- Mở một cửa sổ Chrome **thật** ở `/login`.
- Owner tự đăng nhập bằng tài khoản Admin của họ. Script không đọc hay lưu thông tin đăng nhập.
- Khi trang tới `/admin/dashboard` (cũng là bằng chứng điều hướng sau đăng nhập), script tự kiểm tra:
  - 3 kỳ 7/30/90 ngày và **6 biểu đồ khớp bảng** (so mảng dữ liệu Chart.js với `data-value`);
  - thông báo lỗi bộ lọc;
  - không cuộn ngang ở 1366, 1920, 768, 390;
  - tắt JS thì cả 6 bảng hiển thị;
  - menu "Tổng quan" có `aria-current`; icon logout dùng được bằng bàn phím;
  - chụp ảnh Dashboard ở 1366 và 390, từng biểu đồ, header Admin và chế độ không JS;
  - cuối cùng đăng xuất bằng bàn phím.

Phần Admin đã được kiểm ở mức server: MockMvc, test tích hợp MariaDB với Dashboard render thật trên dữ liệu MariaDB, và truy vấn trên `beautyshop_demo` qua seed-verify. Riêng Chart.js vẽ thật, khớp bảng và bố cục Admin vẫn chờ lần chạy trên.

## 9. Ảnh chụp

`docs/ui/screenshots/P20/`:

- `storefront-header-guest-focus.png`
- `storefront-header-customer-logout-focus.png`
- `storefront-home-header-1366.png`
- Sau khi chạy chế độ admin sẽ có thêm: `dashboard-1366.png`, `dashboard-390.png`, `chart-daily/status/top/category/payment/rating.png`, `admin-header-logout-focus.png`, `dashboard-nojs-1366.png`.

## 10. File thay đổi

**Mới:**

- `controller/admin/AdminTongQuan_Controller.java`
- `dto/BoLocThongKe_Form.java`
- `repository/ThongKe_Repository.java`
- `service/ThongKe_Service.java`
- `templates/admin/dashboard.html`
- `static/js/dashboard.js`
- `static/js/vendor/chart-4.6.1.umd.min.js`, `chart-4.6.1.LICENSE.md`, `chart-4.6.1.provenance.md`
- `scripts/seed-demo-orders.ps1`
- Test: `AdminDashboardPagesTest`, `LoginRedirectTest`, `ThongKeServiceTest`, `AdminServiceMethodSecurityTest`, `ThongKe_MariaDb_Test`

**Sửa:**

- `security/LoginSuccess_Handler.java`
- `controller/admin/AdminDonHang_Controller.java`
- `templates/order/list.html`
- `templates/fragments/header.html`
- `static/css/storefront.css`, `static/css/admin.css`
- `@PreAuthorize` trong 7 service: `DanhMucSanPhamThuongHieu`, `BienTheSanPham`, `NguoiDung`, `PhieuGiamGia`, `DonHang`, `SauMua`, `TuVan`
- Test: `XacThuc_MariaDb_Test`, `DonHangCOD_MariaDb_Test`

**Không đụng tới:**

- quy tắc giá, tồn, voucher và trạng thái đơn/thanh toán;
- migration V1–V5;
- `application-dev.properties` (thay đổi của owner);
- file `.docx` chưa theo dõi;
- các thay đổi P19 chưa commit, gồm 14 file `brand-kit/` đang ở trạng thái `D`.

## 11. Điểm chưa xác minh hoặc cần owner quyết

1. QA trình duyệt phần Admin (§8): cần owner đăng nhập.
2. Đề xuất chỉ mục (§4): chờ duyệt trước khi tạo migration V6.
3. Dữ liệu demo nằm trong `beautyshop_demo`: 6 khách demo, 120 đơn, 71 đánh giá, 1 tài khoản Customer QA. Không có script xóa tự động. Nếu cần, có thể xóa theo tiền tố `DEMO-ORD-` và `demo-khach-` (phải trả lại kho tương ứng).
