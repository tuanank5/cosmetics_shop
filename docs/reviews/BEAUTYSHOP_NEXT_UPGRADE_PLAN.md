# Kế hoạch đề xuất sau đối chiếu revision a9870e1

**10 phase, 19 Work Package.** BASE đi trước mọi triển khai. Ưu tiên COD trọn luồng. PAY-LIVE là gate có điều kiện cho MoMo. Các phase nhóm theo trách nhiệm và có thể gối nhau khi đã đủ đầu vào. Số phase không bắt các công việc độc lập chờ nhau. [Hiện trạng và đối chiếu](BEAUTYSHOP_CURRENT_STATE_REVIEW.md) · [Phạm vi, kiến trúc và 17 quyết định](BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) · [Sổ đề xuất](BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md)

| Phase | WP | Thứ tự và lý do |
|---|---|---|
| 0 | BASE | Chọn SHA ứng viên rồi build sạch và xác lập revision nghiệm thu. |
| 1 | AUTH, AUTH-LIVE | Hoàn thiện phần thiếu, nghiệm thu thật các kênh theo điều kiện môi trường. |
| 2 | AUDIT, ACCOUNT | Chốt hợp đồng event trước thao tác ghi và email khóa/mở. |
| 3 | VOUCHER, PAYMENT, PAY-LIVE | Chốt số tiền, sau đó nghiệm thu attempt/thu tiền và đối soát thật. |
| 4 | ORDER, RETURN, REFUND | ORDER mua lại độc lập kiểm định; REFUND cần phân biệt tiền với kho. |
| 5 | AI-OPS, AI-HISTORY | Kiểm guard/counter rồi mới lưu nội dung thuộc Customer. |
| 6 | NOTIFY, SHOP-12 | Có thể làm song song trên danh tính và nguồn nội dung đã duyệt. |
| 7 | PERF, QUALITY | Đo và xác minh trước sửa; cả hai có thể kết thúc bằng bằng chứng giữ nguyên. |
| 8 | BACKUP | Dùng lại công cụ, bao phủ schema thực tế tại revision cần bàn giao. |
| 9 | UI-UAT | Chuẩn bị ma trận từ BASE, ký từng luồng trên revision tích hợp. |

Xem [thứ tự 19 prompt, trạng thái BLOCKED và các nhánh song song](BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md). Việc soạn prompt chưa mở triển khai.

## Quan hệ phụ thuộc

| Loại | Điều kiện và việc có thể chạy độc lập |
|---|---|
| Kỹ thuật | BASE → mọi branch triển khai. AUTH → ACCOUNT, NOTIFY, AI-HISTORY theo maNguoiDung ổn định. AUDIT → sự kiện ACCOUNT, VOUCHER, RETURN, REFUND, NOTIFY, BACKUP và AI reindex. Có thể phát triển caller cùng hợp đồng trên fixture, chỉ tích hợp khi audit đạt. |
| Nghiệp vụ | Quyết định mã/ship → VOUCHER → nghiệm thu lại quote/COD/MoMo; PAYMENT → REFUND. Duyệt điều kiện kiểm → RETURN → nhập kho; RETURN chỉ là đầu vào hoàn đối với yêu cầu hàng trả. Kho theo SKU. Chính sách/FAQ cần nội dung duyệt. |
| Nghiệm thu | AUTH-LIVE cần callback/Redis/mail thật; PAY-LIVE cần merchant/HTTPS, giao dịch thử và số đầu kỳ. UI-UAT cần binary mới và role/fixture. Backup cần thử restore cùng revision/schema. Chưa có môi trường ghi BLOCKED_EXTERNAL riêng gate đó. |
| Chuẩn bị song song | Lập fixture, xin test users/merchant, soạn nội dung shop, ma trận UAT, thiết kế audit và bảng tiền khi owner duyệt BASE. Sau BASE có thể làm ORDER, thiết kế AI, nội dung SHOP, benchmark và runner backup độc lập trên dữ liệu riêng. |
| Chưa cần chờ | COD, AI mock, nội dung shop và kiểm định không đợi merchant, pin Maps, tệp 91 trang, Ollama thật hoặc FEFO. Lịch sử Customer cần AUTH và guard; callback ngoài là chứng cứ release auth, không là điều kiện viết kho lịch sử. |

## Hồ sơ nghiệm thu theo WP

