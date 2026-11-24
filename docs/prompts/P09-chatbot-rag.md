> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P09 — Spring AI + RAG catalog và FAQ, chỉ tư vấn

**Chủ trì:** D


- AI là điểm nhấn có **timebox demo 24–29/11** và owner D; chuẩn bị dữ liệu FAQ song song từ 19/11, không làm block COD nếu hạ tầng AI lỗi. Boot **3.5.x** tương thích Spring AI **1.1.x**; đọc document đúng phiên bản trước khi thêm dependency. Không chuyển Boot 4/AI 2 tùy tiện.
- Mặc định `AI_ENABLED=false`, `AI_PROVIDER=disabled`. Khi dev chọn Ollama cần `OLLAMA_BASE_URL=http://localhost:11434`, **`OLLAMA_CHAT_MODEL=qwen2.5:3b`**, **`OLLAMA_EMBED_MODEL=bge-m3`**; hướng dẫn `ollama pull qwen2.5:3b` và `ollama pull bge-m3`. Khi thiếu RAM/API: fallback FAQ thông thường có nhãn `không phải AI`. Không coi frontend public là Ollama server public miễn phí. Không giả định cloud free-tier chạy Ollama.
- Ingest `Product` metadata + `FaqArticle` có source/updated_at; chunking/embedding/vector store; nạp lại/index lại admin có auth. Dữ liệu chưa kiểm nguồn không được tuyên bố là thực tế.
- Retrieval chỉ dữ liệu công khai. Đọc giá/tồn hiện hành từ MariaDB khi gợi ý. Không làm medical diagnosis; nếu không có nguồn đáp "chưa đủ thông tin". Không đặt hàng hoặc ghi DB qua chatbot.
- Chat endpoint input ≤ 500 chars, rate limit/timeout, output escape XSS, CSRF nếu session/cookie API; không tiết lộ session/user data.
- Test mock provider, retrieval nguồn, hallucination cases, lỗi provider, malicious prompt. 
**Done:** minh họa chatbot có RAG thực sự, link nguồn SKU và no-context fallback; hoặc báo `BLOCKED_AI` và phân biệt FAQ search bình thường với RAG.

## Quy định thực hiện

Áp dụng [AGENTS.md](../../AGENTS.md) và chỉ làm trong phạm vi prompt này. Báo các trang hoặc API bị tác động, cách chạy thử và kết quả kiểm chứng.
