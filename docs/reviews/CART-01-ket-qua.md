# CART-01 · Thông báo điều chỉnh giỏ sau đăng nhập

Baseline `main` sạch tại `38fb1ca300ce9903db033da13ee4105588ea394a`, đã có trên `origin/main`. G3–G5 đã đạt. Checkpoint của gói là HEAD này cộng working diff.

## Thay đổi

- `GioHang_Service.gopSauDangNhap` trả `KetQuaGop` immutable gồm cờ điều chỉnh chung và các dòng còn tồn dương, mỗi dòng có tên sản phẩm/biến thể, số dự kiến, thực nhận và chênh lệch. Tên lấy từ `MucGio` đang khóa; dùng các câu SQL hiện có. Giữ V3 và thứ tự ghi nhận, khóa giỏ, khóa SKU theo mã tăng dần rồi lưu `min(tổng, khả dụng)`. SKU hết, ẩn hoặc mất giữ xử lý cũ và chỉ góp vào thông báo chung.
- `LoginSuccess_Handler`, `ThongBaoGopGio` và `GopGioNotice_Config` đặt thông báo sau khi lời gọi qua proxy đã commit. Session giữ kết quả theo mã người dùng, phiên bản xác thực và path đích. `cartMerge` là mã điều hướng ngẫu nhiên chỉ dùng để ghép GET với lượt login cùng session. URL không mang tên, số lượng hay ID tài khoản. Saved request giữ path/query, gồm đích checkout. Chỉ lần GET dựng HTML đúng đích nhận thông báo dưới mutex. Fetch, asset, tab đi vào giỏ độc lập và tài khoản khác không dùng được nó.
- `GioHang_Controller` bỏ đường đọc/tiêu thụ boolean cũ ở GET giỏ. `fragments/ui.html` dùng vùng thông báo hiện có của giỏ/checkout và fragment header cho các đích khác. `components.css`, `storefront.css` và `app.js` bố trí thông báo, phục vụ cả khi tắt JS, và bỏ DOM notice trước khi trình duyệt cất trang vào lịch sử. Phản hồi chứa thông báo dùng `Cache-Control: no-store, private`.
- Cập nhật `LoginRedirectTest`, `GioHangPagesTest`, `XacThuc_MariaDb_Test`, `OAuth2Callback_MariaDb_Test` và `XacThucDaPhuongThuc_MariaDb_Test`. Thêm fixture QA MariaDB và Chrome tại `docs/qa/CART-01/`.

## Hợp đồng bàn giao

Caller đọc `KetQuaGop.daGioiHan()` thay boolean và chỉ dựng thông báo sau commit. Checkout tiếp tục tính lại từ giỏ và địa chỉ theo đường hiện có. QA-01 cần giữ các ca một lần theo UUID, refresh/Back, logout và ba login trên cùng ID. WISH-01 nên giữ saved request và bỏ qua tham số UI `cartMerge` khi dựng nguồn từ catalog.

## Kiểm thử

- Temurin 21.0.13.1, Maven 3.9.15. `mvn -o test` đạt **236/300**, 64 skip theo profile, 0 lỗi. `mvn -o -DskipTests package` thành công. [Log Maven](../qa/CART-01/maven-results.txt)
- `run-fixture.ps1 -Integration` đạt **69/69**, gồm `XacThuc_MariaDb_Test`, `GioHangPagesTest`, checkout/COD, auth đa phương thức và redirect. Hai phiên cùng tài khoản đăng nhập đồng thời cho kết quả 12→10 và 15→10, hai UUID ghi một lần và thông báo thuộc đúng từng phiên. Kiểm 5 + 7 với tồn 10 qua Form Login, Google và GitHub trên cùng ID. LINK/REAUTH giữ snapshot; reset hủy phiên và không ghi V3. Trigger fixture làm lỗi ghi SKU thứ hai, rollback cả mục đã ghi lẫn V3, giữ snapshot để retry. Sau retry thành công, gửi lại UUID không thay đổi giỏ và không tạo notice mới. [Log tích hợp](../qa/CART-01/integration-results.txt)
- Chrome headless 390px kiểm tab khác đọc giỏ trong khi lượt điều hướng checkout đang đợi. Giỏ đã có 10 SKU A, tab giỏ không hiện hoặc lấy mất thông báo. Checkout hiện đúng một thông báo gồm 12→10 giảm 2 và 4→3 giảm 1. Back, refresh và logout không phát lại. Tài khoản khác không xem được thông báo cũ. Lượt checkout khi tắt JavaScript nhận thông báo qua HTML sau đăng nhập bằng bàn phím. [Kết quả Chrome](../qa/CART-01/browser-results.json) · [Ảnh checkout](../ui/screenshots/CART-01/checkout-390-notice.webp)
- Google/GitHub chạy qua callback fixture có phiên và mã trả về. Chưa xác minh nhà cung cấp thật hoặc trên hai thiết bị vật lý.
- `git diff --check` đạt, diff staged trống. Không commit/push. **G7 đạt trên fixture.**

## Quay lui

Nếu tiến trình chết sau commit nhưng trước khi chuyển kết quả vào phiên, giỏ đã gộp và V3 chặn gộp lại trong khi thông báo có thể mất. Quay lui phần hiển thị về thông báo chung giữ nguyên dữ liệu và V3.
