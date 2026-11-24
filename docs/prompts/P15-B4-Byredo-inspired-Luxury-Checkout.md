# P15 · B4 — Byredo-inspired Luxury Checkout (C27 / F07)

> **Vai trò:** Bạn là Codex đang làm việc trực tiếp trong repository BeautyShop (Spring Boot + Thymeleaf + JPA/MariaDB). **Hãy triển khai B4, kiểm thử và dừng.** Đây là một lát cắt giới hạn, không phải lệnh viết lại checkout hay đổi nghiệp vụ giao dịch.
>
> **Baseline đã được duyệt:** B2 = **23/30 PASS (76,67%), core 11/12**, `81 PASS / 0 FAIL / 0 ERROR / 34 SKIPPED` trên JDK 21. GSAP C19 đã được nghiệm thu kỹ thuật có giới hạn. **Mục tiêu có điều kiện:** C27 PARTIAL → PASS, tổng **24/30 = 80%, core 12/12** trong phạm vi đủ bằng chứng UI/MVC/fixture. **F01/MariaDB là cổng nghiệm thu riêng; không đánh đồng.**

## 0. Đọc nguồn thật và bảo toàn working tree (bắt buộc)

1. Đọc `AGENTS.md`, `README`, SRS, Business Rules, ERD, Decisions, Design System U1 và các tài liệu U2/U3–U8 liên quan checkout. Đọc **toàn bộ phần B2 mới nhất** trong:
   - `docs/reviews/byredo-inspired-ui-implementation.md`
   - `docs/reviews/byredo-component-gap-analysis.md`
   - `docs/reviews/frontend-audit-2026-11.md` (đặc biệt F07)
   - `docs/reviews/gsap-c19-pilot.md`
   - Các test checkout, voucher, order, security và cấu hình MoMo thực tế trong repository.
2. Kiểm tra `git status --porcelain=v1 -uall`, nhánh, HEAD, diff và lập inventory SHA-256 trước sửa. Source đã có nhiều thay đổi U1–U2/B1/B2/GSAP **chưa commit**: không reset, clean, stash, checkout/revert nguyên file hay ghi đè công việc khác. Đặt snapshot, log, bản đối chứng trong `target/p15-b4/`.
3. Truy vết **source thực tế** của GET `/checkout`, `POST /checkout/quote`, `POST /checkout`; DTO/model (`addressId`, `voucher`, `payment` đúng tên trong code), validation, Spring Security/CSRF, flash attributes, PRG, COD/MoMo, quote/tổng tiền `BigDecimal`. Đọc template `src/main/resources/templates/order/checkout.html`, controller/service và stylesheet liên quan. **Không suy đoán** đường chuyển trạng thái.
4. Chạy baseline test JDK 21 và chụp trạng thái Checkout trên fixture trước sửa. Nếu baseline lệch B2, ghi nhận và giải thích; không âm thầm cập nhật con số cũ thành test mới.

## 1. Mục tiêu và giới hạn

**Mục tiêu chính C27:** giao diện checkout sang trọng, dễ hiểu và **không hiển thị báo giá có vẻ hợp lệ khi thực tế đang lỗi/đã mất hiệu lực**. Xử lý lỗi F07: mã voucher vẫn hiện trong input nhưng giảm giá được render về `0` sau một lỗi đặt đơn/quote, có thể gây hiểu nhầm. Giữ nguyên tính đúng của địa chỉ, voucher, COD và cấu hình MoMo.

Phạm vi ưu tiên:
- Template checkout và CSS checkout tối thiểu dùng tokens U1.
- Test MockMvc/integration cô lập cho view state của quote/voucher/submit.
- **Chỉ nếu thật sự cần** được sửa ở controller **phần dựng model/view-state, thông điệp lỗi hoặc PRG** để bảo đảm UI hiển thị đúng; giải thích diff. **Không thay đổi** quy tắc tính tiền, giảm giá, kho, lưu đơn, idempotency, giao dịch, phương thức thanh toán, callback/IPN, CSRF, security, schema, Flyway, entity/repository hay API contract.
- Giữ các POST và tên tham số contract hiện hữu. Không thêm AJAX/SPA/framework, không thêm dependency.
- GSAP chỉ ở Home C19, không tải vào Checkout.

**STOP gate:** Nếu sửa F07 hoặc đạt C27 PASS không thể thực hiện mà không đổi nghiệp vụ/service tính giá/đặt đơn hoặc contract backend, DỪNG tại phát hiện đó; xuất đề xuất sửa tối thiểu + test chứng minh cần thiết, **không tự sửa rộng**. Không sửa dữ liệu thật để làm test pass.

