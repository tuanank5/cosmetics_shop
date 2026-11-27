# BIZ-UX-03 · Checkpoint tiếp tục

HEAD `daea756053021310d9fd1c6ada9475564711fac6` trên `main`. Index trống. Cả 316 tệp hiện có trong `src` khớp SHA-256 với snapshot BIZ-UX-02 sau sửa và nguồn build BIZ-UX-03. Giữ nguyên working tree BIZ-UX-01/02, bản backup patch và dữ liệu cũ.

- Một vòng chẩn đoán Chrome/Edge với profile và disk cache riêng đã hoàn tất. Dừng headless sau khi hai trình duyệt báo GPU access denied và CDP cấp tab timeout. Manual QA là chặng còn mở.
- Maven test 242 đạt, 76 skip và package exit 0. Integration 38/38, HTTP 35 phép kiểm route và VM giỏ đạt. SQL kiểm một đơn, tổng 165.000 ₫, tồn còn 19 và voucher dùng một lượt. Runtime hiện tại là `target/biz-ux-03/current/target/beautyshop-0.0.1-SNAPSHOT.jar`. Tiến trình app/DB của các lượt tự động đã dừng.
- [Báo cáo nghiệm thu](BIZ-UX-03-nghiem-thu-giao-dien.md) · [Checklist manual](../qa/BIZ-UX-03/manual-visual-qa.md) · [Mẫu kết quả](../qa/BIZ-UX-03/mau-ket-qua.md)

## Chạy lại

Nếu bản dựng đã có, dùng script trực tiếp. Khi cần dựng ở máy khác, sao chép `pom.xml` và toàn bộ `src` của working tree đang có vào thư mục riêng `target/biz-ux-03/current`. Bản dựng phải gồm cả thay đổi chưa commit của BIZ-UX-01/02. Dùng bản chép cache Maven vào `target/biz-ux-03/maven` hoặc nạp dependency vào cache này. Giữ mọi fixture và cache cũ.

```powershell
$env:JAVA_HOME = Split-Path (Split-Path (Get-Command java.exe).Source)
$qaCache = (Resolve-Path 'target/biz-ux-03/maven').Path
mvn.cmd -B -o -ntp "-Dmaven.repo.local=$qaCache" -f target/biz-ux-03/current/pom.xml test
mvn.cmd -B -o -ntp "-Dmaven.repo.local=$qaCache" -f target/biz-ux-03/current/pom.xml -DskipTests package
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/BIZ-UX-03/run-fixture.ps1 -Phase http
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/BIZ-UX-03/run-fixture.ps1 -Phase integration
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/BIZ-UX-03/run-fixture.ps1 -Phase manual
```

Xác nhận Java và Maven đều dùng JDK 21. Với `mvn test` mặc định, các gate `P01_MARIADB_TEST`, `P23_MONGO_TEST`, `AUTH_02_MIGRATION_FIXTURE`, `LOCAL_BOOTSTRAP_MIGRATION_FIXTURE`, `P21_DEMO_REPORT`, `ARCH_01_BSON_PHASE`, `DATA_01_MONGOD` phải không được bật. Lượt này các gate không có sẵn và biến nối dịch vụ của dự án được tách khỏi tiến trình kiểm thử. Script integration tự chọn suite và chỉ bật gate dùng datadir mới.

`manual` hỏi mật khẩu tạm, in URL loopback và chờ người kiểm thao tác. Đóng fixture bằng Enter ở terminal. Kết quả cần lưu trong [mẫu](../qa/BIZ-UX-03/mau-ket-qua.md) và lưu ảnh thật tại `docs/ui/screenshots/BIZ-UX-03/`. Đánh dấu màn chưa chạy là NOT RUN và provider thiếu là BLOCKED. Chỉ tạo ảnh trước/sau nếu có lỗi tái hiện và một bản sửa, dùng cùng fixture và viewport.

Hướng tiếp theo là kiểm Home ở desktop và touch, giỏ hai tab/timeout, checkout COD, focus/lỗi SKU, sau đó hoàn tất ma trận màn hình và zoom 200%. Nếu phát hiện lỗi, ghi bước tái hiện và ảnh/DOM trước khi chỉnh source. Chưa commit, push hay deploy.
