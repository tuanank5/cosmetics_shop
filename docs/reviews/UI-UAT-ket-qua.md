# 19-UI-UAT · Ma trận giao diện sau PR #22

**IN_PROGRESS · 9/19 WP DONE.** Ngày 29/11/2026, kiểm trên `fb70c8b168dc6609b8d766d50421c4399a82df88`, tree `f20e144f8e54e1d6343df2812e10ca6aafae2cc2`, checkout riêng `qa/ui-uat-19`. Phạm vi tự động đạt các phép kiểm trong bảng. **Số ca được owner ký UAT thủ công là 0.**

## Review FINAL

**AUTOMATED_QA_PASS / MANUAL_UAT_PENDING.** Đủ điều kiện mở PR bằng chứng tự động. Review FINAL đọc lại toàn bộ JSONL, CSV, log giao dịch, kết quả package, chín XML Surefire và hai ảnh. Bằng chứng hiện có đủ để xác minh kết quả trên baseline. Đã đối chiếu byte của 362 blob thuộc `src/` và `pom.xml` với Git, cùng 450 mục trong `BOOT-INF/classes/` của JAR với thư mục build.

WebKit có 354 dòng từ fixture chính, 108 từ fixture dispatch lỗi và 8 từ MoMo mock. Chrome có 59 + 18 + 2 dòng từ ba log, gồm một lượt Guest Chat lặp. Surefire có **69 lượt PASS thuộc 66 phương thức Java**. Ba phương thức có hai bộ tham số. Lượt chạy lại phương thức HTTP để cấp fixture cho trình duyệt đã được loại khỏi tổng.

| Phép kiểm trên 470 dòng áp dụng | AUTOMATED_PASS | AUTOMATED_NOT_RUN | MANUAL_NOT_RUN |
|---|---:|---:|---:|
| WebKit render, reflow và focus sau skip link (mỗi phép) | 470 | 0 | — |
| Chrome desktop, JS bật, zoom native 200% | 78 | 392 | — |
| Tab/Shift+Tab của form login | 1 | 469 | — |
| Back/Forward của giỏ | 1 | 469 | — |
| Wishlist rỗng | 6 | 464 | — |
| Tên sản phẩm dài | 18 | 452 | — |
| Hiển thị trang lỗi | 132 | 338 | — |
| UAT trên thiết bị do owner ký | — | — | 470 |

Mỗi dòng ma trận có thể còn thiếu nhiều phép kiểm. Các cột dùng cùng tập tổ hợp nên không cộng số chưa chạy giữa các dòng bảng trên. Có thêm 468 dòng `SKIP_NOT_APPLICABLE` theo phạm vi vai trò. Số assertion báo `FAIL` trong các lượt cuối là 0. Các gate `BLOCKED` được ghi ở phần dành cho owner.

Ảnh Chrome Guest Chat là bitmap 1654×845 px. Runner yêu cầu cửa sổ 1672×941, đo được chiều rộng ngoài 1672 và vùng CSS 1654 ở 100%, 827 ở 200%. Ảnh WebKit là 390×844 px. Đã xem nội dung và kiểm hash cả hai ảnh. Log JSONL giữ kết quả gốc; ma trận FINAL dùng nhãn riêng cho tự động, thủ công và ngoài phạm vi. [Kết quả FINAL](../qa/UI-UAT/verification.json) · [Danh sách file và hash SHA-256](../qa/UI-UAT/manifest.sha256)

## Màn thực sự có trong bản tích hợp

Có **44 màn nghiệp vụ và 8 giao diện lỗi**. Đơn vị đếm là họ route HTML theo nghiệp vụ/vai trò. Ví dụ Admin và Customer có hai màn danh sách trả hàng dù dùng chung template. Form tạo/sửa chung template, tham số ID, lượt lọc/pager và nhiều trạng thái dữ liệu cùng thuộc một màn. Danh mục, thương hiệu và sản phẩm có các section riêng trong Admin. Các fragment, alias redirect, API JSON, tệp ảnh và POST hành động nằm ngoài số màn. Không có màn inbox, AI-HISTORY hay quản trị backup ở SHA này. Danh mục ghi route, template Thymeleaf, vai trò và số lượt quan sát. [52 màn](../qa/UI-UAT/screens.csv) · [Ma trận theo tổ hợp](../qa/UI-UAT/coverage.csv)

