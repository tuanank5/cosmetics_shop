# 10-RETURN · Kiểm định và nhập lại kho

**IN_PROGRESS · 9/19 DONE.** Owner duyệt RETURN-1 đến RETURN-5 cho fixture tại baseline `e100a1e1edd32a26b36877572aa2eeac06801c41`. Nghiệm thu, CI và triển khai production có gate riêng. [Quyết định](../DECISIONS.md#return--quyết-định-owner-ngày-28112026)

## Tiếp nhận phiên trước

HEAD và tracking ref origin/main local cùng ở baseline. Index gốc trống, có 14 stash. Tại lúc tiếp nhận chỉ thấy dịch vụ mysqld đã có, không thấy JVM Maven/Surefire hoặc MariaDB fixture cũ đang chạy. Tên thư mục có chữ “final” chỉ là tên lượt chạy.

| Checkout cũ trong target | Bằng chứng đọc lại |
|---|---|
| return-build / return-integration | Maven test 380, skip 123, BUILD SUCCESS. XML fixture có 59 ca integration và 12 ca migration. |
| return-final-build / return-final-it | Maven test 385, skip 127 và package có BUILD SUCCESS. XML fixture có 73 ca kể cả migration. |
| return-delivery-build / return-delivery-it | Maven test 387, skip 129 và package có BUILD SUCCESS. XML khớp hai log bàn giao, 63 integration và 12 migration. WebKit nằm trong suite RETURN. |

Các bộ XML trên có 0 lỗi và 0 failure. Hai log bàn giao còn tại [integration](../qa/RETURN/integration.txt) và [migration](../qa/RETURN/migration.txt). Log lưu được của các build trên không chứa nguyên nhân MojoExecutionException của lần trung gian. Chưa thể gán lỗi đó cho một suite cụ thể. Execution policy local là Restricted. Harness được gọi trong tiến trình PowerShell riêng với đường dẫn checkout, cache, playwright-core và evidence đã resolve.

## Nghiệp vụ đã kiểm tra

| Quyết định | Hiện thực và phép phân biệt |
|---|---|
| RETURN-1 | Tổng đủ bán, mở, hỏng, cách ly khác bằng lượng nhận của dòng. Có checklist khi đủ bán và lý do khi cách ly khác. Khi xác nhận, đọc lại trạng thái SKU, sản phẩm, hãng và danh mục dưới khóa. |
| RETURN-2 | Mã kiện duy nhất toàn hệ thống, lưu riêng phần sai lệch. Chỉ lượng khớp dương mới chuyển APPROVED sang RECEIVED. Nhận bổ sung kiểm tổng đã nhận trên hồ sơ khóa. Cửa sổ 7 ngày UTC chỉ áp lúc tạo yêu cầu. |
| RETURN-3 | Admin có phiên hợp lệ, CSRF và phân quyền backend. Lệnh nhận, kiểm, xác nhận lưu riêng người thực hiện và giờ UTC. Customer đọc theo chủ sở hữu, thấy phân loại đã chốt từng đợt. Chứng từ đối chiếu, actor và lý do tự do dùng nội bộ. |
| RETURN-4 | Khóa người thao tác → đơn → hồ sơ → SKU theo ID tăng dần. UUID nhận diện lệnh và kiểm cả actor, payload, phiên bản. Khóa chính dòng nhận của bút toán bảo vệ nguồn hàng vật lý. Hai đợt khác nhau cùng SKU được nhập độc lập. |
| RETURN-5 | Mọi dòng thực nhận phải đã xác nhận. Phần thiếu cần chứng từ xử lý, sai lệch cần đối chiếu, và Admin phải dẫn chứng từ giải quyết nghĩa vụ. DB chặn khi chờ thanh toán/hoàn, giữ kho/voucher hoặc thiếu chứng từ thanh toán phù hợp. Hồ sơ mở tiếp tục tham gia BR21. |

V11 bổ sung sổ thao tác, kiện, dòng nhận, lịch sử kiểm, chứng từ nhập và chứng từ đóng. Phiên bản NULL giữ dấu hồ sơ V10. PENDING cũ có thể bắt đầu theo dõi từ quyết định duyệt mới. APPROVED/RECEIVED cũ thiếu chứng cứ cần đối chiếu thủ công. COMPLETED cũ chỉ đọc. [Migration](../../src/main/resources/db/migration/V11__kiem_dinh_hang_tra.sql)

## Phép đối chiếu SQL

- Với COD và MoMo mock, fixture chụp mọi cột của đơn, dòng đơn, attempt, hai lượt voucher và hai phiếu trước khi tạo yêu cầu. Sau xác nhận và đóng, so lại toàn bộ hàng, bao gồm địa chỉ, giá và tổng thanh toán 505.000 đồng. Hai lượt HÀNG và VẬN_CHUYỂN đã CONSUMED, mỗi phiếu đã dùng 1/5 lượt. Chạy COD có chứng từ xuất V2 và MoMo mock có reservation CONSUMED.
- Cùng SKU còn 2 đơn vị giữ cho đơn khác. So snapshot của cả đơn đó, reservation và lượng tạm giữ. Mỗi ca nhập có đúng 1 đơn vị trên bút toán nguồn, tồn 10 → 11. Ca hai kiện hợp lệ trên cùng SKU có tồn 10 → 12. Ca REFUND_PENDING giữ RECEIVED và BR21 vẫn đếm đơn.
- Kiểm rollback sau INSERT bút toán bằng giới hạn INT, sau tăng kho bằng lỗi ghi SUCCESS, và sau khi đọc thấy SUCCESS bên trong transaction bằng rollback trước commit. Đối chiếu tồn, số bút toán và chuỗi FAILED/SUCCESS đã commit sau đó. Hai Admin tranh xác nhận cùng kiện chỉ có một người thành công.
- V10 có đủ năm trạng thái cũ và năm dòng trả. Sau hai lần migrate, kiểm phiên bản 11, đủ lịch sử, lượng yêu cầu 5, tồn khởi điểm 8 và số bút toán nhập bằng 0. [Test migration](../../src/test/java/com/beautyshop/bootstrap/LocalDatabaseMigration_MariaDb_Test.java) · [Test MariaDB](../../src/test/java/com/beautyshop/integration/KiemDinhDoiTra_MariaDb_Test.java)

## Finding xử lý trong lượt tiếp tục

- Đưa đăng ký FAILED lên trước kiểm phiên bản cho UUID mới, sau bước nhận diện replay. Race hai Admin có một SUCCESS và một FAILED cho lệnh thua do form cũ.
- Bổ sung kiểm chứng rollback sau SUCCESS thật, tràn kho sau INSERT, hai đợt đủ bán cùng SKU, hai hồ sơ dùng cùng mã kiện, kiện đến sau bảy ngày, lượng nhận vượt duyệt và đối chiếu kho cũ/tạm giữ.
- Bổ sung kết quả theo đợt trên trang Customer, chỉ đọc kết luận đã xác nhận theo đúng chủ. WebKit mở form gửi yêu cầu từ điện thoại, Admin ghi kiện mở và kiện đủ bán, rồi giải quyết phần thiếu bằng chứng từ.

## Giới hạn vận hành

Độ đúng của mã kiện, kiểm định mỹ phẩm và chứng từ giải quyết tiền ngoài hệ thống phụ thuộc nhân viên đối chiếu hàng thật. Không có tích hợp vận đơn hay nguồn xác nhận chi tiền trong RETURN. REFUND, merchant MoMo thật, SMTP thật, tải production, khôi phục DB production và manual UAT nằm ngoài lượt này. Dừng thao tác nhập khi điều tra chênh lệch, giữ lịch sử để đối chiếu theo dòng nhận và SKU.

## Kết quả local trên source bàn giao

JDK Temurin 21.0.13, Maven 3.9.11 offline, MariaDB 12.2.2, Playwright Core 1.63.0 và WebKit build 2359. Hai thư mục build/IT chứa cùng 358 tệp đầu vào. SHA-256 tổng hợp là `9d20ce70674ee9acdef67fadc39046e590844e4e25a60e841d15d2e05a278d3e`. Fixture dùng datadir GUID mới, bind loopback và kiểm tra lại đường dẫn qua SQL trước khi tạo schema. [Manifest source](../qa/RETURN/source.json)

| Gate | Kết quả |
|---|---|
| Maven test | 258 PASS, 129 SKIP, 0 FAIL. [Test](../qa/RETURN/verified/test.txt) |
| MariaDB | 63/63 PASS gồm RETURN, BR21/catalog, COD, voucher, MoMo mock và AUDIT. [Integration](../qa/RETURN/verified/integration.txt) |
| Flyway và JPA | Fresh V1→V11 và JPA validate ở profile dev trên schema return_fixture. Bootstrap/upgrade 12/12 PASS gồm V10 có dữ liệu legacy, chạy migrate lần hai, checksum lệch và DB ngoài quản lý. [Migration](../qa/RETURN/verified/migration.txt) |
| Browser | WebKit desktop Admin 1280×800 bật JS, Customer 390×844 tắt JS. Ca này đã nằm trong 63 test MariaDB. [Kịch bản](../qa/RETURN/browser.cjs) |
| Maven package | PASS, tạo Spring Boot JAR với `-DskipTests`. [Package](../qa/RETURN/verified/package.txt) |
| Đếm kết quả | 333 lượt thực thi PASS riêng biệt. Trong 129 phương thức bị skip mặc định, 43 được chạy trên fixture (75 lượt sau khi bung tham số), 86 vẫn NOT RUN. [Kết quả và danh sách NOT RUN](../qa/RETURN/verification.json) |

Ở lượt trung gian, test/package đạt nhưng WebKit timeout khi tìm ô số lượng ngay sau khi đăng nhập. Sau khi đợi điều hướng đăng nhập hoàn tất, ca browser chạy riêng đạt. Lượt đầy đủ phía trên chạy lại cả test/package, browser, migration và integration trên đúng source bàn giao. [Lượt lỗi](../qa/RETURN/local/integration.txt) · [Khoanh vùng](../qa/RETURN/probe/integration.txt) · [Chạy riêng sau sửa](../qa/RETURN/probe2/integration.txt)

## Bàn giao RETURN-FINAL

Checkout riêng nằm tại `target/rr-stage` của working tree BeautyShop. Baseline là HEAD đã xác minh ở đầu báo cáo. Manifest liệt kê từng file thay đổi và SHA-256 của byte trong index, trừ chính manifest. Để giữ Git text nhất quán, hai checkout chạy gate dùng LF. Hash raw của working tree trước chuẩn hóa cũng nằm trong source.json. Index ở working tree gốc trống. [Danh sách file và hash](../qa/RETURN/manifest.sha256)

Từ thư mục gốc BeautyShop, owner có thể kiểm index và lấy staged tree ID bằng PowerShell.

```powershell
Push-Location -LiteralPath 'target/rr-stage'
try {
    git status -sb
    git diff --cached --name-status
    git diff --cached --check
    python docs/qa/RETURN/check-staged.py
    if ($LASTEXITCODE) { throw 'RETURN staged verification failed' }
} finally { Pop-Location }
```

Để tái lập gate từ checkout trên, chọn JDK/cache/WebKit local đã dùng và một thư mục evidence mới bên trong repository. Harness tự tạo datadir và cổng mới mỗi lần.

```powershell
$root = (Resolve-Path '.').Path
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
$repo = (Resolve-Path 'target/pay-reval-cache').Path
$qa = (Resolve-Path 'target/rr-stage').Path
$core = (Resolve-Path '../Ros-aBeauty/CosmeticsShop/node_modules/playwright-core').Path
$logs = (New-Item -ItemType Directory -Path (Join-Path $root ('docs/qa/RETURN/retest-' + [guid]::NewGuid().ToString('N')))).FullName
Push-Location -LiteralPath $qa
try {
    & mvn.cmd -o -B -ntp "-Dmaven.repo.local=$repo" test
    if ($LASTEXITCODE) { throw 'Maven test failed' }
    & mvn.cmd -o -B -ntp "-Dmaven.repo.local=$repo" -DskipTests package
    if ($LASTEXITCODE) { throw 'Package failed' }
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/RETURN/run-fixture.ps1 -Checkout $qa -MavenRepo $repo -PlaywrightCore $core -EvidenceDirectory $logs
    if ($LASTEXITCODE) { throw 'MariaDB or migration gate failed' }
} finally { Pop-Location }
```

## Closeout sau merge PR #20 · 28/11/2026

**PASS · MERGED / READY_FOR_OWNER_ACCEPTANCE** trong phạm vi fixture đã duyệt. RETURN giữ **IN_PROGRESS**, tiến độ chính thức **9/19 DONE** đến quyết định nghiệm thu của owner. Ba run CI dưới đây cập nhật mục “remote CI chưa chạy” trong bằng chứng lịch sử RETURN-FINAL. [PR #20](https://github.com/tuanank5/cosmetics_shop/pull/20) · [Dữ liệu đối chiếu closeout](../qa/RETURN/closeout/verification.json)

| Đối tượng Git | SHA |
|---|---|
| Baseline, parent của candidate và parent thứ nhất của merge | `e100a1e1edd32a26b36877572aa2eeac06801c41` |
| Candidate, parent thứ hai của merge | `0de31f7fa470404a510c6540915ac3ccc5b1c91d` |
| Merge, HEAD và origin/main khi kiểm | `16060628d4ed599ab6b4214b9a5a463f0c60352e` |
| Tree chung của candidate và merge | `6ac6f4166bdb4926524417b039b1e3b8954e7cd0` |

Đối chiếu toàn tree và 39 đường dẫn thay đổi so với baseline bằng `git ls-tree`, `git diff` và `git cat-file --batch`. Cả 39 blob giống nhau ở candidate và merge. Manifest kê 38 tệp, còn chính manifest được băm độc lập. Đã khớp từng byte của 358 đầu vào ở **cả hai** checkout đã chạy test với blob đã merge. SHA-256 tổng hợp là `9d20ce70674ee9acdef67fadc39046e590844e4e25a60e841d15d2e05a278d3e`. Trong working tree Windows, 294 tệp khác byte do CRLF, 64 tệp giống nguyên byte. V1–V10 trùng blob baseline, V11 khớp SHA-256 `c8c1db70a845d78a94d385f1fc14b2ae873a7c19049297d425efc9142ca44a1f`. Chi tiết SHA-256 từng migration và từng file thay đổi nằm trong [verification.json](../qa/RETURN/closeout/verification.json).

| GitHub Actions | SHA thực tế checkout | Kết quả |
|---|---|---|
| [Push candidate · 46163547694](https://github.com/tuanank5/cosmetics_shop/actions/runs/46163547694) | `0de31f7` | completed/success |
| [Pull request · 46163569792](https://github.com/tuanank5/cosmetics_shop/actions/runs/46163569792) | `6b772dfed59bd129466c71ca2ee55b16b7072048` từ pull/20/merge. Head của run là `0de31f7`, tree merge thử là `6ac6f41`. | completed/success |
| [Push main · 46163687590](https://github.com/tuanank5/cosmetics_shop/actions/runs/46163687590) | `1606062` | completed/success |

GitHub chạy Temurin 21, `mvn -B test` và `mvn -B -DskipTests package`. MariaDB, WebKit, fresh Flyway/JPA validate và upgrade V10 được kế thừa từ fixture local trên đúng source. Closeout đọc lại 67 XML mặc định và 7 XML fixture cùng các log BUILD SUCCESS. Theo khóa lớp và tên ca thực thi, 258 PASS mặc định và 75 PASS fixture có giao rỗng. Trong 75 lượt có 63 integration (gồm WebKit) và 12 migration. Khi gộp các lần chạy có tham số theo phương thức, 43 trong 129 phương thức skip mặc định đã chạy trên fixture. Kết quả cuối là **333 PASS, 0 FAIL và 86 phương thức vẫn NOT RUN**. [Log đã kiểm chứng](../qa/RETURN/verified/) · [Danh sách và đối chiếu khoảng trống](../qa/RETURN/closeout/verification.json)

### Những ca NOT RUN có giao với RETURN

| Điểm cần phân biệt | Assertion đã chạy | Giới hạn bằng chứng |
|---|---|---|
| Hai phương thức đổi trả trong SauMua_MongoDanhGia_Test | KiemDinhDoiTra_MariaDb_Test kế thừa đúng hai thân test từ DoiTraFixture. Một ca kiểm nhiều dòng, lượng đủ bán và quyền chủ sở hữu. Ca kia tranh lượng cuối, đúng một request thành công và còn đúng một hồ sơ. | Hai định danh thuộc lớp Mongo và phần khởi tạo Mongo vẫn NOT RUN. |
| SKU, tranh tồn, retry và rollback tại checkout | Hai Admin tranh một kiện hai dòng cho đúng một xác nhận, hai bút toán theo hai dòng, tồn 11/11. Hai kiện hợp lệ cùng SKU cho tồn 10→12 dù POST xác nhận lại. Ca giới hạn INT, lỗi lúc ghi audit và rollback sau khi đã đọc thấy SUCCESS đều đối chiếu được tồn và bút toán. Suite COD còn chạy ca hai khách tranh SKU. | Các ca riêng của BienTheSanPham về đọc lại sau khi khóa, hai checkout COD/MoMo, reserve/consume/release và retry sau rollback vẫn NOT RUN. Lịch RETURN tranh cùng SKU với checkout chưa được mô phỏng trực tiếp. |
| Điều kiện bán, kho cũ, số liệu tài chính | SKU ngừng bán làm rollback toàn kiện. Trên cả COD và MoMo mock, so từng hàng đơn/dòng/attempt/voucher/chứng từ cũ trước và sau, giữ 2 đơn vị tạm giữ. Mỗi chứng từ nhập đủ bán tăng 1. REFUND_PENDING chặn đóng, trạng thái RECEIVED và BR21 tiếp tục đếm đơn. | CRUD ảnh/SKU, giao diện tồn storefront và KPI dashboard có phương thức riêng chưa chạy. |
| Login, owner, ROLE_ADMIN và CSRF | WebKit đăng nhập form thật bằng hai vai trò. MockMvc kiểm chủ đọc được, khác chủ đọc/gửi trả 404, Customer gửi POST duyệt dù có CSRF nhận 403, Admin thiếu CSRF cũng nhận 403. Service ghi kiểm lại ADMIN còn hiệu lực và phiên bản xác thực dưới khóa. | Chưa thử thu hồi quyền đúng giữa giao dịch RETURN. Các ca logout, OAuth/khóa mở tài khoản, bộ trang lỗi HTTP và ma trận quyền của các WP khác giữ NOT RUN. |
| Nhận 0, hàng mở, thời hạn và legacy | Kiện sai khớp 0 giữ APPROVED và tồn 10. Đối chiếu có 1 hàng khớp mới sang RECEIVED. Test nhiều đợt nhận kiện sau hạn gửi yêu cầu, chặn nhận vượt. WebKit kiểm hàng mở bị cách ly. Migration V10 đủ năm trạng thái, sau migrate hai lần vẫn còn 5 đơn vị đã yêu cầu, tồn 8 và 0 bút toán nhập. | Xác minh mã kiện là cùng một kiện vật lý, niêm phong, chất lượng mỹ phẩm và chứng từ giải quyết tiền thuộc quy trình đối chiếu thực tế. |

Các finding về audit form cũ, rollback sau SUCCESS, đối chiếu hai kiện/chứng từ cũ và chờ điều hướng đăng nhập WebKit đã đóng trên source merge. Manual UAT, tải và dữ liệu production, SMTP và provider thanh toán thật vẫn NOT RUN. Chi tiền thật và đối soát khoản hoàn thuộc REFUND. Không có blocker kỹ thuật trong phạm vi trình nghiệm thu fixture. [Assertion MariaDB](../../src/test/java/com/beautyshop/integration/KiemDinhDoiTra_MariaDb_Test.java) · [Assertion dùng chung](../../src/test/java/com/beautyshop/integration/DoiTraFixture.java)
