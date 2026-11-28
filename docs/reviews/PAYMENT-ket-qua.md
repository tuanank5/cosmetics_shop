# PAYMENT · Lịch sử từng lần thanh toán

**DONE · Owner nghiệm thu ngày 28/11/2026.** Lịch sử payment attempt và bốn gate tích hợp sau VOUCHER trên `e4c00c8`, trong phạm vi COD và MoMo mock. [Quyết định owner](../DECISIONS.md#nghiệm-thu-voucher-payment-và-order-ngày-28112026)

## Lịch sử triển khai và kiểm chứng

Ngày 28/11/2026, owner giao phần độc lập sau BASE. Tại bước triển khai ban đầu, PAYMENT ở trạng thái **IN_PROGRESS**. HEAD và ref local origin/main cùng ở `98d2fa3da4e10ae030f99c3e9a5899de17e2cf28`. Bản kiểm thử là revision này cộng các tệp source/test trong [manifest SHA-256](../qa/PAYMENT/source.json). QA dùng checkout riêng `target/payment-07`. Bản nghiệm thu local của BASE, AUTH, QUALITY và PERF là cơ sở cập nhật tiến độ.

## Đối chiếu dữ liệu V1

| Thông tin | Nguồn và cách hiển thị |
|---|---|
| Đơn | Truy vấn theo khóa ngoại `lan_thanh_toan.ma_don_hang` sau kiểm tra chủ sở hữu hoặc quyền Admin. |
| ID attempt | `ma_lan_thanh_toan`, chỉ chi tiết Admin. |
| Lần thanh toán | Vị trí từ 1 của dòng thật trong đơn, tăng theo `ngay_tao`, sau đó theo khóa chính khi trùng thời gian. Đơn có 0 dòng hiển thị thông báo rỗng. |
| Phương thức | `cong_thanh_toan` của từng lần. COD, MoMo và MoMo mô phỏng có nhãn riêng. |
| Request ID | `ma_yeu_cau`. Chỉ Admin xem tham chiếu đúng dạng UUID hoặc COD kèm ID số. |
| Giao dịch cổng | `ma_giao_dich_cong`. Chỉ Admin xem tham chiếu dạng số. Thiếu hoặc ngoài định dạng hiển thị “Chưa ghi nhận”. |
| Số tiền | `so_tien` DECIMAL của từng dòng, định dạng tiền VND của dự án. |
| Trạng thái và kết quả | `trang_thai` của từng attempt. Hiển thị nhãn tương ứng với mã đã biết. V1 không lưu resultCode hoặc lý do thất bại. |
| Thời điểm tạo | `ngay_tao`, UTC của ứng dụng, trình bày theo giờ Việt Nam. |
| Thời điểm thanh toán | `ngay_thanh_toan` khi đã ghi nhận. NULL hiển thị “Chưa ghi nhận”, kể cả dòng PAID cũ thiếu mốc. Không có timestamp cập nhật chung. |
| Nguồn đối soát | V1 không phân biệt IPN và chủ động truy vấn cho kết quả đã lưu. Không gán nguồn theo loại cổng. |

## Phạm vi mã

Hai trang chi tiết dùng cùng fragment SSR. Projection Customer nhận NULL từ SQL cho cả ba tham chiếu nội bộ. Trạng thái dùng cho bộ lọc, nút xử lý và chuyển trạng thái vẫn theo truy vấn hiệu lực hiện hành. Chỉ thêm một SELECT theo chỉ mục khóa ngoại trên mỗi lần mở chi tiết. V1 đủ dữ liệu cho phạm vi đã giao, không có migration. Không thêm endpoint hoặc writer. Hợp đồng ghi của `Momo_Facade`, `ThanhToanMomo_Service` và giao dịch thu COD giữ nguyên.

## Kiểm chứng

Windows, Temurin JDK 21.0.13, Maven 3.9.11, MariaDB 12.2.2 trên loopback. Dữ liệu E2E gồm ba đơn fixture với 0, 1 và 2 attempt, hai dòng cùng thời điểm tạo để kiểm thứ tự theo ID. Integration dùng schema và datadir mới do runner tạo. Các giá trị thanh toán trực tuyến trong QA thuộc chế độ mock.

- `mvn -o -B -ntp test` với cache local, **238 PASS, 89 SKIP, 0 FAIL**. Các lớp có điều kiện môi trường được tính riêng khi chạy fixture. [Tổng hợp Maven](../qa/PAYMENT/test.txt)
- `mvn -o -B -ntp -DskipTests package` **PASS**. [Kết quả đóng gói](../qa/PAYMENT/package.txt)
- **MariaDB 95/95 PASS**, 0 FAIL, 0 SKIP. Luồng mock kiểm retry sau FAILED, tái sử dụng PENDING, hai yêu cầu đồng thời, chữ ký, order/gateway/amount sai, replay, thành công muộn và trạng thái hiệu lực. Regression COD có thu tiền khi giao, hủy, hoàn kho và quyền/CSRF. [Kết quả từng lớp](../qa/PAYMENT/integration.txt)
- **WebKit 8/8 PASS** (4 vai trò × 2 viewport). Guest chuyển tới login, Customer khác nhận 404. Customer chủ đơn và Admin thấy đúng màn rỗng, COD chưa thu và hai lần MoMo mô phỏng có cùng timestamp. Chỉ HTML Admin chứa mã yêu cầu và mã cổng đã gieo. Nút xác nhận đơn PAID hiển thị ở cả hai kích thước. [Kết quả](../qa/PAYMENT/webkit.txt) · [Các phép kiểm](../qa/PAYMENT/order-detail.cjs)

- **Git diff --check PASS** trên working tree.

### Tái lập

Từ checkout ứng viên sạch bytecode, đặt `JAVA_HOME` tới JDK 21 và `BASE_PLAYWRIGHT` tới module playwright-core đã cài WebKit. `$repo` là đường dẫn tuyệt đối tới Maven cache, `$qa` là thư mục bằng chứng PAYMENT. Hai runner tạo MariaDB riêng, kiểm tra cổng trống và dừng tiến trình khi kết thúc.

```powershell
mvn.cmd -o -B -ntp "-Dmaven.repo.local=$repo" test
mvn.cmd -o -B -ntp "-Dmaven.repo.local=$repo" -DskipTests package
powershell -NoProfile -ExecutionPolicy Bypass -File docs/qa/QUALITY/final-review/run-mariadb.ps1 -Checkout . -MavenRepo "$repo" -EvidenceDirectory "$qa"
powershell -NoProfile -ExecutionPolicy Bypass -File docs/qa/PAYMENT/run-order-detail.ps1
git diff --check
```

## Gate tại thời điểm review local

- **BLOCKED_DEPENDENCY / NOT RUN** — tổng tiền, snapshot và đối soát kho/lượt của hai slot sau VOUCHER.
- **NOT RUN** — provider MoMo thật, manual UAT, review PR và CI của revision ứng viên. PAY-LIVE chưa bắt đầu. AUTH-LIVE tiếp tục BLOCKED_EXTERNAL.

## PAYMENT-FINAL

**PAYMENT — PARTIAL_ACCEPTED / BLOCKED_DEPENDENCY**. Review bảo mật, nghiệp vụ và regression local đạt cho lịch sử độc lập trên đúng manifest. Tổng tiền, snapshot, đối soát và regression sau VOUCHER còn chờ dependency. [Review cuối và bằng chứng từng lượt](../qa/PAYMENT/final-review/review.md)

## Nghiệm thu sau merge

**Lịch sử thanh toán MERGED / PARTIAL_ACCEPTED.** HEAD và origin/main cùng ở `4fa226210c95a4539ccb4279a2a78df54af7577f`. Parent thứ hai `f734d9ecd717d96b3572f7ac7566ac2913636c50` và merge có cùng tree `f390fbe1fa14e934d92abc969ae5f675d1abb559`. Đã đối chiếu đủ 24 blob với manifest đã review. Owner xác nhận CI sau merge PASS cho revision này. Phiên closeout không truy cập được GitHub API để đọc lại kết quả run độc lập. [PR #10](https://github.com/tuanank5/cosmetics_shop/pull/10) · [CI main](https://github.com/tuanank5/cosmetics_shop/actions/runs/46120901806)

Bằng chứng trên tree đã merge ghi Maven 238 PASS, 89 SKIP, package PASS, MariaDB 95/95 và WebKit 8/8 (4 vai trò × 2 viewport). Guest đến login, Customer khác nhận 404, tham chiếu nghiệp vụ chỉ xuất hiện cho Admin. Retry song song dùng chung một PENDING mới sau FAILED. Replay và IPN muộn giữ đúng số attempt, trạng thái và kho/lượt trên fixture. Diff với parent main không có migration. Closeout chỉ đối chiếu bằng chứng đã chạy. [Review và log](../qa/PAYMENT/final-review/review.md)

Stash `PAYMENT local files before sync` (`8d72f257`) có 23/24 blob trùng main. Đã ghép các mốc nghiệm thu và điều kiện phụ thuộc còn thiếu vào sổ trên main, giữ nguyên stash. **4/19 DONE**. Tổng tiền tích hợp VOUCHER, snapshot hai slot, đối soát voucher/payment và regression sau VOUCHER còn BLOCKED_DEPENDENCY. PAY-LIVE nghiệm thu MoMo Sandbox thật riêng. AUTH-LIVE tiếp tục BLOCKED_EXTERNAL.

## Revalidation PR #12

28/11/2026 · **PARTIAL_ACCEPTED / READY_FOR_OWNER_ACCEPTANCE trong phạm vi mock.** Đã chạy lại trên checkout detached từ `e4c00c8dc977789eafec0a70a522f846a43898b4`. Phần lịch sử ở PR #10 tiếp tục đạt. Bốn gate tích hợp có kết quả dưới đây. Chờ owner xác nhận phạm vi mock trước khi chuyển DONE. PAY-LIVE nghiệm thu provider riêng.

| Gate | Kết quả chạy trên merge |
|---|---|
| 1 · Tổng tiền | PASS. COD hai dòng giá SKU 370.000 và 150.000, mỗi dòng một đơn vị. Phân bổ HÀNG 21.346 và 8.654. Phí chuẩn 25.000, giảm SHIP 10.000. Đơn và attempt 505.000. MoMo mock một SKU 520.000 dùng cùng hai mức giảm. SQL và spy trên lời gọi gateway xác nhận 505.000, sai lệch 0 VND. |
| 2 · Snapshot | PASS. SQL đọc hai dòng, hai chứng từ, đơn và attempt trước/sau khi cập nhật cả hai voucher trên fixture (giá trị, ngày hết hạn và quota). Hai bộ kết quả bằng nhau. Đơn 860.000 miễn ship chỉ có một slot. Kiểm hai unique và FK, V8→V9 giữ hai đơn legacy tổng 130.000 với một HELD và một CONSUMED. Lịch sử được kiểm riêng theo từng vai trò. |
| 3 · Quota và vòng đời | PASS. MoMo bắt đầu với HELD=1 mỗi mã, sau PAID mỗi mã USED=1 và HELD=0 kể cả replay. Khi đã release rồi nhận tiền muộn, test đối chiếu nghĩa vụ hoàn. Khi nghĩa vụ chưa rõ, fixture giữ HELD=1 và USED=0 cho cả hai mã. Hai checkout tranh lượt cuối chỉ một đơn thành công. Trigger gây lỗi ở slot hai của COD để lại USED=0 cho cả hai, kho vẫn 5, số đơn bằng 0. Fixture MoMo cũng kiểm rollback số HELD và số chứng từ về 0. Hủy hợp lệ và replay kiểm số lượt qua SQL. |
| 4 · Regression PAYMENT | PASS. COD thu khi giao, retry sau FAILED, dùng lại PENDING, từ chối sai signature/order/gateway/amount, callback trùng, đến muộn và race với kết quả hủy từ gateway. WebKit 1280 và 390 px xác nhận Guest về login, Customer khác nhận 404, chủ đơn thấy lịch sử và chỉ Admin thấy tham chiếu được phép. Đã kiểm trang rỗng, một và nhiều attempt. |

Maven mặc định 244 PASS/102 SKIP, integration 127/127 PASS, bootstrap 10/10 PASS, package PASS. Trong ba lượt Maven có 365 định danh invocation PASS khác nhau, 16 xuất hiện ở cả mặc định và integration. WebKit VOUCHER 4/4 nhóm, PAYMENT 8/8 tổ hợp vai trò/kích thước PASS. Phân loại 13 phương thức NOT RUN ngoài gate VOUCHER theo review candidate. MoMo Sandbox, manual UAT và hoàn tiền thật NOT RUN.

Các fixture tạo datadir mới, ban đầu không có đơn hoặc lượt. JDK Temurin 21.0.13, Maven 3.9.11, chế độ offline và bản sao cache Maven trong workspace. Lượt chuẩn bị đầu thiếu dependency ở cache project, sau đó compile lỗi với cache profile. Đã chạy lại thành công với cache riêng. Ba checkout `target/pay-reval-{merge,int,boot}` cùng SHA, suite mặc định và package dùng checkout merge. Integration dùng P01_MARIADB_TEST, AUTH_02_MIGRATION_FIXTURE và P23_MONGO_TEST; bootstrap dùng LOCAL_BOOTSTRAP_MIGRATION_FIXTURE. MariaDB loopback ở 33472/33475, MongoDB ở 33473. Browser dùng hai datadir MariaDB riêng lần lượt ở 33381. Schema đích Flyway V9. Fixture trình duyệt seed SKU, giỏ, mã và tài khoản theo runner. Script kiểm snapshot bổ sung chỉ chạy trong fixture. CI main chỉ chạy Maven test và package.

[Bằng chứng từng invocation và thời gian](../qa/PAYMENT/revalidation/invocations.json) · [Integration](../qa/PAYMENT/revalidation/integration.txt) · [Bootstrap](../qa/PAYMENT/revalidation/bootstrap.txt) · [WebKit VOUCHER và số liệu SQL](../qa/PAYMENT/revalidation/webkit-voucher.txt) · [WebKit PAYMENT](../qa/PAYMENT/revalidation/webkit-payment.txt)
