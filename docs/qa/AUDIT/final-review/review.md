# AUDIT-FINAL · Review nhánh QA

Parent `4fa226210c95a4539ccb4279a2a78df54af7577f`. 28 hash ban đầu trùng [manifest triển khai](../source.json). Source sửa sau review được khóa riêng trong [manifest cuối](source.json). Nhánh bàn giao là `qa/audit` trong `target/qa-audit`.

### Findings và quyết định vận hành

| Finding | Cách xử lý và phạm vi kiểm |
|---|---|
| D6 · Thiếu dấu bảo lưu trên hồ sơ bên ngoài có thể dẫn đến xóa nhầm | Dry-run trên dòng cũ chưa phân loại tái hiện lỗi. Mặc định khóa vận hành. Khi có nguồn, cả count và DELETE kiểm ID, batch và hạn xác nhận. Giữ kiểm FK bảo lưu, đối soát ORDER, khóa tài khoản, phiên và CSRF. Retry dùng biên nhận đã commit của cùng actor/action, hoặc chỉ có thể xét lại ID được nguồn chấp thuận. |
| D7 · Form lưu y nguyên sinh SUCCESS | Đã tái hiện bằng lần lưu lại danh mục. So sánh trường sau chuẩn hóa cho danh mục, sản phẩm và SKU; UPDATE mã giảm kiểm cấu hình cũ ngay trong SQL, bao gồm trường nullable. Chỉ ghi SUCCESS khi có thay đổi. |
| D7 · Edit SKU vừa đổi tồn vừa đổi metadata | Điều kiện cũ chỉ nhìn cờ active để quyết định có ghi CATALOG_UPDATE cùng SKU_STOCK. Điều kiện mới xét trường catalog thực tế, dùng chung correlation khi cả hai action phát sinh. |

Hai testcase trên code đầu đều FAIL trước bản sửa. [Bằng chứng đỏ](red.txt)

**Purge thật BLOCKED_EXTERNAL.** Chưa có nguồn đối soát/tranh chấp bên ngoài được owner xác nhận. Rủi ro còn lại là nguồn upstream phê duyệt sai, chậm thu hồi hoặc dùng lại batch ID. Owner cần kiểm phạm vi nguồn, tính mới, quyền ghi tách khỏi tài khoản ứng dụng và quy trình thu hồi trước khi bật. Fixture mô phỏng nguồn bằng bảng riêng. Không có cấu hình bật mặc định, role mới, job purge tự động hoặc thao tác sửa/xóa trong màn tra cứu.

### Phạm vi D7

| Entry point | Truy vết |
|---|---|
| Chuyển/hủy đơn bởi Admin | ORDER_STATUS từ trạng thái thực trước/sau, ID đơn. Tác vụ thanh toán và trừ kho COD tự động giữ chứng từ riêng. |
| Save, status, delete mềm catalog/SKU | Các đường ghi dùng service có transaction; endpoint ngừng SKU đi qua phương thức ngoài đã mang transaction. Snapshot cờ active hoặc số tồn; tên, giá trị form, ảnh không được ghi vào snapshot. |
| Mã giảm hiện hành | CREATE/UPDATE/DELETE ở thao tác Admin. Không mở VOUCHER hai slot. |
| Auth V7 | Đọc được dòng cũ, nullable hiển thị “Chưa ghi nhận”. |
| ACCOUNT, RETURN, REFUND | BLOCKED_OWNER/BLOCKED_DEPENDENCY theo ma trận lý do khóa/mở, kiểm định, chứng từ tái nhập và hoàn. |
| NOTIFY, BACKUP, AI | BLOCKED_OWNER cho hợp đồng tác vụ, kết quả I/O và failure. |