| Nhóm | Phạm vi đã chạy |
|---|---|
| Guest | Trang chủ, catalog/PDP với tên dài, giỏ, Chat, login/đăng ký/quên/đặt lại mật khẩu và trang quay về từ MoMo. Session mới ở PDP bị 403 khi thiếu CSRF. Form hợp lệ đưa đúng một SKU vào giỏ. |
| Customer | Hồ sơ, bảo mật, địa chỉ, wishlist rỗng, checkout, đơn/dòng đánh giá, yêu cầu và chi tiết trả hàng, trang MoMo mô phỏng. Kiểm truy cập chéo, hai tab và mua lại. WebKit thực hiện đăng nhập, logout và thử lại phiên cũ với IdP fixture. |
| Admin | Tổng quan, catalog/SKU, người dùng, mã giảm, đơn, đánh giá, RETURN, nhật ký và AI. Audit giữ bộ lọc, sort, trang 2 và tab độc lập. Với JS bật, Back trả focus đúng dòng; khi dòng biến mất, focus chuyển về đầu danh sách. |
| Lỗi | 400, 403, 404, 409, 413, 429, 500 và fallback 503. Có 403 qua Customer vào Admin và 404 qua slug thiếu. Sáu kiểu còn lại dùng `HttpLoi_MariaDb_Test.TraceConfig` để buộc Tomcat dispatch vào template thật. Các đường kích hoạt chỉ có trong fixture. |

## Kết quả theo tổ hợp

| Gate | Thực chạy trên SHA tích hợp |
|---|---|
| WebKit 26.6 | **470 tổ hợp PASS**. Desktop 1672×941, tablet 768×1024, mobile 390×844, JS bật và tắt. Thêm hai lượt MoMo mock tại 320×640. Kiểm HTTP/route, vùng main, tràn ngang toàn trang và focus vào nội dung chính sau khi kích hoạt skip link. Các fixture dài, rỗng và lỗi có assertion riêng. |
| Chrome 158.0.8127.37 · zoom native | **78 tổ hợp PASS** với JS bật, desktop, ba vai trò. Đổi zoom trang qua cài đặt Chrome trên profile dùng riêng. Cửa sổ rộng 1672, vùng CSS từ 1654 xuống 827, DPR từ 1 lên 2, visual viewport scale giữ 1. Kiểm main và độ tràn trên từng màn. Phép đo này chưa xác nhận độ dễ đọc, focus bị che hoặc khả năng dùng hết mọi nút. |
| Hành trình tương tác | 7 kịch bản WebKit tại 768×1024. Tab/Shift+Tab đi qua lại giữa email và mật khẩu, focus-visible được kích hoạt. Back/Forward giỏ và reload đọc số lượng từ server. Login gộp từ 7 lên 8, reload vẫn là 8. Form địa chỉ sai có lỗi trường. Từ 9 địa chỉ gửi đồng thời hai POST hợp lệ chỉ lưu thêm một, còn đúng một mặc định. Sửa rồi xóa địa chỉ nguồn vẫn đọc được snapshot ban đầu trên đơn và qua SQL. [Log hành trình](../qa/UI-UAT/journeys.txt) · [Log SQL cuối](../qa/UI-UAT/matrix-run.txt) |
| MariaDB và hồi quy theo WP | **69 ca Surefire duy nhất PASS**, 0 lỗi, 0 skip trong các suite chọn. 59 ca cho AUTH/ACCOUNT, ORDER, AUDIT, RETURN và AI-OPS; 10 ca bổ sung cho HTTP lỗi và MoMo mock. Lượt dùng lại phương thức HTTP để mở trình duyệt đã loại khỏi 69. [Log 59 ca](../qa/UI-UAT/integration.txt) · [Log 10 ca](../qa/UI-UAT/http/integration.txt) |
| Các runner WebKit riêng | AUDIT 3 nhóm, VOUCHER 4, ORDER 2, PAYMENT 8. Có tổng tiền COD/attempt và hai slot voucher, lịch sử attempt khác nhau theo quyền, Customer khác chủ 404, Admin/CSRF 403, pager và focus. RETURN tạo yêu cầu, Admin nhận/kiểm/xác nhận nhiều đợt, Customer xem kết quả theo chủ. AI-OPS kiểm FAQ khi tắt AI và Admin reindex với provider mock. [AUDIT](../qa/UI-UAT/AUDIT.txt) · [VOUCHER và ORDER](../qa/UI-UAT/ORDER.txt) · [PAYMENT](../qa/UI-UAT/PAYMENT.txt) |
| Đóng gói | JDK 21.0.13, Maven 3.9.11 offline, package đạt. JAR SHA-256 `e96fe42fc435dc7d7cf99d7aae5985f9b7910071de05c95fb2329ce36cb7990f`. MariaDB 12.2.2 cục bộ, datadir mới theo lượt. Tất cả dữ liệu nghiệp vụ và tài khoản dùng cho QA là fixture. [Package](../qa/UI-UAT/package.txt) |

**548 tổ hợp trình duyệt duy nhất** là 470 WebKit + 78 Chrome native. Guest Chat ở zoom 200% chạy hai lần và chỉ tính một. Các nhóm hành trình và phương thức Java dùng đơn vị đếm riêng. Trong 938 dòng ma trận có 468 tổ hợp WebKit **SKIP** vì ngoài vai trò nghiệp vụ đã chọn, ví dụ Customer mở màn quản trị như người điều hành. Các kiểm tra từ chối quyền có bằng chứng riêng. Các ô `AUTOMATED_NOT_RUN` và `MANUAL_NOT_RUN` ghi phần chưa kiểm tại chính tổ hợp đó. `BLOCKED` dùng cho các gate cần đầu vào bên ngoài. [Kết quả máy đọc được và 69 tên ca](../qa/UI-UAT/verification.json)

