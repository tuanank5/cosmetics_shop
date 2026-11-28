# 09-ORDER · Danh sách đơn Customer và mua lại

**MERGED / READY_FOR_OWNER_ACCEPTANCE** tại `13c586a`. ORDER bắt đầu ngày 28/11/2026 trên baseline `3da645dd76f22b8f83351d63ca419bd5646af6e8`. Owner giao phần ORDER sau khi VOUCHER và PAYMENT đã tích hợp. Tiến độ 5/19 DONE. VOUCHER chờ nghiệm thu, PAYMENT chờ nghiệm thu phạm vi COD/MoMo mock. AUTH-LIVE, PAY-LIVE và AUDIT PURGE tiếp tục BLOCKED_EXTERNAL.

## Danh sách

Đối chiếu bảng hiện trạng, tách bộ đếm và thông báo rỗng thành hai phép kiểm.

| Tiêu chí | Trước ORDER | Candidate |
|---|---|---|
| Tìm mã | Customer chưa có | Tiền tố mã hiển thị, escape dấu gạch dưới, bind tham số. |
| Trạng thái | Chưa có | Chọn nhiều trạng thái đơn. |
| Ngày | Chưa có | Ngày ISO hợp lệ, kiểm hai đầu khoảng. Từ 00:00 ngày đầu đến trước 00:00 ngày kế ngày cuối theo giờ Việt Nam, truy vấn UTC. |
| Sort | Repository sắp mặc định | Ngày mới/cũ, tổng tăng/giảm. Mỗi biểu thức có ID theo sau. |
| Pager | 10 đơn/trang | Giữ cỡ 10 và quay về trang cuối nếu trang yêu cầu vượt kết quả. |
| Bộ đếm | Đếm đơn theo chủ | COUNT và SELECT dùng cùng predicate chủ và bộ lọc, cùng transaction REPEATABLE_READ. Query danh sách chỉ đọc tóm tắt đơn. |
| Rỗng | “Chưa có đơn hàng.” | Giữ nguyên khi chưa lọc, báo chưa có đơn phù hợp khi đã lọc. |
| Đường về | Trang và focus dòng | Giữ cả mã, trạng thái, khoảng ngày, sort, trang, anchor và cơ chế focus hiện có. |

