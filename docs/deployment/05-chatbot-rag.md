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

- FAQ lấy từ `bai_viet_faq` đang hoạt động, có URL nội bộ đã cho phép. Với nguồn ngoài, cấu hình `app.ai.trusted-faq-urls` bằng danh sách URL HTTPS chính xác, phân cách bằng dấu phẩy, sau khi đã duyệt nội dung. Mặc định danh sách ngoài rỗng. Link có userinfo, query hoặc fragment bị loại. Admin kiểm tra nội dung, URL và `ngay_cap_nhat` trước khi nạp. Bộ lọc kiểm cả tiêu đề và nội dung trước khi tìm FAQ hoặc chia đoạn để gửi embedding.
- Nguồn sản phẩm gồm tên, thương hiệu và danh mục từ catalog công khai. Các mô tả và thành phần chưa có nguồn kiểm chứng bị loại khỏi chỉ mục. Khi nhắc sản phẩm, ứng dụng đọc lại giá, tồn và URL của SKU trực tiếp từ MariaDB.
- Spring AI 1.1.8 tạo embedding và tìm đoạn tương tự bằng `SimpleVectorStore`. Ứng dụng kiểm tra nguồn còn công khai và khớp chủ đề. Model chọn một đoạn nguồn có thể trả lời. Ứng dụng hiển thị đoạn và liên kết đã duyệt. Kết quả ngoài danh sách nguồn hoặc thiếu ngữ cảnh trả “Chưa đủ thông tin”.
- `POST /chat` dùng CSRF, tối đa 500 ký tự, 6 câu/phút mỗi phiên và 30 câu/phút mỗi tiến trình. Thời gian chờ AI cho một câu tối đa 12 giây. Ứng dụng escape đầu ra, chặn mẫu dữ liệu cá nhân phổ biến và các yêu cầu chẩn đoán trước lúc gọi model. Model không có quyền truy cập phiên, đặt đơn, ghi DB hoặc gọi công cụ.
- `POST /admin/ai/reindex` chỉ Admin và yêu cầu CSRF. Tác vụ nạp tối đa 30 giây mỗi lần. Nếu lỗi, ứng dụng giữ chỉ mục trước đó và kiểm tra lại nguồn trước khi trích dẫn.
- Trên `/admin/ai`, thống kê tính từ khi tiến trình khởi động. Mỗi POST /chat kết thúc có một kết quả. Trả được FAQ sau lỗi AI vẫn là một phản hồi người dùng thành công. Từng lượt HTTP embedding truy vấn, embedding index và chat có kết quả và độ trễ riêng. Deadline 12/30 giây của worker và timeout HTTP có thể xảy ra ở hai thời điểm khác nhau. “Phản hồi HTTP gần nhất” chỉ cập nhật khi quan sát được phản hồi từ provider. Audit `AI_REINDEX` dùng target logic `AI_INDEX/1`, ghi số đoạn trước/sau và công bố chỉ mục sau commit.

[Spring AI 1.1.8](https://spring.io/blog/2026/06/12/spring-ai-1-1-8-1-0-9-avaialble-now/) · [Ollama Chat](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html) · [Ollama Embeddings](https://docs.spring.io/spring-ai/reference/api/embeddings/ollama-embeddings.html)
