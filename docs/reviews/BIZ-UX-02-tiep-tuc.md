# BIZ-UX-02 · Checkpoint

HEAD `daea756053021310d9fd1c6ada9475564711fac6` trên `main`. Index trống khi bắt đầu. Baseline là HEAD cộng 15 tệp tracked thay đổi BIZ-UX-01. Các hồ sơ chưa theo dõi, gồm `docs/qa/BIZ-UX-01.zip` và `env`, đã có từ trước. Baseline đã đóng gói từ bản chép source trong `target/biz-ux-02/baseline`.

- **A:** Browser tích hợp trả danh sách rỗng. Chrome và Edge kết nối được tiến trình chính, nhưng tab timeout ở `Page.enable`. Log ghi GPU process crash và lỗi cache. Chrome thử tùy chọn kết xuất CPU timeout tại `Browser.getVersion`. Firefox không tạo được tab renderer ở lượt này và BIZ-UX-01. [Chẩn đoán](../qa/BIZ-UX-02/)
- **B–D:** Đã sửa predicate Swiper cho mobile, link tư vấn da tới đúng SKU, tham chiếu lỗi form SKU và màu viền focus kỳ đang chọn. Kiểm kê 35 lượt GET HTTP mỗi revision và hành trình Guest/Customer/Admin với cùng fixture.
- **E–F:** PASS 12 trường hợp predicate, số contrast CSS, quét cấu trúc SSR và hồi quy VM giỏ BIZ-UX-01. Visual QA, zoom 200%, keyboard và regression thực trong browser vẫn BLOCKED.
- **G:** Đã lưu số liệu HTML/script/CSS và mẫu đến headers trước/sau. Đo browser performance BLOCKED.
- **H:** Maven test 242 đạt, 76 skip. Package exit 0. Integration 22/22. HTTP trước/sau exit 0. `git diff --check` exit 0. Hai thư mục ảnh chưa có ảnh thật.

JDK 21 được chọn qua `JAVA_HOME` của tiến trình. Maven dùng cache và bản dựng riêng trong `target/biz-ux-02`. Không chạy `mvn clean` ở root.

## Chạy lại

Mỗi bản dựng có bản chép `pom.xml` và `src` riêng. Bản trước cần cây nguồn lúc bắt đầu BIZ-UX-02, gồm các sửa chưa commit của BIZ-UX-01. Bản sau lấy từ working tree hiện tại. Chạy Maven với `-f target/biz-ux-02/<build>/pom.xml` và `-Dmaven.repo.local=<cache-cô-lập>`, rồi đóng gói bằng `-DskipTests package`. Hai JAR nằm dưới `<build>/target/beautyshop-0.0.1-SNAPSHOT.jar`.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/BIZ-UX-02/run-fixture.ps1 -Phase before
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/BIZ-UX-02/run-fixture.ps1 -Phase after
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/BIZ-UX-02/run-fixture.ps1 -Phase integration
node docs/qa/BIZ-UX-02/source-check.mjs
```

Để kiểm lại trình duyệt, chạy fixture before và after với `-BrowserExe <đường-dẫn-chrome.exe>` hoặc chọn `-Browser edge -BrowserExe <đường-dẫn-msedge.exe>`. Probe làm smoke trên HTML tối giản trước, sau đó mới mở BeautyShop, đọc DOM và chạy tám viewport Home. Probe lưu ảnh tại 390 và 1440 px nếu tab render được. Chạy trên máy có browser hoạt động để hoàn tất zoom thật, kiểm layout các màn còn lại và regression E2E giỏ. Lấy các ca trong báo cáo và harness BIZ-UX-01 làm mốc, không dùng ảnh minh họa thay cho ảnh thực tế.

Fixture khởi tạo V1–V7 trên hai datadir và bộ uploads riêng cho từng lượt. Các tiến trình QA đã dừng. Ứng dụng và dịch vụ có sẵn của người dùng vẫn nằm ngoài fixture. Chưa commit, push hay deploy.
