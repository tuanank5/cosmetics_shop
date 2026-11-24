# P09 · Tư vấn catalog và FAQ

**Ollama thật: BLOCKED_AI.** Máy kiểm thử chưa có Ollama hoặc hai model đã tải. Code Spring AI, vector search và luồng suy luận đã được kiểm thử với một HTTP mock của Ollama trên localhost. Mock chỉ nằm trong test.

## Dùng FAQ khi AI tắt

Mở `/chat`. Mặc định `AI_ENABLED=false` và `AI_PROVIDER=disabled`. Trang tìm FAQ trên MariaDB và hiển thị nhãn **Tìm FAQ · không phải AI**. Ví dụ hỏi “Đổi trả trong bao lâu?”. V5 nạp FAQ chính sách BeautyShop kèm URL nguồn.

## Bật RAG trên máy có Ollama

```powershell
ollama pull qwen2.5:3b
ollama pull bge-m3
$env:AI_ENABLED = 'true'
$env:AI_PROVIDER = 'ollama'
$env:OLLAMA_BASE_URL = 'http://localhost:11434'
$env:OLLAMA_CHAT_MODEL = 'qwen2.5:3b'
$env:OLLAMA_EMBED_MODEL = 'bge-m3'
.\scripts\p00.ps1 -Task run
```

Khởi động Ollama trước khi nạp dữ liệu. Từ phiên Admin, mở `/admin/ai` và chọn **Nạp lại FAQ và catalog**. Khi số đoạn đã lập chỉ mục lớn hơn 0, thử câu hỏi có tên sản phẩm hoặc chủ đề FAQ, rồi một câu nằm ngoài nguồn. Chỉ mục nằm trong RAM. Nạp lại sau khi restart hoặc sau khi nguồn đã được kiểm tra và cập nhật. Khi thiếu tài nguyên hoặc provider lỗi, dùng FAQ. COD chạy độc lập với Ollama.

## Nguồn và cách kiểm chứng

- FAQ lấy từ `bai_viet_faq` đang hoạt động, có URL HTTPS hoặc URL nội bộ đã cho phép. Admin phụ trách kiểm tra nội dung, URL nguồn và `ngay_cap_nhat` trước khi nạp.
- Nguồn sản phẩm gồm tên, thương hiệu và danh mục từ catalog công khai. Các mô tả và thành phần chưa có nguồn kiểm chứng bị loại khỏi chỉ mục. Khi nhắc sản phẩm, ứng dụng đọc lại giá, tồn và URL của SKU trực tiếp từ MariaDB.
- Spring AI 1.1.8 tạo embedding và tìm đoạn tương tự bằng `SimpleVectorStore`. Ứng dụng kiểm tra nguồn còn công khai và khớp chủ đề. Model chọn một đoạn nguồn có thể trả lời. Ứng dụng hiển thị đoạn và liên kết đã duyệt. Kết quả ngoài danh sách nguồn hoặc thiếu ngữ cảnh trả “Chưa đủ thông tin”.
- `POST /chat` dùng CSRF, tối đa 500 ký tự, 6 câu/phút mỗi phiên và 30 câu/phút mỗi tiến trình. Thời gian chờ AI cho một câu tối đa 12 giây. Ứng dụng escape đầu ra, chặn mẫu dữ liệu cá nhân phổ biến và các yêu cầu chẩn đoán trước lúc gọi model. Model không có quyền truy cập phiên, đặt đơn, ghi DB hoặc gọi công cụ.
- `POST /admin/ai/reindex` chỉ Admin và yêu cầu CSRF. Tác vụ nạp tối đa 30 giây mỗi lần. Nếu lỗi, ứng dụng giữ chỉ mục trước đó và kiểm tra lại nguồn trước khi trích dẫn.

[Spring AI 1.1.8](https://spring.io/blog/2026/06/12/spring-ai-1-1-8-1-0-9-avaialble-now/) · [Ollama Chat](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html) · [Ollama Embeddings](https://docs.spring.io/spring-ai/reference/api/embeddings/ollama-embeddings.html)