Roadmap cũ ghi freeze 29/11 và demo 03/12. Chưa có ước lượng nhân lực hoặc lịch owner duyệt cho 19 WP. Giữ mốc demo theo baseline đã kiểm; owner chọn phạm vi release sau BASE. Bảng phase thể hiện thứ tự kỹ thuật, không cam kết toàn bộ nâng cấp kịp mốc cũ.

Mỗi checkpoint dưới đây là điều kiện đề nghị cho lượt triển khai sau. Backend tiếp tục là Spring MVC/service/repository, Thymeleaf SSR/PRG, CSRF/session và quyền sở hữu theo principal. Các thay đổi giao diện chỉ dùng fragment, pager/bảng/form/modal/nút/thông báo hiện có, focus/keyboard/zoom, pending/empty/error khi cần. Thay đổi DB theo migration Flyway mới trên schema trống và bản nâng từ V7, JPA validate; duy trì đọc dữ liệu cũ. Chưa thấy nhu cầu dependency, database, cache server hay framework mới. Thành phần hạ tầng bên ngoài chỉ đưa vào WP có yêu cầu riêng. Với WP chỉ kiểm thử, cột Unit/Integration chỉ rõ bộ test để kế thừa/đối chiếu. Các lệnh Git bên dưới là đề xuất cho đợt có phê duyệt.

### BASE · Revision và CI

13 WP có sổ cũ, QA-01 BLOCKED; HEAD hiện có CI đạt. pom, src, CI JDK21, fixture DB trống/legacy. Kiểm manifest và 9 xóa D41, dựng sạch, lập sổ từng skip. Rủi ro dùng nhầm artifact hoặc thiếu tài nguyên.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P0/Vừa · Owner chọn SHA |
| Ranh giới | Chỉ xác định bản nghiệm thu, không sửa nghiệp vụ. |
| Unit | Suite mặc định |
| Integration | MariaDB, Mongo, BSON, Flyway, G13 |
| Browser/E2E | smoke jar đúng SHA |
| Regression | G1–13, CSRF, role, owner, IPN. |
| Checkpoint nghiệm thu | Owner chốt SHA và ma trận test/package/CI/skip. QA-01 chờ quyết định riêng. |
| Branch đề xuất, cách chia commit và rollback | `qa/base-revision`. Manifest → môi trường → biên bản. Giữ bản đang chạy nếu chưa tái lập được. |

Trên checkout sạch của SHA được duyệt, ghi `mvn -version`, `mvn test`, `mvn -DskipTests package` và chạy JAR với profile dev trên DB riêng. Nếu chọn a9870e1, kế thừa run CI đã checkout và build đúng SHA; bổ sung môi trường/gate mà workflow mặc định chưa chạy. Đối chiếu sáu nhóm skip theo XML trong báo cáo hiện trạng. Dùng fixture MariaDB cho 67 ca, Mongo cho 9 ca kèm các điều kiện lồng khi cần, schema rỗng/legacy cho hai nhóm migration 3+1, fixture cũ/mới cho BSON 1, báo cáo demo P21 1 chỉ chạy khi có bộ dữ liệu phù hợp. Mỗi ca được map sang suite riêng hoặc lý do tiếp tục skip; không bật tất cả cờ trên DB đang sử dụng.

G13 lịch sử sau sửa HTML có 312/314 integration, 2 skip (BSON chạy riêng, P21 opt-in) và 8/8 tổ hợp RAG-HTML. Đối chiếu assertion D35 trên view mới D41, quyền Guest/Customer/Admin, owner 404, role/CSRF 403, IPN có chữ ký và là ngoại lệ CSRF duy nhất, upload, OTP/session bị thu hồi, gộp giỏ và bất biến giao dịch. Lập bảng G1–G13 với SHA, kết quả đã kế thừa/cần chạy lại, người xác nhận; chuyển gate phát hành cho owner sau khi có kết quả. [Báo cáo G13](QA-01-ket-qua.md) · [Ma trận fixture G13](../qa/QA-01/rag-fix-data/verification.txt)

### AUTH · Reset và liên kết danh tính