## 2. B4.1 — State và lỗi báo giá F07 (ưu tiên cao nhất)

Phân tích chính xác nhánh GET/quote/submit thành công và thất bại; phân biệt ít nhất:
- Chưa có báo giá xác nhận / giỏ hoặc địa chỉ chưa hợp lệ.
- Báo giá được server tính thành công, với/không voucher; voucher có hoặc không giảm giá thật.
- Voucher sai, hết hạn, hết lượt hoặc không áp dụng (theo *quy tắc code thực*, không sáng tạo nghiệp vụ).
- Quote/submit lỗi hoặc có dữ liệu thay đổi khiến số tiền trước đó **không còn được xác nhận**.
- Kết quả POST thành công/redirect/flash theo contract hiện hữu.

Yêu cầu:
- UI phải nói rõ trạng thái **“Cần cập nhật/xác nhận lại báo giá”** hoặc nội dung tương đương khi quote không còn hợp lệ; không ngụy tạo một con số `0 ₫` như thể giảm giá đã được xác nhận trong khi voucher vẫn được giữ.
- Có thể giữ `voucher` để người dùng sửa, **nhưng input còn mã không đồng nghĩa đã áp dụng thành công**. Mọi nhãn “đã áp dụng”, giảm giá và tổng phải dựa vào **kết quả server có hiệu lực**. Tránh tự tính tiền hay lưu số tiền đáng tin trên client.
- Nếu state chưa đáng tin, thiết kế hành động “Cập nhật báo giá” rõ ràng và trạng thái đặt hàng có ý nghĩa; bảo toàn thao tác với JavaScript disabled. Không dùng disabled button như **biện pháp duy nhất** kiểm soát an toàn server.
- Server phải tiếp tục quyết định lại tính hợp lệ tại submit theo **logic hiện hành**; không làm yếu quy tắc validation hoặc CSRF. Nếu luồng hiện hành thiếu bảo vệ này, ghi blocker thay vì âm thầm tuyên bố bảo đảm.
- Lỗi từ server phải hiển thị rõ, có liên kết với trường liên quan khi có thể, không biến mất nếu quay lại từ POST; tránh trạng thái vừa báo thành công vừa báo lỗi.

## 3. B4.2 — Layout checkout và action hierarchy

- Nhóm rõ địa chỉ giao hàng, voucher/quote, phương thức thanh toán (chỉ phương thức được cấu hình), tóm tắt đơn và CTA.
- Dùng tokens U1 (nền ngà/burgundy/serif/spacing) sẵn có; không sửa toàn hệ thống, header/footer hoặc GSAP.
- Phân biệt trực quan và ngữ nghĩa giữa **“Cập nhật báo giá”** và **“Đặt hàng”**, không làm người dùng tưởng rằng báo giá đã đặt đơn.
- Giữ tiền VND và phép tính từ server `BigDecimal`; không sửa format và giá trị vì mục đích thẩm mỹ.
- Bảo đảm label, error summary, focus, Tab/Shift+Tab, Enter/Space, trạng thái disabled nếu có, thứ tự đọc, `aria-describedby`/`aria-invalid` khi cần, kích thước vùng bấm và không tràn ngang ở zoom 200%.
- Giữ khả năng checkout/no-JS phù hợp luồng đang có; các đường dẫn và phương thức POST không thay đổi.

## 4. B4.3 — Verification matrix (bắt buộc)

### MVC / contract / nghiệp vụ không đổi

Viết ca regression rõ ràng cho:
1. GET checkout Guest → login/redirect theo Security hiện hành; Customer vào checkout; Admin không bị lộ đường giao dịch Customer.
2. Giỏ trống, thiếu địa chỉ, địa chỉ không thuộc Customer, dữ liệu submit sai: phản hồi đúng; **không thể báo thành công giả**.
3. Không voucher; voucher hợp lệ; voucher không hợp lệ/không áp dụng; lỗi báo giá; lỗi sau khi báo giá có voucher; voucher còn trong input nhưng quote không hợp lệ; xác nhận lại quote sau sửa.
4. Trước/sau POST quote và POST checkout: giữ CSRF, tên tham số thực, không gửi POST trùng do handler JS; **không tuyên bố idempotency giao dịch nếu backend chưa hỗ trợ**.
5. COD ở mức fixture/mock/service và thứ tự redirect/trạng thái đơn hợp lệ **nếu fixture hỗ trợ**; nếu không, ghi NOT VERIFIED thay vì mock thành công không tương ứng source.
6. MoMo OFF: option ẩn/không thể chọn; MoMo ON: option/contract/redirect hoặc **mock được dán nhãn rõ**, KHÔNG gọi gateway thật, KHÔNG tự nhận đã xác nhận IPN hoặc thanh toán thành công.
7. Giá/tổng/giảm giá luôn đến từ server; không dùng tiền client làm source of truth.
8. Không đổi chức năng PDP, cart, voucher contract; đối chiếu diff và regression của C01–C26/C28 đã PASS.

