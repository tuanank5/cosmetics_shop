# BIZ-UX-03 · Nghiệm thu giao diện BeautyShop

**Kết quả hiện tại:** headless vẫn **BLOCKED**. Đã chuyển sang checklist nghiệm thu thủ công với fixture riêng có thể mở bằng Chrome/Edge thông thường. Nghiệm thu thủ công **NOT RUN** trong phiên này, chưa có ảnh trước/sau.

## Revision và môi trường

HEAD `daea756053021310d9fd1c6ada9475564711fac6` trên `main`, index trống. Đã so SHA-256 từng tệp của 316 tệp dưới `src` với bản BIZ-UX-02 sau sửa và bản build BIZ-UX-03. Không có khác biệt ở cả hai phép so. `BIZ-UX-02-backup.patch`, các file chưa theo dõi và hồ sơ QA cũ đã có từ trước. [Snapshot](../qa/BIZ-UX-03/revision.json)

Windows 11, Temurin 21.0.13, Maven 3.9.11 và Node 26.13.0. Maven kế thừa Java 17 theo `JAVA_HOME`, nên các lệnh QA chọn JDK 21 ở tiến trình riêng. Build, cache Maven, schema MariaDB, MongoDB và uploads nằm trong `target/biz-ux-03/`. Các tiến trình fixture dùng cổng tự cấp trên loopback và Flyway V1–V7 từ JAR. Dịch vụ người dùng ở 8081 và database có sẵn nằm ngoài fixture.

## Khoanh vùng browser blocker

| Kiểm tra | Kết quả | Bằng chứng |
|---|---|---|
| Browser tích hợp | **BLOCKED** | Kết nối không có browser khả dụng. Discovery trả danh sách trống. |
| Chrome 158.0.8127.37 | Transport cấp browser **PASS**, renderer **BLOCKED** | Profile và disk cache mới đều ghi được file probe. CDP trả phiên bản và danh sách tiến trình gồm browser và network service, không thấy renderer tại thời điểm lấy mẫu. `Page.enable` và `Target.attachToTarget` chế độ flatten cùng timeout. [Chrome](../qa/BIZ-UX-03/chrome-separate-cache.json) |
| Edge 158.0.4370.33 | Transport cấp browser **PASS**, renderer **BLOCKED** | Cùng thiết lập profile/cache riêng. Browser-level CDP trả lời. Hai thao tác trên tab cùng timeout. [Edge](../qa/BIZ-UX-03/edge-separate-cache.json) |
| Firefox | **BLOCKED** theo bằng chứng BIZ-UX-02, **NOT RUN** lượt này | Gecko không tạo được tab subprocess tại `SpawnTarget`, mã `-2147024809`. Không có ảnh từ lần thử trước. [Firefox](../qa/BIZ-UX-02/firefox-probe-stderr.log) |
| DOM trong tab, click, nhập liệu và screenshot | **BLOCKED** | Lượt Chrome/Edge dùng HTML tối giản do server loopback trả trước khi truy cập ứng dụng. Cả hai dừng ở phần kết nối tab. |