Có form, reset link, OTP, unlink và tăng phiên bản xác thực. MatKhau_Service, MatKhauMoi_Form, DanhTinhOAuth_Service, hai auth controller và view. V7 đủ theo rà soát. Chặn tái dùng mật khẩu, kiểm xung đột link. Rủi ro mất đường đăng nhập.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P0/Vừa · BASE, chốt TTL/gỡ nếu đổi |
| Ranh giới | Giữ V6/V7, SMTP và mô hình danh tính, không thêm role/lịch sử mật khẩu. |
| Unit | matches/mismatch/UTF-8/hash null |
| Integration | hai reset và link tranh chấp, rollback |
| Browser/E2E | reset, reauth, gỡ cuối, OAuth-only; sau POST /logout kiểm /account bị chặn trước khi bấm Google. Chỉ sửa lỗi ứng dụng có tái hiện |
| Regression | session version, CSRF, cùng ID và giỏ merge một lần. |
| Checkpoint nghiệm thu | Mật khẩu cũ bị từ chối ở cả hai đường, token hợp lệ chỉ đổi một lần, dữ liệu cùng owner. |
| Branch đề xuất, cách chia commit và rollback | `feat/auth-account-methods`. Quy tắc → form → regression. Lùi binary tương thích V7, giữ token đã tiêu. |

### AUTH-LIVE · Nghiệm thu OAuth, Redis và mail

Callback có fixture, mail/Redis chưa nghiệm thu đủ chuỗi. OAuth handlers, EmailVerification_Service, Email_Service, MatKhau_Service, Redis, Mailpit và SMTP. Rủi ro liên kết nhầm người hoặc kết luận từ kết nối TCP.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P0/Lớn · BASE, AUTH cho luồng đã sửa, test users và callback |
| Ranh giới | Không ghi credential, OTP hay callback URL chứa code vào chứng cứ. |
| Unit | Kế thừa proof/OTP |
| Integration | Redis thật TTL, sai/đúng/resend/replay/race, Mailpit nhận local |
| Browser/E2E | callback thật hai provider, consent/deny/trùng email, nhận link; đối chiếu logout và Google SSO với một/nhiều tài khoản và cửa sổ riêng tư |
| Regression | phiên cũ, khóa và gộp giỏ. |
| Checkpoint nghiệm thu | Biên bản theo provider/môi trường, đúng ID. Bên ngoài cần mailbox thử do owner kiểm soát, chỉ lưu kết quả đã che. |
| Branch đề xuất, cách chia commit và rollback | `qa/auth-live`. Fixture và kịch bản → biên bản đã che. Thu hồi grant/session thử. |

### AUDIT · Nhật ký Admin chỉ đọc

V7 ghi audit auth trong transaction. NhatKyQuanTri_Service/Repository, V7 mở rộng, controller.admin, ui/admin-table, NguonDieuHuong, app.js. Rủi ro lộ dữ liệu hoặc ghi SUCCESS cho việc rollback.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P0/Lớn · BASE, duyệt action/retention |
| Ranh giới | Chỉ thao tác Admin quan trọng và màn nhật ký mới, không ghi mọi request. |
| Unit | Allowlist snapshot, sort |
| Integration | commit, rollback, FAILED riêng, unique correlation, purge |
| Browser/E2E | tìm-lọc-sort-page→detail→Back đúng scroll/focus, hai tab/no-JS, dòng bị dọn |
| Regression | audit auth cũ, quyền chỉ Admin. |
| Checkpoint nghiệm thu | Actor, giờ, target, trước/sau, lý do/kết quả đúng; không có thao tác sửa; khôi phục trạng thái chỉ cho list mới. |
| Branch đề xuất, cách chia commit và rollback | `feat/admin-audit`. Schema additive → service → list/detail. Tắt ghi mới/ẩn màn, giữ chứng từ. |

### ACCOUNT · Khóa/mở có lý do và mail

Có khóa, kiểm active/version và danh sách Admin. NguoiDung_Service, AdminNguoiDung_Controller, TrangThaiTaiKhoan_Filter, Email_Service, admin/user. Metadata gửi theo event. Rủi ro mail lỗi làm đảo trạng thái hoặc gửi lặp.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P0/Vừa · AUTH, AUDIT, nội dung email |
| Ranh giới | Giữ thống kê khách và email đúng loại. |
| Unit | Lý do hai chiều, no-op |
| Integration | user + audit + tác vụ gửi, retry, hai Admin |
| Browser/E2E | khóa rồi mở với form/Google/GitHub, phiên cũ |
| Regression | cấm tự khóa và giữ thống kê. |
| Checkpoint nghiệm thu | Khóa có hiệu lực dù SMTP lỗi, xem được ai, khi nào, lý do và kết quả gửi. |
| Branch đề xuất, cách chia commit và rollback | `feat/account-status`. Transaction + audit → gửi sau commit → form. Dừng worker và đối soát thư treo. |

