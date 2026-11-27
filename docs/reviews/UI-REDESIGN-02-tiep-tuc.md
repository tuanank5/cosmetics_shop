# UI-REDESIGN-02 · Checkpoint

HEAD `daea756053021310d9fd1c6ada9475564711fac6` trên `main`. Index sạch lúc bắt đầu. Working tree có 40 tệp tracked thay đổi cùng mã và tài liệu chưa theo dõi của các đợt trước. Bốn ảnh tham chiếu đã được mở và xác nhận 1672 × 941. [Thông tin ảnh](../qa/UI-REDESIGN-02/reference-evidence.json)

## Tiến độ

P1–P5 đã triển khai theo bốn ảnh. P6 **PASS** ở Maven (241 đạt, 76 skipped), package, integration (47/47), HTTP trước/sau, VM Checkout (5/5) và kiểm diff. Bằng chứng trực quan ở 1672 × 941 và kiểm responsive trình duyệt thật **BLOCKED** trong phiên này. Lượt kiểm thủ công là **NOT RUN**.

## Chạy tiếp

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-REDESIGN-02/build.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-REDESIGN-02/run-fixture.ps1 -Phase integration
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-REDESIGN-02/run-fixture.ps1 -Phase http -Revision before
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-REDESIGN-02/run-fixture.ps1 -Phase http -Revision after
node --test docs/qa/UI-REDESIGN-01/checkout.test.mjs
git diff --check
```

`before` dùng JAR UI-REDESIGN-01 tại `target/ui-redesign-01/current/target/beautyshop-0.0.1-SNAPSHOT.jar`. `after` dùng bản mới tại `target/ui-redesign-02/current/target/beautyshop-0.0.1-SNAPSHOT.jar`. Cache Maven, thư mục bản dựng, datadir và upload cho lượt này nằm dưới `target/ui-redesign-02/`. Lượt integration sao chép nguồn vào build riêng. Không cần reset database, xóa target hoặc chỉnh Flyway.

Khi có browser, làm theo checklist, so bốn ảnh gốc với ảnh ứng dụng tại cùng viewport và ghi sai lệch từng hạng mục. Chụp Wishlist sau khi mở Khám phá. Thực hiện tiếp ma trận responsive, zoom và keyboard. [Checklist](../qa/UI-REDESIGN-02/manual-visual-qa.md) · [Báo cáo](UI-REDESIGN-02-ket-qua.md)