Chrome và Edge ghi GPU subprocess thoát với `-1073741790`, tương ứng `0xC0000022` (`STATUS_ACCESS_DENIED`), cùng lỗi mở cache `0x20` (`ERROR_SHARING_VIOLATION`). Cache lỗi là GPU cache trong profile mới, dù disk cache HTTP đã tách riêng. Phiên thử còn khoảng 1,4 GiB RAM tự do và 75 GiB dung lượng đĩa, đủ để xác nhận thư mục được tạo và file probe được ghi. Từ bằng chứng này, blocker nằm ở quá trình khởi tạo tiến trình browser/GPU/renderer trong môi trường thực thi. Cơ chế Windows cụ thể gây từ chối và tiến trình giữ file GPU cache chưa xác định. Không cần tiếp tục thử headless lặp lại trong cùng điều kiện. [Microsoft NTSTATUS](https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-erref/596a1078-e883-4972-9bbc-49e60bebca55) · [Microsoft Win32 codes](https://learn.microsoft.com/en-us/windows/win32/debug/system-error-codes--0-499-) · [Chromium multi-process architecture](https://www.chromium.org/developers/design-documents/multi-process-architecture/)

## Trạng thái theo màn hình

**PASS** ở cột hồi quy là xác nhận trong HTTP, MVC, integration hoặc VM ghi ở từng dòng. **BLOCKED** ở cột trực quan là do renderer. Kiểm thủ công tất cả các dòng đang **NOT RUN**.

| Màn | Hồi quy trong lượt này | Nghiệm thu trực quan |
|---|---|---|
| Home, sáu chương, header/mobile navigation/footer | **PASS** HTTP cho sáu section SSR và hai đích tư vấn. Điều kiện Swiper trên các viewport kế thừa kết quả source-check 12 ca của BIZ-UX-02. | **BLOCKED** cuộn tới footer, wheel, chạm, tương tác biểu mẫu, reduced motion và fallback không JS trong tab. |
| Catalog, tìm/lọc, chi tiết và SKU | **PASS** HTTP cho catalog, bộ lọc và hai biến thể còn/hết hàng, cùng tích hợp catalog/SKU. | **BLOCKED** bố cục grid, ảnh thiếu, tên dài, chọn SKU bằng bàn phím. |
| Giỏ hàng | **PASS** HTTP thêm SKU và gộp giỏ. VM có đối chứng hai SKU, phản hồi treo/mất, cập nhật hai tab mô phỏng, subtotal/badge và 0 lần tự gửi lại POST. | **BLOCKED** hai tab thật, hộp thoại xóa 0, Escape, focus restoration và mạng chậm trong tab. |
| Checkout, địa chỉ, voucher, COD | **PASS** HTTP gộp giỏ, tạo địa chỉ, giữ mã sai, báo giá 165.000 ₫ với `FIXTURE10` và một đơn sau hai POST đồng thời. MVC và tích hợp kiểm quyền, tiền, tồn và trạng thái COD. | **BLOCKED** zoom, hướng dẫn khôi phục lỗi, trạng thái thiếu tồn trong tab. |
| Login, đăng ký, OTP, quên/đặt lại mật khẩu | **PASS** HTTP form đăng ký/quên mật khẩu và đăng nhập Customer/Admin. **NOT RUN** OTP, reset qua provider thật và OAuth trong fixture của lượt này. | **BLOCKED** lỗi form, focus, autofill, toggle mật khẩu. |
| Chatbot và tư vấn da | **PASS** hai route và lỗi provider tắt qua HTTP. MVC/service kiểm đích PDP tới đúng biến thể với phản hồi mô phỏng. **NOT RUN** Ollama thật. | **BLOCKED** đọc kết quả, focus và tương tác form trong tab. |
| Wishlist, lịch sử, chi tiết đơn, đánh giá và đổi trả | **PASS** HTTP mở các trang và tích hợp trên giao dịch/đánh giá từ fixture mua hàng. | **BLOCKED** trình bày dữ liệu nhiều dòng, trạng thái rỗng và thao tác có/không JS. |
| Admin Dashboard | **PASS** HTTP và tích hợp dashboard. Biên contrast kỳ đang chọn kế thừa phép đo màu BIZ-UX-02. | **BLOCKED** viền focus, biểu đồ, bảng và cuộn ngang cục bộ. |
| Admin CRUD và đơn | **PASS** HTTP các nhóm quản trị, multipart giá SKU âm có `aria-invalid` và mô tả gắn lỗi, luồng xác nhận đơn. Customer vào Admin nhận 403. | **BLOCKED** thao tác pointer/keyboard, form nhiều trường và bảng ở viewport hẹp. |
| Trang lỗi và trạng thái không có dữ liệu | **PASS** HTTP route 404, 403 theo vai và các danh sách rỗng trong fixture. | **BLOCKED** bố cục, thông báo và CTA khôi phục ở 200%. |

Viewport 320/360/390/768/1024/1280/1440/1920 CSS px và zoom trang 200% chưa đo ở trình duyệt thật. Không có lỗi UI mới đáp ứng ngưỡng bằng chứng để sửa trong BIZ-UX-03. Thay đổi của lượt này nằm ở harness và tài liệu, các bản sửa sản phẩm từ hai đợt trước được kiểm hồi quy.

## Bằng chứng và điều kiện release

- Maven `test` trong build riêng chạy 318 ca, **242 PASS**, 76 skip theo gate môi trường, 0 fail/error. `package -DskipTests` và `git diff --check` exit 0. [Test](../qa/BIZ-UX-03/maven-test.log) · [Package](../qa/BIZ-UX-03/maven-package.log)
- MariaDB/MongoDB integration **38/38 PASS**. HTTP **35** phép kiểm route đạt trên JAR thật. Sau hai POST checkout đồng thời và bước Admin xác nhận, MariaDB có đúng **1 đơn**, 1 dòng hàng và tổng **165.000 ₫**. Tồn SKU từ 20 xuống 19 và mã giảm giá ghi đúng 1 lượt. [Integration](../qa/BIZ-UX-03/maven-integration.log) · [HTTP](../qa/BIZ-UX-03/http-regression.json) · [SQL đối chiếu](../qa/BIZ-UX-03/cod-db-check.json)
- Hồi quy dùng cùng working source đã so SHA-256 và fixture mới. VM giỏ ghi **0** POST fallback khi mất phản hồi. Bằng chứng mô phỏng được giữ riêng với các lượt HTTP. [VM](../qa/BIZ-UX-03/cart-vm-regression.json)
- Lượt chạy đầu của harness dừng ở parser Windows PowerShell do encoding file. Đã lưu script dưới UTF-8 BOM và chạy lại thành công. Đường dẫn manual dùng cùng bootstrap, ứng dụng và tài khoản fixture với HTTP. Phiên manual tương tác chưa được mở.
- Trước release, hoàn tất manual QA ở các viewport yêu cầu và zoom 200%, ưu tiên checkout/COD, Home đến footer, cho phép đọc lại giỏ và phản hồi lỗi SKU. Ghi lại FAIL có ảnh/route/thao tác và kiểm lại sau sửa. Kiểm OTP/reset hoặc Ollama đầu cuối cần fixture provider riêng. [Checklist](../qa/BIZ-UX-03/manual-visual-qa.md) · [Mẫu kết quả](../qa/BIZ-UX-03/mau-ket-qua.md)

[Checkpoint và lệnh chạy lại](BIZ-UX-03-tiep-tuc.md)