### VOUCHER · Phạm vi mã và giảm phí ship

Giảm toàn đơn một mã, Admin có lọc/đếm/pager. PhieuGiamGia_Service/Repository, DonHang_Service, ShippingFee_Policy, ThanhToanMomo_Service, checkout, admin/voucher, migration quan hệ/slot/quota. Rủi ro vượt lượt hoặc sai tổng.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P0/Rất lớn · BASE, AUDIT cho thao tác Admin, quyết định 1–3 |
| Ranh giới | Giữ tồn theo biến thể và D27 đến khi có duyệt, không Excel. |
| Unit | FIXED/%, trần, ngưỡng, phân bổ phần dư, phí 0 |
| Integration | hai đơn giành lượt cuối, rollback giữa hai slot, giữ/consume/release, legacy |
| Browser/E2E | Guest→merge→quote→đặt JS/no-JS, duyệt mã |
| Regression | COD, MoMo, hủy, BR21, snapshot, KPI. |
| Checkpoint nghiệm thu | Đối soát từng đồng và mỗi lượt trên đơn, kho/giữ và quota đúng cả khi đồng thời. |
| Branch đề xuất, cách chia commit và rollback | `feat/voucher-scope-shipping`. Schema/legacy → policy → khóa/checkout → Admin. Dừng nhận đơn hai mã, đối soát holds, giữ khả năng đọc đơn mới khi lùi. |

### PAYMENT · Lịch sử từng lần thanh toán

Có nhiều lần thử/đơn, chi tiết dùng trạng thái hiệu lực. ThanhToanMomo_Repository/Service, Momo_Facade, DonHang_Service, order/detail và customer-detail. Tái dùng lan_thanh_toan, thêm nullable metadata khi cần. Rủi ro lộ mã nội bộ hoặc đổi trạng thái hiệu lực.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P0/Lớn · BASE, VOUCHER trước nghiệm thu tổng |
| Ranh giới | Tái dùng bảng attempt, không làm quote có TTL. |
| Unit | Map trạng thái/legacy |
| Integration | retry song song, IPN lặp/trễ, mismatch, query, pending reuse |
| Browser/E2E | hai vai trò xem số tiền/giờ/trạng thái đúng, chặn truy cập chéo |
| Regression | COD thu khi giao, hai slot mã và tồn chỉ chốt một lần. |
| Checkpoint nghiệm thu | Attempt hiển thị khớp DB, lỗi đã lọc, thiếu mốc hiển thị chưa ghi nhận. |
| Branch đề xuất, cách chia commit và rollback | `feat/payment-attempt-history`. Projection/metadata → view → regression. Ẩn phần trình bày, giữ mọi ref/attempt. |

### PAY-LIVE · Nghiệm thu MoMo Sandbox

Đã kiểm MoMo mock, thiếu bằng chứng Sandbox. Gateway, IPN, reconciliation job, deployment/04 và SQL fixture. Rủi ro callback đến muộn, lẫn mock với merchant.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P1/Lớn · PAYMENT, VOUCHER, merchant Sandbox, HTTPS |
| Ranh giới | Sandbox riêng, không merchant production hoặc FEFO. |
| Unit | Kế thừa chữ ký/validator |
| Integration | mock lỗi khó kích hoạt + đối sổ DB |
| Browser/E2E | E2E Sandbox success/fail/cancel/replay/late/query, đối chiếu merchant |
| Regression | tồn SKU và hai slot trước/sau. |
| Checkpoint nghiệm thu | Mỗi ca có đơn, attempt, amount VND, kết quả cổng và số dư; thiếu môi trường thì BLOCKED_EXTERNAL riêng MoMo. |
| Branch đề xuất, cách chia commit và rollback | `qa/payment-sandbox`. Đầu kỳ → kịch bản → đối soát. Xử lý xong lệnh đang chờ trước khi tắt endpoint. |

### ORDER · Danh sách và mua lại vào giỏ

