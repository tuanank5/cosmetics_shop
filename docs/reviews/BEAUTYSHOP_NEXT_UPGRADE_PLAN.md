# Kế hoạch tiếp tục từ working tree 27/11/2026

Đề xuất 4 phase, 9 work package. Đầu vào là [hiện trạng](BEAUTYSHOP_CURRENT_STATE_REVIEW.md), [phân tích ảnh hưởng](BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md), D26/D40–42 và thiết kế FEFO đã có. Ưu tiên khóa được bản sẽ nghiệm thu trước khi chạy cả ma trận. Mỗi dòng là phạm vi riêng để owner duyệt.

| Phase · WP | Hiện trạng và vấn đề | Mục tiêu, phạm vi và phương án | Ảnh hưởng, rủi ro | Ưu tiên · phụ thuộc · độ phức tạp |
|---|---|---|---|---|
| 1 · BASE | Diff nhiều tầng, có tệp nguồn chưa tracked. CI trước chỉ đại diện commit cũ. | Lập manifest, rà file xóa/tệp mới, chuẩn hóa profile JDK21, build từ checkout sạch, xin phép tích hợp revision rồi gắn log vào SHA. | Thiếu DTO/view gây lỗi build/runtime; đưa nhầm cấu hình riêng vào Git. | P0 · độc lập · trung bình |
| 2 · AUTH-LIVE | OAuth dùng fixture, OTP thật chưa nghiệm thu. | Tạo test users, chạy Redis + Mailpit local rồi SMTP/hộp thư thật, Google và GitHub đúng callback. | Liên kết nhầm danh tính, gửi thư sai, giữ PII trong log. | P0 · BASE · lớn |
| 2 · PAY-LIVE | COD và MoMo mock đã kiểm. Thiếu bằng chứng Sandbox đối soát thật. | Staging độc lập, HTTPS callback, giao dịch sandbox, so số tiền/giữ voucher và tồn trước-sau. | IPN đến muộn/lặp, hoàn hoặc xuất hai lần. | P0 · BASE · lớn |
| 2 · AI-LIVE | /chat đã thay route cũ. RAG chỉ có bằng chứng mock. | Nạp corpus fixture, dùng hai model thật, thử FAQ, timeout và từ chối PII trước embedding/chat. | Lộ dữ liệu, giá/tồn cũ, thời gian phản hồi. | P1 · BASE · trung bình |
| 2 · UI-UAT | Chrome theo màn đã đạt, còn thiếu zoom native, pin Maps, ma trận toàn hệ thống và đối chiếu tài liệu nộp. | Dùng ma trận Guest/Customer/Admin và route theo vai trò. Kiểm bàn phím, bộ lọc, trang cuối, khôi phục vị trí, form lỗi, ảnh, toolbar. Owner so pin và báo cáo 91 trang với bản chạy. | Có thể xác nhận nhầm mock hoặc ảnh cũ; sửa rộng gây hồi quy. Chỉ mở bug có bước tái hiện. | P1 · BASE; chuẩn bị checklist độc lập · lớn |
| 3 · LOT-DESIGN | Đã có bản thiết kế; chưa chốt HSD, legacy, hàng trả và đối soát trễ. | Owner kho, vận hành và thanh toán duyệt bất biến, sơ đồ trạng thái, thứ tự khóa, mapping dữ liệu và tiêu chí dừng chuyển SKU. | Quyết định sai có thể làm tăng tồn bán được khi thiếu hàng thật. | P0 trước ghi kho · dùng baseline và kết quả PAY-LIVE để chốt nhánh MoMo · lớn |
| 3 · LOT-DATA | Chưa có bảng lô hoặc kiểm kê mở đầu. | Sau duyệt, thêm schema kiểu mở rộng, FK/index/check, nhật ký nhập/điều chỉnh, kiểm kê và báo cáo chênh lệch trên bản sao. | Sai checksum V5, gán lô giả cho đơn cũ, bỏ sót giữ hàng. | P1 · LOT-DESIGN + BASE · lớn |
| 4 · LOT-SELL | Tồn hiện theo biến thể. | Phân bổ FEFO trong transaction COD/MoMo; hủy, release, hoàn đúng lô, đổi trả qua kiểm định; thống nhất số bán được cho mọi consumer. | Bán quá tồn, deadlock, replay, báo cáo lệch, BR21 mất hiệu lực. | P1 · LOT-DATA + PAY-LIVE · rất lớn |
| 4 · RELEASE | Chưa có rehearsal cho bản theo lô. | Chạy staging đủ dữ liệu, thử backup/restore, bật theo nhóm SKU, giám sát chênh lệch, diễn tập dừng ghi và đối soát. | Rollback mất truy vết hoặc để reservation mồ côi. | P1 · LOT-SELL và các gate Phase 2 đã đạt hoặc owner xử lý ngoại lệ · lớn |