## Finding và nguồn bằng chứng

Trong 470 tổ hợp WebKit, 470 kiểm được focus khi kích hoạt skip link, 18 kiểm nội dung dài trên PDP, 6 kiểm wishlist rỗng và 132 kiểm màn lỗi. Tab/Shift+Tab của form login và Back/Forward của giỏ có một tổ hợp tablet cho mỗi phép kiểm. Những hành trình Audit/ORDER dùng viewport riêng và có log riêng. Owner có thể lọc các cột tương ứng trong CSV để chọn chính xác phần cần kiểm tiếp.

**0 lỗi sản phẩm tái hiện trong các assertion đã chạy.** Lượt rà ban đầu seed hồ sơ RETURN với giá trị `REQUESTED`. Mở danh sách hoặc chi tiết khi đọc lại hồ sơ trả 500 do enum không tồn tại. Đầu vào hợp lệ là `PENDING`. Rerun với schema mới trả 200 ở các màn này. Runner chụp ảnh ban đầu truyền sai kiểu tham số mask; đã sửa và kiểm ảnh sau rerun. Hai finding thuộc fixture. Không có bản sửa ứng dụng.

Tree candidate AI-OPS của PR #22 trùng tree baseline. Cả 364 hash đầu vào trong hồ sơ AI-OPS khớp byte. Bằng chứng WebKit AI-OPS trên tree này đã được đối chiếu và chạy lại. Với AUTH, ACCOUNT, ORDER, RETURN, VOUCHER, PAYMENT, AUDIT, báo cáo cũ dùng để chọn kịch bản. Các kết quả nêu trên đến từ các lượt mới. SHA tham chiếu và log từng WP ở mục `priorEvidence`. [Đối chiếu revision](../qa/UI-UAT/verification.json)

[Ảnh WebKit Guest 390×844, JS tắt](../ui/screenshots/UI-UAT/guest-mobile-chat.png) · [Ảnh Chrome Guest, zoom native 200%](../ui/screenshots/UI-UAT/native-200-chat.png). Ảnh chỉ chứa màn Chat công khai trên fixture. Các trường nhập đã che.

## Gate cần owner thực hiện

1. Trên thiết bị desktop, tablet và điện thoại thật, dùng bàn phím, touch và screen reader để đi trọn luồng mua, trả hàng và quản trị. Kiểm thông báo lỗi được đọc, focus nhìn thấy sau đóng dialog, ở sticky header, pager và sau khi mất dòng. Ghi thiết bị, phiên bản, SHA và kết quả từng luồng.
2. Đặt zoom 200% bằng giao diện trình duyệt thật. Đọc hết nhãn/nội dung dài, thao tác đến nút cuối với JS bật/tắt, thử cỡ chữ hệ điều hành. Đối chiếu nhất là bảng Admin, chọn nhiều dòng RETURN và màn checkout có lỗi. Kiểm Back/Forward sau sửa ở tab khác và phục hồi từ BFCache theo trình duyệt. Các ô bàn phím, điều hướng và trạng thái dữ liệu chưa chạy nằm trong ma trận.
3. **BLOCKED** đối với xác minh pin Maps, liên hệ và đối chiếu báo cáo nộp cho tới khi owner cung cấp pin/kênh liên hệ và đúng bản hồ sơ. OAuth, email, merchant thật và kiểm định kiện hàng vật lý cần môi trường/người phụ trách tương ứng. Manual UAT, nghiệm thu provider và phát hành production vẫn mở.

## Tái lập

Từ checkout riêng ở SHA đã ghi, đặt `JAVA_HOME` về JDK 21, `BASE_PLAYWRIGHT` về playwright-core có WebKit, và `MAVEN_REPO` về Maven cache cục bộ. Dùng profile dùng một lần cho Chrome; browser runner chỉ nhắm ứng dụng trên loopback. Các runner tạo datadir riêng. Để lưu lượt khác, dùng checkout QA mới.

```powershell
mvn.cmd -o "-Dmaven.repo.local=$env:MAVEN_REPO" -DskipTests package
if ($LASTEXITCODE) { throw 'Package failed' }
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-UAT/run-matrix.ps1
if ($LASTEXITCODE) { throw 'Matrix failed' }
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-UAT/run-payment-screen.ps1
if ($LASTEXITCODE) { throw 'Mock payment screen failed' }
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-UAT/run-errors.ps1 -MavenRepo $env:MAVEN_REPO -PlaywrightCore $env:BASE_PLAYWRIGHT
if ($LASTEXITCODE) { throw 'Error dispatch fixture failed' }
```