Admin đủ tìm/lọc/ngày/sort; Customer có pager; chưa có mua lại. DonHang_Controller/Repository, QuyenDonHang_Service, GioHang_Service, order/list/customer-detail. Giữ pager; thêm control thiếu của Customer và POST mua lại. Rủi ro mua nhầm giá/xuyên chủ.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P1/Vừa · BASE, AUTH, VOUCHER/PAYMENT ở điểm giao checkout/detail |
| Ranh giới | Giữ số mục/trang và layout đã nâng cấp, mua lại không checkout. |
| Unit | SKU còn hợp lệ, lượng có thể thêm |
| Integration | khóa giỏ/SKU, hai POST, thay giá và ngừng hãng/danh mục |
| Browser/E2E | Back đúng dòng, mua lại đơn hỗn hợp và thấy lý do |
| Regression | snapshot, merge và timeline. |
| Checkpoint nghiệm thu | Đơn cũ, tiền và attempt không đổi; giỏ dùng giá/khả dụng hiện tại, báo theo dòng. |
| Branch đề xuất, cách chia commit và rollback | `feat/order-reorder`. Bộ lọc → service giỏ → chi tiết. Ẩn hành động mới, giữ dữ liệu giỏ hợp lệ. |

### RETURN · Kiểm định và nhập kho SKU

APPROVED và RECEIVED riêng, chưa kiểm định từng dòng. SauMua_Service, DoiTra_Repository, ReturnStatus, TonKho_Service, return/detail, migration kiểm định theo dòng + unique bút toán. Rủi ro nhập hàng hỏng/hai lần.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P0/Lớn · BASE, AUDIT, duyệt tiêu chuẩn kiểm định |
| Ranh giới | Không lô/FEFO, không tự hoàn tiền. |
| Unit | Số đủ bán ≤ thực nhận ≤ duyệt, transitions |
| Integration | hai Admin, rollback giữa nhập/audit, đọc hồ sơ cũ |
| Browser/E2E | duyệt→nhận→kiểm định, thiếu/hỏng |
| Regression | 7 ngày, lượng cộng dồn, BR21, owner. |
| Checkpoint nghiệm thu | Đúng người/giờ/kết luận; chỉ tăng đúng lượng đủ điều kiện một lần sau xác nhận. |
| Branch đề xuất, cách chia commit và rollback | `feat/return-inspection`. Schema/kết quả → transaction → UI. Dừng nhập mới, đối ứng sai lệch đã xác nhận, không xóa lịch sử. |

### REFUND · Hoàn toàn phần theo giao dịch

COD có thu tiền, MoMo có REFUND_PENDING. Sổ hoàn gắn attempt, Momo_Facade, SauMua_Service, ThongKe_Repository và order/return detail. Rủi ro chi hai lần hoặc hoàn cả đơn từ một mục.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P0/Rất lớn · PAYMENT, AUDIT, RETURN cho hàng trả, duyệt COD/trả một phần |
| Ranh giới | Chỉ hoàn toàn phần, chỉ ghi thành công khi có chứng cứ. |
| Unit | Số thực thu, điều kiện bao phủ |
| Integration | unique, retry/query sau timeout, hai lệnh cùng lúc, COD chưa thu và MoMo muộn |
| Browser/E2E | Admin xác nhận có chứng từ, Customer xem kết quả; Sandbox khi được cấp |
| Regression | doanh thu, kho, lượt không đảo hai lần. |
| Checkpoint nghiệm thu | Tối đa số thực trả, ref/actor/thời điểm và bằng chứng cuối; pending khi chưa rõ. |
| Branch đề xuất, cách chia commit và rollback | `feat/full-refund`. Ledger/policy → provider hoặc thủ công/đối soát → UI/KPI. Dừng lệnh mới, query lệnh treo, không rollback tiền bằng xóa DB. |

### AI-OPS · Bảo vệ và thống kê /chat

Có FAQ/RAG, giới hạn và reindex, thiếu thống kê người dùng/provider. TuVan_Controller/Form, TuVan_Service, KhoTriThuc_Service, RagProvider, OllamaRag_Config, AdminTuVan_Controller và admin/chat. Chỉ guard/đo/kiểm. Rủi ro PII vào provider hoặc metric.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P1/Vừa · BASE, AUDIT cho reindex |
| Ranh giới | Giữ /chat và thuật toán, chỉ mock provider. |
| Unit | Guard/parse/timeout và count |
| Integration | mock HTTP, lỗi, worker bận, embedding query/index khác chat |
| Browser/E2E | FAQ tắt AI, Admin reindex, trạng thái cuối |
| Regression | D35/D41, không phản chiếu câu từ chối. |
| Checkpoint nghiệm thu | Từ chối dùng 0 provider call, counters phân biệt request/call, kết quả văn bản và fallback cũ. |
| Branch đề xuất, cách chia commit và rollback | `feat/ai-operations`. Guard/test → counter → Admin. Gỡ thay đổi quan sát, giữ FAQ/RAG hiện hành. |

