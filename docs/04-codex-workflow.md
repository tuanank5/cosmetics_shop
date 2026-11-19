# Quy trình Codex cho dự án BeautyShop

1. Mở workspace tại root `beautyshop`, đọc `AGENTS.md`, `docs/DECISIONS.md`, SRS/Business Rules/ERD trước khi sửa.
2. Chạy `java -version` (21), `mvn -version`, `mvn test` và `mvn spring-boot:run -Dspring-boot.run.profiles=dev` (sau khi cấu hình DB).
3. Làm **một task tại một thời điểm**, mỗi task ở `docs/prompts/P00..P11`, branch riêng, một PR, review chéo.
4. Khi thiếu DB/credential/API/dữ liệu xác minh, báo blocker và triển khai interface/adapter có thể test bằng mock; không hardcode secret, không ghi PASS giả.
5. Sửa trong module được giao; nếu bắt buộc đổi contract shared (`InventoryService`, `VoucherService`, order API), thông báo owner các module liên quan, cập nhật test và docs.
6. Sau mỗi task ghi rõ: file nào thay đổi, code compile/run gì, test PASS/FAIL, điểm chưa xác minh, quyết định mới ở docs/DECISIONS.md.
7. Không dùng `--dangerously-bypass-approvals-and-sandbox` hay tự ý bỏ qua bảo vệ; chạy lệnh kiểm tra có kiểm soát.

## Quy trình tổng hợp dùng với Codex

```text
Đọc AGENTS.md, docs/DECISIONS.md và các tài liệu nguồn của task.
Chỉ thực hiện task Pxx hiện tại, không sửa business rules đã khóa.
Dùng package-by-feature, ưu tiên test; hãy bắt đầu bằng kiểm tra trạng thái repo
và hiện code/files đã có. Làm xong chạy mvn test và mvn package nếu môi trường cho phép.
Trình bày changed files, lệnh thực hiện, kết quả kiểm chứng, blockers;
không công bố tích hợp MoMo/AI PASS nếu chưa xác minh thực tế.
```

## Tài nguyên máy & AI

Ollama local là tùy chọn phục vụ test. `AI_PROVIDER=disabled` mặc định; P09 thêm provider/config khi có model sẵn. Trang web deploy miễn phí không đảm bảo có GPU/RAM hoặc free API uptime. Không cố chạy model LLM nặng trong cùng JAR. Chỉ cho chatbot sử dụng catalog & FAQ đã xác minh nguồn, không dùng ảnh/đánh giá người dùng để suy đoán y tế.
