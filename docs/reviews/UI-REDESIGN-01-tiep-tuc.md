# UI-REDESIGN-01 · Checkpoint

HEAD `daea756053021310d9fd1c6ada9475564711fac6` trên `main`. Index sạch khi bắt đầu. Working tree đã có 21 tệp tracked thay đổi và các tệp báo cáo, fixture chưa theo dõi từ các đợt trước. Các thay đổi redesign dựa trên working tree này.

## Trạng thái

- A **PASS**. Đối chiếu tài liệu, patch và hợp đồng render thông qua Thymeleaf, truy vấn MariaDB và JavaScript hiện hữu. Không thấy bốn ảnh tham chiếu riêng của yêu cầu.
- B–F **Đã triển khai**. Checkout báo giá khi mở trang và khi đổi địa chỉ/voucher, chi tiết đơn hàng riêng cho Customer, Chat là đích tư vấn thống nhất, hành động phiên ở header bên phải, Wishlist thẻ sản phẩm và nút đóng Khám phá ở đầu trái panel.
- G–H **PASS** ở Maven, integration, Node VM và Git. Maven test có 317 khai báo, 241 chạy đạt, 76 skipped. Integration MariaDB/MongoDB độc lập có 47 test đạt. Checkout VM có 5 test đạt. HTML trước/sau có bằng chứng đối chiếu trong thư mục QA.
- Nghiệm thu trực quan **BLOCKED**. Browser tích hợp trả `No browser is available` và danh sách phiên rỗng. BIZ-UX-03 đã khoanh vùng Chrome/Edge/Firefox headless. Kiểm thủ công ở tám viewport, zoom 200%, Tab/Escape, reduced motion và Home đến footer còn **NOT RUN**. [Checklist kiểm trực quan](../qa/UI-REDESIGN-01/manual-visual-qa.md)

## Chạy tiếp

Từ root dự án, dùng PowerShell. Script dựng sao chép nguồn vào `target/ui-redesign-01/current` và dùng cache Maven cô lập. Script fixture mở MariaDB, MongoDB và HTTP trên loopback với datadir và upload riêng theo mỗi lượt.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-REDESIGN-01/build.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-REDESIGN-01/run-fixture.ps1 -Phase integration
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-REDESIGN-01/run-fixture.ps1 -Phase http -Revision before
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-REDESIGN-01/run-fixture.ps1 -Phase http -Revision after
node --test docs/qa/UI-REDESIGN-01/checkout.test.mjs
git diff --check
```

`before` đọc JAR đã lưu của BIZ-UX-03 tại `target/biz-ux-03/current/target/beautyshop-0.0.1-SNAPSHOT.jar`. `after` đọc bản dựng UI-REDESIGN-01. Cả hai đều chạy trên fixture mới cùng cấu hình và dữ liệu seed. Khi kiểm trực quan thủ công, dùng `-Phase manual -Revision before` hoặc `-Phase manual -Revision after`. Script hỏi mật khẩu tạm và dừng các tiến trình fixture khi thoát. Bảo toàn dữ liệu runtime và thay đổi chưa commit của người dùng. Báo cáo môi trường ghi mã SHA-256 để đối chiếu JAR.

[Báo cáo](UI-REDESIGN-01-ket-qua.md) · [QA](../qa/UI-REDESIGN-01/)