Admin dùng tiếp bộ lọc và template chung. Customer không nhận ID người dùng từ request để quyết định danh sách hoặc giỏ. [Hiện trạng](BEAUTYSHOP_CURRENT_STATE_REVIEW.md#đối-chiếu-danh-sách-và-chi-tiết-đơn)

## Mua lại

GET chi tiết tạo UUID của thao tác. POST có CSRF tới `/orders/{id}/reorder`, kiểm quyền Customer và quyền sở hữu đơn trước khi ghi. Service nhận ID chủ từ phép kiểm quyền. Trong một transaction READ_COMMITTED, service ghi biên nhận, khóa giỏ, xử lý dòng theo ID SKU tăng dần và dùng khóa SKU cùng khóa đọc các hàng sản phẩm, hãng, danh mục như checkout. PRG về giỏ với thông báo theo dòng được escape bằng Thymeleaf.

Dùng unique key đã có trên bảng biên nhận V3. Khóa mua lại là UUID phiên bản 3 được tạo từ namespace reorder, chủ đơn, đơn và UUID thao tác. Khóa merge giỏ Guest là UUID phiên bản 4. Insert trùng giúp retry bỏ qua toàn bộ thao tác. Lỗi transaction rollback cả biên nhận và ghi giỏ. Mở lại chi tiết tạo thao tác mới có thể chủ động thực hiện tiếp. Không thêm migration.

| Dòng trong đơn cũ | Ghi vào giỏ |
|---|---|
| SKU, cha, hãng, danh mục đang bán, đủ tồn sau khi trừ giữ và lượng trong giỏ | Cộng đủ lượng cũ. |
| Lượng còn mua được nhỏ hơn lượng cũ | Cộng đúng phần còn mua được và báo số thêm/số yêu cầu. |
| Hết tồn khả dụng hoặc giỏ đã đủ | Bỏ qua, giữ cả lượng đang nằm trong giỏ. |
| Thiếu SKU hoặc SKU/cha/hãng/danh mục ngừng bán | Bỏ qua, dùng tên và SKU snapshot để chỉ rõ dòng. |
| Giá thay đổi | Giỏ và quote đọc lại giá SKU hiện hành. |

Ghi giỏ không giữ kho, tạo đơn, tạo attempt hay sao chép voucher. Chỉ POST đặt hàng riêng mới chốt giao dịch. [SRS](../requirements/01-srs.md) · [BR02](../requirements/02-business-rules.md)

## Phạm vi và tái lập

[Manifest source và test](../qa/ORDER/source.json) liệt kê đường dẫn và SHA-256 từng file. Ngoài source, cập nhật SRS, BR, sổ tiến độ, biên bản này và [bằng chứng ORDER](../qa/ORDER/). Git root giữ tám stash và tài liệu local của các WP khác.

Hai bản sao source vào `target/order-build` và `target/order-it`, JDK 21, Maven offline với cache đã có. Từ checkout build chạy `mvn test` và `mvn -DskipTests package`. Từ checkout integration dùng runner `docs/qa/VOUCHER/run-mariadb.ps1` với Checkout, MavenRepo và EvidenceDirectory của ORDER. Runner tạo datadir MariaDB và MongoDB loopback mới, bật P01_MARIADB_TEST, AUTH_02_MIGRATION_FIXTURE và P23_MONGO_TEST. Từ bản chứa JAR chạy `docs/qa/ORDER/run-browser.ps1` và `docs/qa/PAYMENT/run-order-detail.ps1` với JAVA_HOME JDK 21 và BASE_PLAYWRIGHT trỏ vào runtime WebKit. Các fixture có dữ liệu tổng hợp, không dùng DB làm việc.

## Kết quả trên working tree

| Kiểm tra | Kết quả |
|---|---|
| Maven test JDK 21 | 251 PASS, 105 SKIP theo điều kiện môi trường, 0 FAIL/ERROR. 7 invocation unit mới PASS. [Log](../qa/ORDER/maven.txt) |
| Integration MariaDB/Mongo | 130/130 PASS. Ba ca ORDER bị SKIP ở Maven mặc định đều chạy thật ở đây. Hai POST đồng thời cùng khóa chỉ thêm một lần. Một UUID mới thêm tiếp. Khách khác nhận 404 và thiếu CSRF nhận 403. Ca lọc kiểm hai khách, escape tiền tố, biên ngày UTC, count 11 và thứ tự trang thứ hai. Ca NOWAIT xác nhận khóa cha chặn thay đổi giữa đọc và ghi giỏ. [Log](../qa/ORDER/integration.txt) · [Ca ORDER](../qa/ORDER/cases.json) |
| WebKit | 2/2 kịch bản ORDER ở 1280 và 390 px, bao gồm bấm đúp, truy cập Guest/Customer khác, giỏ hỗn hợp và đặt đơn mới giá 225.000 VND. Cùng fixture chạy 4 nhóm VOUCHER (gồm JS/no-JS) và đối chiếu SQL ba đơn. [Log](../qa/ORDER/webkit.txt) |
| PAYMENT WebKit | 8/8 tổ hợp vai trò/viewport PASS. [Log](../qa/ORDER/webkit-payment.txt) |
| Package và diff check | PASS. JAR WebKit có cùng 413 entry ứng dụng với JAR build cuối. [Log](../qa/ORDER/package.txt) |

SQL trên fixture COD xác nhận sau ba lần gửi (hai lần cùng request), số đơn gốc vẫn là 1, attempt 505.000 VND, giá snapshot SKU 510.000, hai chứng từ CONSUMED 30.000/10.000, mỗi mã USED=1. Giá giỏ đọc lại là 490.000 VND. Không cộng các suite giao nhau thành tổng số ca độc lập.

**Finding đã xử lý.** Regression ban đầu phát hiện empty state khi không lọc bị đổi lời. Đã phân biệt lọc và chưa lọc rồi chạy lại suite. Runner WebKit được sửa để chọn đúng ô tìm mã đơn. Test khóa ban đầu kiểm sai lớp exception Spring; MariaDB trả HY000/1205 khi ghi xung đột. Test cuối kiểm mã 1205.

**ORDER-FINAL.** SHA-256 của 12 file source/test thay đổi là `bea633c0898d00d2826f58db63376846a45215ca931286b36242cfa1537624b2`. Manifest cũng có hash toàn bộ src. Cần review quyền sở hữu, khóa và biên nhận V3 trên candidate, so tree với bản kiểm thử, kiểm CI và owner nghiệm thu đúng revision. Biên nhận dùng chung bảng merge giỏ, tăng một dòng cho mỗi thao tác mới. Chưa đo tải hoặc dung lượng production. Manual UAT và MoMo Sandbox thật NOT RUN. AUTH-LIVE, PAY-LIVE và AUDIT PURGE giữ BLOCKED_EXTERNAL.

## ORDER-FINAL

Đã tái hiện và sửa lỗi chuyển form sang đơn khác cùng chủ. POST kiểm chữ ký gắn với phiên, chủ, đơn và UUID; biên nhận V3 cùng transaction giỏ tiếp tục chặn retry sau commit. Maven 252 PASS/107 SKIP, integration 132/132 PASS, WebKit ORDER 2/2, VOUCHER 4/4, PAYMENT 8/8 và package PASS. Bản FINAL chờ review PR, CI và quyết định owner. [Review cuối, manifest và log](../qa/ORDER/final-review/review.md)


## Closeout sau PR #14 · 28/11/2026

**ORDER đủ điều kiện trình owner nghiệm thu.** HEAD của main và origin/main cùng ở `13c586ac0e5e9c637e071dedfa67dfb07dd0a6da`. Merge có hai parent là baseline và candidate `1292b7616203047a28217a0fe307a8fa0fb8321e`. Candidate và merge cùng tree `d8f347e95b1a5b29e1a1caf92741938f043d26e7`. Đủ 35/35 blob đúng manifest, 13/13 file source/test khớp SHA-256 đã kiểm thử. Hash tổng hợp ORDER-FINAL là `a5a6f42ad7254159453ee59ae4e5dc547f528445c68c695f4850602809c5e3c2`. Owner đã xác minh CI Maven test/package completed/success tại đúng merge SHA. [PR #14](https://github.com/tuanank5/cosmetics_shop/pull/14) · [CI main](https://github.com/tuanank5/cosmetics_shop/actions/runs/46136054999) · [Kết quả đối chiếu](../qa/ORDER/closeout/verification.json)

Bản đã merge chứa bản sửa form từ đơn A gửi sang đơn B cùng chủ. Máy chủ kiểm CSRF, principal và quyền sở hữu trước khi ghi. Chữ ký ràng buộc phiên, Customer, đơn và UUID. Hai POST cùng thao tác chỉ cộng giỏ một lần. Hai thao tác chủ đích mới được xử lý riêng. Biên nhận mua lại và cập nhật giỏ cùng rollback khi giao dịch lỗi. Khóa biên nhận mua lại và gộp giỏ thuộc hai không gian UUID riêng. Review và fixture ORDER-FINAL đã đóng finding này. [Review cuối](../qa/ORDER/final-review/review.md)

| Bằng chứng kế thừa từ tree đã kiểm thử | Kết quả |
|---|---|
| Maven mặc định, package và diff check | 252 PASS, 107 SKIP. Package và diff check PASS. |
| Integration trên fixture | 132/132 PASS. Trong 107 phương thức SKIP mặc định, 91 phương thức có invocation PASS, 16 phương thức ngoài gate ORDER vẫn NOT RUN. |
| WebKit local | ORDER 2/2, VOUCHER 4/4 nhóm, PAYMENT 8/8 tổ hợp PASS. |
| SQL trước/sau mua lại | Đơn và attempt 505.000 VND, giá snapshot 510.000 VND, giá hiện hành trong giỏ 490.000 VND. Hai voucher CONSUMED 30.000 và 10.000 VND, USED=1 từng mã. Toàn bộ hàng dữ liệu của đơn, địa chỉ snapshot, dòng hàng, attempt và chứng từ được đối chiếu. Kho ba SKU giữ mức 11, 2 và 0. |

Các suite có giao nhau. Số liệu MariaDB và WebKit đến từ fixture local của ORDER-FINAL. Closeout kế thừa kết quả do tree trùng hoàn toàn. [Log fixture và WebKit](../qa/ORDER/final-review/review.md) · [Đối chiếu phương thức SKIP](../qa/ORDER/final-review/skip.json)

Stash `ORDER local files before sync` có object `f633f056da376d239e094507bc31b1ee03c35003`. Có 34/35 file trùng merge. Dòng AI-OPS là phần duy nhất cần lấy lại từ sổ tiến độ. Cả chín stash được bảo toàn.

**Gate còn mở.** Owner cần nghiệm thu ORDER tại merge SHA trên. Mỗi thao tác mua lại mới đã commit tạo một dòng biên nhận V3. Production chưa có retention và chưa đo tốc độ tăng, dung lượng hoặc tải. Việc dọn biên nhận cần chính sách về thời hạn retry để bảo toàn idempotency. Manual UAT và MoMo Sandbox thật vẫn NOT RUN. AUTH-LIVE, PAY-LIVE và AUDIT PURGE tiếp tục BLOCKED_EXTERNAL. Các giới hạn này thuộc nghiệm thu vận hành hoặc WP riêng. Tiến độ chính thức vẫn 5/19 DONE.
