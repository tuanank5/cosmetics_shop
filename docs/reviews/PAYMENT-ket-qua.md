# PAYMENT · Lịch sử từng lần thanh toán

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

## Gate còn mở

- **BLOCKED_DEPENDENCY / NOT RUN** — tổng tiền, snapshot và đối soát kho/lượt của hai slot sau VOUCHER.
- **NOT RUN** — provider MoMo thật, manual UAT, review PR và CI của revision ứng viên. PAY-LIVE chưa bắt đầu. AUTH-LIVE tiếp tục BLOCKED_EXTERNAL.

## PAYMENT-FINAL

**PAYMENT — PARTIAL_ACCEPTED / BLOCKED_DEPENDENCY**. Review bảo mật, nghiệp vụ và regression local đạt cho lịch sử độc lập trên đúng manifest. Tổng tiền, snapshot, đối soát và regression sau VOUCHER còn chờ dependency. [Review cuối và bằng chứng từng lượt](../qa/PAYMENT/final-review/review.md)