### Nghiệm thu, kiểm thử và Git đề xuất

| WP | Điều kiện đạt và phép kiểm | Branch đề xuất và cách chia commit |
|---|---|---|
| BASE | Source manifest đủ; Maven test/package, Flyway validate + restore fixture độc lập, G13 vai trò/CSRF/owner, CI trên đúng SHA. Kiểm nguyên nhân từng skip. | review/current-baseline; commit riêng theo nhóm chức năng và bằng chứng |
| AUTH-LIVE | Hai provider trả đúng tài khoản qua callback thật, từ chối hoạt động đúng; mã hết hạn/dùng lại bị từ chối, đăng ký hoàn tất đúng một lần. Unit/mock + integration Redis/Mailpit + E2E hộp thư thử và manual consent. | qa/auth-live; tách fixture, lỗi có tái hiện và biên bản UAT |
| PAY-LIVE | Integration chữ ký, tiền, COD, IPN lặp; Sandbox E2E thành công/hủy/trễ, đối sổ đơn, payment, tồn và voucher; manual đối chiếu merchant. | qa/payment-sandbox; tách contract, kịch bản và bằng chứng khử định danh |
| AI-LIVE | Unit guard + mock counters, integration model thật, E2E /chat hỏi giá/tồn, chặn PII, lỗi mạng và thủ công đánh giá dẫn nguồn. | qa/rag-live; tách corpus thử và biên bản model |
| UI-UAT | Mỗi route có role, kích thước, empty/error/data, bàn phím, zoom thật 200%, kết quả và ảnh chọn lọc. E2E trang 2, hai tab, hết dòng, checkout sửa địa chỉ; integration 9→10 đồng thời, snapshot đơn; manual Maps, screen reader và kiểm tệp nộp. | qa/system-uat; checklist, từng lỗi đã chứng minh và retest riêng |
| LOT-DESIGN | Bảng quyết định và ví dụ tính tay nhiều lô, thứ tự khi trùng HSD, case COD/MoMo/hàng trả; workshop duyệt với owner và thiết kế unit/integration trước migration. | design/lot-contract; tách domain, migration và runbook |
| LOT-DATA | Integration MariaDB từ trống và V7 có đơn cũ, dữ liệu SQL/Mongo/ảnh phục hồi được, tổng nhập/giữ/cách ly khớp, rollback diễn tập; manual kiểm kê và bảng lô theo SKU. | feat/lot-foundation; schema, repository/service, UI nhập, kiểm thử từng commit |
| LOT-SELL | Unit chính sách FEFO, integration checkout đồng thời và rollback phần thiếu, E2E COD/hủy/MoMo/lặp callback/đổi trả. Manual truy vết từ đơn tới lô, xác nhận báo cáo và chặn ngừng bán. | feat/lot-fulfillment; tách phân bổ, giao dịch COD, MoMo, hậu mãi, read model |
| RELEASE | Regression + E2E staging, manual đối sổ độc lập, số lệch = 0 và thử khôi phục thành công; owner duyệt trước chuyển dữ liệu thực. | release/lot-pilot; tag bản nghiệm thu và commit runbook theo revision |

AUTH-LIVE, PAY-LIVE, AI-LIVE và UI-UAT có thể chạy song song trên fixture/tài khoản riêng sau BASE. LOT-DESIGN có thể thảo luận ngay. OAuth, Maps và báo cáo 91 trang không phải phụ thuộc kỹ thuật của thiết kế lô. Gate cứng cho ghi kho là baseline giao dịch, quyết định nghiệp vụ, kiểm kê, khả năng đối soát/khôi phục và kiểm thanh toán. Chiến lược Git trong bảng là đề xuất cho lượt được duyệt.