### AI-HISTORY · Lịch sử riêng của Customer

Trả lời hiện tại render theo request. D48 chốt MongoDB cho cuộc và tin riêng, kiểm maNguoiDung từ phiên MariaDB trên mọi thao tác. Thành phần ảnh hưởng gồm TuVan_Controller/Service, document/repository/DTO, index Mongo, view chat và manifest backup. Rủi ro đọc chéo, lưu dở hoặc gửi nội dung riêng tới model.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P1/Lớn · AUTH, guard AI-OPS, duyệt lưu/xóa |
| Ranh giới | Quyền xem của Customer, không cấp Admin đọc nội dung hay tự gửi ngữ cảnh. |
| Unit | Policy lưu/xóa |
| Integration | Mongo fixture, hai chủ, đọc/ghi/xóa theo owner, retry sau ghi dở, retention đã duyệt, Mongo lỗi và Guest không lưu |
| Browser/E2E | tạo cuộc, tải thêm, Back, empty/error |
| Regression | Guest chat và số call không đổi khi xem lại. |
| Checkpoint nghiệm thu | Chỉ chủ đọc được lịch sử riêng, câu bị từ chối không lưu, Admin chỉ có số liệu tổng. |
| Branch đề xuất, cách chia commit và rollback | `feat/customer-chat-history`. Schema/quyền → lưu → list/detail. Dừng ghi/ẩn màn, giữ quy trình dọn đúng hạn. |

### NOTIFY · Thông báo khuyến mãi trong web

Chưa có campaign/inbox/readAt. Controller/service mới, header/badge, Admin preview, inbox Customer, campaign và recipient/readAt. Rủi ro fan-out trùng hoặc nhầm tập người nhận.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P1/Vừa · AUTH, AUDIT, duyệt tập người nhận |
| Ranh giới | Chỉ gửi chủ động trong website, không lịch sử gửi tổng quát. |
| Unit | Chọn người, URL nội bộ, read idempotent |
| Integration | repeat send, batch retry, read đồng thời, IDOR |
| Browser/E2E | preview→xác nhận→đọc, hai tab/no-JS |
| Regression | tạo/sửa mã không gửi gì, chỉ Admin được gửi. |
| Checkpoint nghiệm thu | Unique theo người nhận/chiến dịch, số chưa đọc khớp DB và có audit. |
| Branch đề xuất, cách chia commit và rollback | `feat/promotion-inbox`. Schema/recipient → gửi/audit → inbox. Dừng gửi mới, giữ bản đã phát. |

### SHOP-12 · Tám trang thông tin và FAQ

Có Footer/FAQ nguồn cho chatbot, thiếu các trang đích. Nối link Footer, SSR public, TriThuc_Repository/KhoTriThuc_Service, bai_viet_faq; migration mới nếu cần nhóm. Rủi ro hai nguồn chính sách hoặc link sai.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P1/Vừa · BASE, văn bản owner duyệt; phối hợp reindex |
| Ranh giới | Giữ Footer/Maps/SVG, không viết thay chính sách đã duyệt. |
| Unit | Nhóm/tìm/URL |
| Integration | bài tắt và revision nguồn, reindex mock, V5 legacy |
| Browser/E2E | từng đích Footer và FAQ, mobile/keyboard |
| Regression | /chat và UI-COPY-01, giữ logo. |
| Checkpoint nghiệm thu | Tám trang có phiên bản owner duyệt, FAQ dùng một nguồn, không xuất bản contact giả. |
| Branch đề xuất, cách chia commit và rollback | `feat/shop-info-faq`. Nội dung duyệt → query → trang/link. Rút bài sai, phục hồi nội dung và reindex. |

### PERF · Đo ảnh, SQL và cache có điều kiện

Có batch, DTO, eager/lazy và thumbnails; thiếu benchmark lớn. AnhBienTheSanPham_Controller, thẻ/Home/PDP, repo catalog, giỏ, Admin/attempt/audit/chat, Redis. Chỉ index/cache/header ở điểm có số đo. Rủi ro stale hoặc làm chậm ghi.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P2/Vừa · BASE, bộ dữ liệu đại diện; đo module mới khi đã có |
| Ranh giới | Giữ pipeline ảnh, không Cloudinary/CDN/dọn ảnh/tìm gần đúng. |
| Unit | Key/TTL và ưu tiên ảnh nếu đổi |
| Integration | EXPLAIN, SQL count/bytes/p95, cold/warm, Redis lỗi/rollback |
| Browser/E2E | waterfall, LCP/CLS/304/ảnh mới |
| Regression | giá, tồn, quyền, thứ tự/count và fallback. |
| Checkpoint nghiệm thu | Bảng trước/sau cùng fixture. Kết quả đo có thể đủ để kết thúc mà không sửa code. |
| Branch đề xuất, cách chia commit và rollback | `perf/measured-hotspots`. Baseline → từng thay đổi có số đo → đo lại. Bypass cache, trả header cũ, đánh giá bỏ index qua migration mới. |