### Chrome Headless

- 5 kích thước vật lý: **360×800, 390×844, 768×1024, 1366×768, 1920×1080**; **zoom thật 100%/200%**.
- Guest/Customer/Admin trên các trang liên quan; Checkout Customer ở đầy đủ các viewport. JavaScript **on/off**, keyboard, Enter/Space, Tab/Shift+Tab, mobile touch mô phỏng; focus/error state.
- Thử quote hợp lệ/sai, submit lỗi F07, giữ mã voucher nhưng trạng thái giảm giá chưa được xác nhận, báo giá lại, COD và MoMo theo cấu hình (chỉ fixture). Trạng thái phải khớp với response server, không chỉ thay DOM trong test.
- Không horizontal overflow; không `Runtime.exceptionThrown`; không gãy header/footer/navigation, giỏ/SKU hoặc GSAP C19 trên Home.
- Chụp before/after/invalid/valid/JS disabled ở desktop/mobile, lưu số đo, JSON/lệnh/log trong `docs/ui/screenshots/byredo-inspired/B4/`.
- Nếu không thể chạy Chrome, đánh dấu NOT RUN và dừng ở mức bằng chứng phù hợp, không tự nâng C27 thành PASS.

### Build và giới hạn môi trường

- Dùng **JDK 21** và Maven có sẵn; chạy `mvn test` và `mvn -DskipTests package`; `git diff --check`, JS syntax check nếu có sửa JS.
- **KHÔNG bật `P01_MARIADB_TEST=true`, KHÔNG seed, migration hoặc ghi vào `beautyshop`.** Nếu chưa có test DB tách biệt được owner cho phép, giữ 34 MariaDB test SKIPPED và ghi chính xác.
- Nếu DB test riêng đã được owner cho phép ngoài phạm vi B4, báo điều kiện cần và cách thực hiện để owner quyết định sau; không tự thực thi.
- Không sửa production CSP/headers hoặc cấu hình thanh toán chỉ để test PASS.

## 5. PASS gates và nguyên tắc báo cáo

- **C27 PASS trong phạm vi UI/MVC/fixture** chỉ khi F07 được kiểm bằng ca lỗi **thực sự qua controller/view**, báo giá hợp lệ/không hợp lệ phân biệt rõ, hai POST contract và các trạng thái COD/MoMo theo cấu hình được kiểm đầy đủ trong fixture hoặc bằng chứng tương đương đáng tin. Nếu thiếu nhánh bắt buộc, ghi **PARTIAL**, không tự làm tròn thành 24/30.
- F01/MariaDB, lưu đơn thật, MoMo IPN thực tế và tải production là **cổng riêng**. PASS fixture tuyệt đối không được diễn giải thành E2E MariaDB.
- Nếu tất cả gate UI PASS và các component cũ không regression: **24/30 =80%, core 12/12**. Nếu không, giữ 23/30 hoặc con số thực tế.
- Không đánh đồng số record Chrome với test Maven; phân biệt historical vs rerun, before vs after; không dùng screenshot/mock làm bằng chứng sản phẩm thật.

## 6. Bàn giao và DỪNG

1. Cập nhật đầy đủ:
   - `docs/reviews/byredo-inspired-ui-implementation.md` (mục B4 mới, đối chiếu trước/sau, F07, hạn chế)
   - `docs/reviews/byredo-component-gap-analysis.md` (C27 và ma trận 30 thành phần, chỉ nâng trạng thái khi đủ chứng cứ)
2. Lưu `docs/ui/screenshots/byredo-inspired/B4/`: ảnh trước/sau, checks, measurements, mô tả fixture/role, kết quả Maven JDK21, `git diff --check`, inventory/preservation và phân biệt trường hợp NOT VERIFIED.
3. Báo chính xác danh sách file sửa, route/contract giữ nguyên, số PASS/FAIL/ERROR/SKIPPED, kết quả QA và blocker MariaDB/CSP/MoMo/khác.
4. **Không commit/push, không reset/clean, không làm B3, không cào/seed dữ liệu sản phẩm, không mở rộng sang C13/C17/C20/C21.**
5. **DỪNG SAU B4**, gửi hai báo cáo mới nhất để owner nghiệm thu. Tuyệt đối không tự tuyên bố P15 production-ready.