Actor lấy từ principal, role kiểm ở backend. Audit writer dùng INSERT và unique (correlation_id, hanh_dong); không dùng UPDATE/UPSERT để ghi FAILED. SUCCESS dùng transaction đang ghi nghiệp vụ. Callback chỉ ghi FAILED khi transaction đã rollback, mở REQUIRES_NEW bằng TransactionTemplate. Mốc thời gian từ DB UTC. Snapshot chỉ nhận giá trị số, boolean, OrderStatus hoặc null. Hai route GET trả SSR dùng th:text; route vận hành POST trả số liệu tổng hợp. Không thấy đường serialize request/entity, credential, OTP, cookie, dữ liệu cá nhân, IPN hoặc thông tin ngân hàng trong mã ghi mới.

### V8, UI và nghiệm thu

V1–V7 đối chiếu blob với parent. V8 additive giữ nguyên bản ban đầu. Fixture trống migrate đến V8; hai schema V5/V6 lên V7, thêm auth event cũ rồi migrate V8, validate và migrate lại 0 thay đổi. Regression kiểm unique, FK RESTRICT, thứ tự cột chỉ mục và kết quả nullable. Kiểm plan có giới hạn ở truy vấn ngày và phân trang trên fixture. Tìm chuỗi con bằng LOCATE, CAST ID hoặc lọc actor/result có thể quét nhiều dòng. Chưa benchmark tải thật hoặc khẳng định mọi bộ lọc dùng index.

List/detail yêu cầu ADMIN ở HTTP và tầng truy vấn. Sort allowlist có ma_nhat_ky phân xử. WebKit kiểm Guest, Customer, hai tab, query/page, anchor khi tắt JS, focus và vị trí dòng khi bật JS, CSRF và focus về bộ đếm sau khi fixture dọn dòng. Review PR, CI GitHub, UAT và nguồn bảo lưu thật còn mở. Tổng tiến độ 4/19 DONE. PAYMENT PARTIAL_ACCEPTED / BLOCKED_DEPENDENCY, AUTH-LIVE BLOCKED_EXTERNAL.

### Phân biệt các lượt kiểm

Lượt regression bổ sung kiểm no-op voucher ban đầu có 1 error do gọi service ngoài phiên sau request MockMvc. Test đã chuyển sang gửi lại POST với giá trị cấu hình đang lưu và kiểm số event. [Lượt trước khi sửa fixture](integration-before-fixture-fix.txt)

Maven của PAYMENT có 89 SKIP. Bản AUDIT đầu thêm 4 phương thức của Audit_MariaDb_Test, cả lớp yêu cầu P01_MARIADB_TEST=true. Bốn phương thức là commitRollbackVaKetQuaRieng, batchBoundaryBaoLuuReplayVaRollback, rangBuocCorrelationActionVaBaoLuuDonChoDoiSoat và adminKhongDuocPhanCong. AUDIT-FINAL thêm dryRunKhongTinhHoSoChuaCoNguonDuyet và luuLaiDanhMucKhongSinhSuccess dưới cùng điều kiện, nên lượt mặc định hiện có 95 SKIP. Các ca tham số hóa có thể sinh nhiều invocation trên fixture.

Trên source cuối, Maven JDK 21 đạt **240 PASS, 95 SKIP, 0 FAIL**. MariaDB đạt **104/104 PASS**, package PASS, WebKit **3/3 nhóm PASS**. Trong 95 phương thức SKIP mặc định, 67 có invocation PASS ở fixture này và 28 vẫn NOT RUN. Danh sách kèm lý do JUnit theo từng phương thức. [Maven](test.txt) · [MariaDB](integration.txt) · [SKIP/NOT RUN](skip.json) · [WebKit](webkit.txt) · [Package](package.txt)

EXPLAIN trên hai schema trả về key ix_nhat_ky_thoi_gian, type range cho truy vấn ngày/sort/page đã kiểm. [Plan fixture](migration.txt)

WebKit cũng kiểm riêng cấu hình mặc định. Với nguồn chưa bật, Admin được phân công nhận 409, Customer nhận 403 dù gửi thêm actor/cờ từ client. Cả 26 dòng fixture còn nguyên. [Kiểm gate mặc định](default-gate.txt)