### QUALITY · Rà mã thừa có chứng cứ

Các tệp cũ D41 đã xóa có chủ đích. Controller/service/repo/DTO, template/CSS/JS, mapping, comment. Rủi ro xóa nhầm bean/selector chỉ dùng runtime.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P2/Nhỏ–vừa · BASE, module đã ổn định |
| Ranh giới | Chỉ xử lý có căn cứ, không phục hồi thành phần D41. |
| Unit | Test caller/quy tắc liên quan |
| Integration | boot context, JPQL, render template |
| Browser/E2E | mở đúng role và trạng thái thực dùng |
| Regression | so contract, build/package và kiểm nghiệp vụ vùng sửa. |
| Checkpoint nghiệm thu | Mỗi ứng viên có bằng chứng bỏ/giữ; không có finding thì kết thúc ở kiểm kê. |
| Branch đề xuất, cách chia commit và rollback | `chore/verified-cleanup`. Mỗi finding và test một commit. Lùi riêng thay đổi nhỏ bị lỗi. |

### BACKUP · Admin quản lý bản sao

Fixture đã phục hồi SQL/BSON/ảnh, chưa có Admin quản lý. Runner cho công cụ dump cố định, manifest/job, controller.admin và view, SQL/BSON/uploads. Học từ run-data.ps1, không chạy harness QA từ web. Rủi ro lệch mốc, partial, PII trong archive.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P1/Lớn · AUDIT, schema/store cần bảo vệ, công cụ/quota/quyền đã cấp |
| Ranh giới | Chỉ tạo/quản lý/tải bản sao theo quyền, restore vận hành riêng. |
| Unit | Job ID, manifest, state |
| Integration | disk đầy, thiếu một store, hash sai, restart, restore đích mới |
| Browser/E2E | tạo-list-verify-tải có quyền, trạng thái và lỗi |
| Regression | Tổng/checksum SQL, BSON và index đánh giá/hội thoại nếu đã triển khai, liên kết maNguoiDung và ảnh. |
| Checkpoint nghiệm thu | Bản READY phục hồi được theo runbook, audit truy được người tạo/tải. Restore web chờ quyết định riêng. |
| Branch đề xuất, cách chia commit và rollback | `feat/admin-backups`. Runner/manifest → quyền/UI → diễn tập. Dừng job, giữ archive tốt, chuyển/lùi đích khi đã đối soát. |

### UI-UAT · Nghiệm thu giao diện và bàn giao

Đã có bằng chứng theo màn, thiếu zoom native và ma trận tổng. Design system, fragments/ui, app.js, CSS, docs/testing và QA. Rủi ro coi screenshot từng màn là UAT toàn bộ.

| Hạng mục | Nội dung |
|---|---|
| Ưu tiên, độ phức tạp và đầu vào | P1/Lớn · BASE, từng WP theo màn; pin tùy nội dung |
| Ranh giới | Chỉ kiểm chứng, không redesign hoặc làm lại scroll toàn hệ thống. |
| Unit | Kế thừa rule/DTO liên quan |
| Integration | hai POST khi có 9 địa chỉ, snapshot sau sửa/xóa, CSRF PDP session mới |
| Browser/E2E | ma trận dưới, Back giỏ và Audit Log |
| Regression | breadcrumb, footer, logo, focus và đặc quyền. |
| Checkpoint nghiệm thu | Có kết quả theo tổ hợp, bug có bước tái hiện, retest trên SHA. Biên bản bàn giao tách báo cáo 91 trang. |
| Branch đề xuất, cách chia commit và rollback | `qa/ui-uat`. Ma trận → bằng chứng → retest. Giữ phát hành phần lỗi đến khi xác nhận lại. |

## Ma trận UI-UAT và bằng chứng bàn giao

| Vai trò / đường đi | Ma trận bắt buộc theo màn | Quan sát đạt |
|---|---|---|
| Guest, catalog→PDP→cart→login | Session mới có CSRF, thêm/hết hàng, Back/Forward và giỏ từ BFCache, JS-off, đăng nhập/gộp | Token thiếu bị chặn, đúng SKU và số lượng sau đọc lại server, không gộp hai lần. |
| Customer, checkout→địa chỉ→checkout→đơn | Hai request tạo tại 9 địa chỉ; sửa/xóa địa chỉ đã chốt; giá thay và lỗi form | Tối đa 10 và một mặc định khi còn địa chỉ, giữ đường về, snapshot đơn trước đó nguyên vẹn. |
| Customer, đơn/đổi trả/wishlist/chat/inbox | Rỗng, tên dài, trang cuối, lỗi, keyboard, hai tài khoản và hai tab | Pager/count/đường về đúng, chặn truy cập chéo, dữ liệu và focus hợp lý khi dòng mất. |
| Admin, catalog/SKU/user/mã/đơn/đổi trả/audit/backup/AI | Tìm/lọc/sort ở trang cuối, mở chi tiết→Back/Forward, thay đổi làm mất hàng, form lỗi/CSRF và lỗi dịch vụ | Filter, keyword, sort, page, scroll, focus đúng; khi dòng mất về vùng kết quả. Không hiển thị “thành công” khi server lỗi. |
| Guest/Customer/Admin theo màn áp dụng | Desktop 1366+, tablet 768, mobile 390 và điểm nhỏ 320 khi cần; tab/Shift-Tab/Enter, zoom trình duyệt **native 200%**, nội dung dài/rỗng/lỗi, tắt JS khi có form cơ bản | Không che hành động, không cuộn ngang toàn trang, focus thấy được, nút/spacing/notice đúng design system. Lưu viewport và cách đặt zoom, phân biệt reflow mô phỏng. |
| Gaps lịch sử | Bổ sung đối chiếu 1672×941 theo UI-REDESIGN-02, kịch bản owner thủ công còn mở, Maps chỉ khi có pin; badge/ảnh/loading mới trên mạng chậm | Ghi rõ không chạy/chặn, chỉ mở bản sửa khi có bước tái hiện hoặc số đo. Báo cáo 91 trang theo đầu việc bàn giao riêng. |

## Git, phân loại kiểm thử và điều kiện phát hành

1. Owner duyệt SHA qua BASE; tạo branch từ baseline đã xác nhận sau khi được giao WP. Ghi SHA nguồn, môi trường/phiên bản, dữ liệu đầu kỳ, cờ fixture và kết quả mong đợi. Không ghi secret, OTP, PII, mã truy cập từ callback hay payload gateway thô.
2. Unit cho quy tắc, Integration cho transaction/khóa và dữ liệu thật trên DB thử, mock cho provider có kiểm số lần gọi. Browser tự động xác minh UX; E2E dịch vụ thật phải có callback/mailbox/merchant; manual UAT ghi người duyệt và cách thao tác. Ghi riêng PASS/FAIL/SKIP với lý do, NOT RUN, BLOCKED_EXTERNAL và bằng chứng kế thừa. Không cộng lượt chạy khác fixture.
3. Sau thay mã chạy Maven test/package đúng JDK21 và nhóm integration liên quan, MariaDB dev cô lập nếu có thay đổi dữ liệu. Không lặp mọi E2E sau sửa nhỏ; khi chạm hợp đồng chung kiểm các consumer trong bảng phụ thuộc. Gate phát hành gồm G13/security, hồi quy giao dịch/owner và CI đúng revision. Bug ghi bước tái hiện, kỳ vọng/thực tế, bản sửa và retest.
4. Rà Git diff và tệp nhạy cảm, commit theo trách nhiệm trong bảng, push branch, PR nêu tác động owner/module và quyết định mới, CI, review rồi merge sau duyệt. Ghi progress, SHA sau merge, liên kết PR/run, log đã che ở docs/qa/<WP>/, báo cáo docs/reviews/ và ảnh chọn lọc ở docs/ui/screenshots/<WP>/. Rollback giao dịch có tiền/dữ liệu phải đối soát trước, không xóa chứng từ.

## Phương án 4 phase ban đầu, lưu để tra lịch sử

Bảng dưới đây ghi lại kế hoạch **đã được thay thế**. AI-LIVE, LOT-DESIGN/DATA/SELL và RELEASE theo lô chỉ còn ở backlog.

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
