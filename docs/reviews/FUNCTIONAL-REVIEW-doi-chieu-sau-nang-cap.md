# BeautyShop · Đối chiếu phiếu rà soát chức năng sau nâng cấp

Đối chiếu [phiếu chức năng ban đầu](BEAUTYSHOP_FUNCTIONAL_REVIEW.md) với `main` tại `c79983a`. Phiếu gốc có 73 ca, gồm **44 Đạt, 2 Đạt kèm nhận xét, 14 Chưa chạy, 1 ô trống và 12 ghi nhận khác**. Giữ nguyên các đánh giá do owner ghi. Lượt đối chiếu này đọc báo cáo, log, mã nguồn liên quan và truy vấn kết quả GitHub Actions. [Ma trận đầy đủ 73 mã](../qa/QA-01/legacy-matrix.md)

## Phần cải thiện từ phản hồi cũ

| Mã ca | Cải thiện đã triển khai | Bằng chứng |
|---|---|---|
| SHOP-01, SHOP-07 đến SHOP-11 | Bấm logo đưa về chương đầu. Back giữ ngữ cảnh catalog. Bộ lọc giữ trạng thái qua phân trang. Review trên chi tiết sản phẩm giữ biến thể đang chọn và neo đánh giá khi đổi trang. | [NAV-01](NAV-01-ket-qua.md) · [UI-01C](UI-01C-ket-qua.md) · [UI-01P](UI-01P-ket-qua.md) |
| SHOP-05, SHOP-10, TK-01/02, DH-02/03, AD-08/12/13 | Giải thích thuật ngữ ngay trên giao diện. Dùng “Mã biến thể sản phẩm”, “Mã giảm giá”, “Tính lại tổng tiền” và các nhãn tồn kho rõ hơn. FAQ mẫu dùng cùng thuật ngữ. | [UI-COPY-01](UI-COPY-01-ket-qua.md) |
| TK-04, TK-06, TK-09 | Gộp giỏ báo rõ số lượng được điều chỉnh, chỉ hiện một lần đúng ngữ cảnh. Nút hiện mật khẩu ổn định khi thao tác. Tim đồng bộ giữa trang và tab, có luồng HTML khi tắt JavaScript. | [CART-01](CART-01-ket-qua.md) · [UI-01C](UI-01C-ket-qua.md) · [WISH-01](WISH-01-ket-qua.md) |
| AD-05, AD-06, AD-07, AD-10, AD-15 | Danh sách đơn Admin lọc, tìm và giữ trang khi quay về. Danh mục/thương hiệu chuyển sang ngừng hoạt động và kích hoạt lại. Duyệt/ẩn đánh giá giữ bộ lọc, cập nhật số đếm và đưa focus về đúng chỗ. | [ORDER-01](ORDER-01-ket-qua.md) · [CAT-01](CAT-01-ket-qua.md) · [REVIEW-01](REVIEW-01-ket-qua.md) |
| DH-05, QA-01/02/07 | Chuẩn hóa giao diện lỗi. Fixture xác nhận Customer xem đơn sai chủ hoặc đơn không tồn tại đều nhận 404. Phiên và yêu cầu gốc từng trả 403 của owner chưa tái hiện. | [SEC-01](SEC-01-ket-qua.md) |
| TV-01/02/03, TV-04/05/06/07 | `/tu-van-da` dùng form, lỗi và loading chung. `/chat` chỉ điền lại câu hỏi hợp lệ. Email/số điện thoại bị từ chối không xuất hiện lại trong HTML và lượt mock ghi nhận 0 request embedding, 0 request chat. FAQ mới đã kiểm trên MariaDB trống. | [Tư vấn da](SKIN-UI-01-chuan-hoa-tu-van-da.md) · [QA-01](QA-01-ket-qua.md) · [FAQ](UI-COPY-01-ket-qua.md) |
| QA-06, vận hành | MongoDB là kho đánh giá duy nhất. Khi mất kết nối, giao diện báo tạm thời không tải được. Khi phục hồi, ứng dụng kiểm chỉ mục duy nhất. Đã thử khôi phục SQL, BSON, index và ảnh, cùng lệnh chạy demo trên fixture. | [DATA-01](DATA-01-ket-qua.md) · [Khôi phục](../qa/UI-COPY-01/data/restore.txt) · [Thiết lập local](LOCAL-SETUP-01-khoi-tao-va-nang-cap.md) |

**SHOP-04** giữ quy tắc sắp xếp cả tập đủ điều kiện rồi lấy tối đa 12 sản phẩm. Phân trang và giữ lựa chọn sắp xếp có kiểm thử. Câu hỏi về tiêu chí lấy 12 sản phẩm được giải thích trong phiếu kỹ thuật. **SHOP-12** mới dọn comment và khoảng trắng. Các link chính sách còn trỏ sang catalog/trang chủ, hai nhãn tư vấn vẫn trỏ `/chat`, link mạng xã hội vẫn là link chung. Phần nội dung và đích Footer còn ở backlog. [Phiếu SHOP](BEAUTYSHOP_FUNCTIONAL_REVIEW.md#41-trang-chủ-và-catalog--shop-01-đến-shop-13) · [Footer hiện tại](../../src/main/resources/templates/fragments/footer.html)

## 14 ca owner từng ghi Chưa chạy và 1 ô trống

Chưa có lần chạy tay mới của owner được ghi. Cột “Đã kiểm” ghi đúng phạm vi của các lần chạy bổ sung.

| Mã | Đã kiểm sau nâng cấp | Còn thiếu |
|---|---|---|
| SHOP-11 | Chrome có 43 review mẫu. Đổi trang giữ biến thể, neo đánh giá và kiểm trang vượt phạm vi. | Owner kiểm trên sản phẩm/dữ liệu của nhóm nếu dùng phiếu nghiệm thu tay. [UI-01P](UI-01P-ket-qua.md) |
| SHOP-13 | Chrome có bằng chứng giảm chuyển động và hiển thị dự phòng. | Chưa chạy video thương phẩm với nguồn hợp lệ. [NAV-01](NAV-01-ket-qua.md) |
| DH-03 | Test checkout và MariaDB kiểm xử lý mã giảm giá không hợp lệ. | Chạy tay từng tình huống chưa đến giờ, hết hạn, hết lượt và dưới ngưỡng trên checkout. [Log tích hợp](../qa/UI-COPY-01/data/integration.txt) |
| DH-07 | Tích hợp COD kiểm chuyển trạng thái và thu tiền. | Owner xem lại từng mốc bằng tài khoản Customer. [Log COD](../qa/UI-COPY-01/data/integration.txt) |
| DH-08 | MariaDB kiểm hai yêu cầu cạnh tranh SKU/lượt cuối và rollback. | Chưa có lượt chạy tay bằng hai phiên của nhóm. [Log COD và mã giảm giá](../qa/UI-COPY-01/data/integration.txt) |
| AD-14 | Trang nguồn tư vấn và thao tác với provider đã được kiểm trong fixture mô phỏng. | Chưa kiểm nạp nguồn và hỏi trên Ollama thật. [QA-01](QA-01-ket-qua.md) |
| MO-02 | Tích hợp mock kiểm đơn chờ và giữ kho/lượt. | Chưa có kết quả owner trên trình duyệt. [QA-01](QA-01-ket-qua.md) |
| MO-03 | Tích hợp mock kiểm thanh toán thành công, chốt và IPN lặp. | Chưa có kết quả owner đối chiếu đơn và dashboard. [QA-01](QA-01-ket-qua.md) |
| MO-04 | Tích hợp mock kiểm từ chối, thử lại và mã lần thanh toán. | Chưa có kết quả owner của luồng thử lại. [QA-01](QA-01-ket-qua.md) |
| MO-05 | Tích hợp mock kiểm hết hạn, tiếp tục giữ khi chưa xác minh và giải phóng khi có kết quả. | Chưa có lượt kiểm tay với đơn mock riêng. [QA-01](QA-01-ket-qua.md) |
| MO-06 | Có test đối soát mock và test gọi trực tiếp job để kiểm con trỏ theo lô. | Chưa đo job tự chạy theo lịch 60 giây, chưa có kết quả owner cho nút đối soát. [QA-01](QA-01-ket-qua.md) |
| MO-07 | Có test mock và chữ ký riêng trong vùng chức năng MoMo. | **Chưa có giao dịch Sandbox thật.** Cần merchant Sandbox và HTTPS nhận IPN. [Runbook](../deployment/04-momo-sandbox.md) |
| TV-07 | Tích hợp HTTP mock kiểm nạp nguồn và trả lời. Bộ RAG đạt 3/3. | **Chưa chạy chat và embedding trên Ollama thật.** [Log RAG](../qa/UI-COPY-01/data/integration.txt) |
| QA-06 | Đã tắt MongoDB trên fixture, kiểm giao diện lỗi rồi phục hồi và kiểm index. | Phần “đổi kho sang MariaDB” trong ca cũ đã bị loại khỏi kiến trúc theo DATA-01. Nếu chạy lại theo phiếu, dùng kịch bản mất và phục hồi MongoDB. [DATA-01](DATA-01-ket-qua.md) |
| QA-03 · Ô trống | Dashboard đạt 24 tổ hợp viewport, kỳ dữ liệu và JS. Có zoom 200% mô phỏng và các kiểm responsive riêng theo màn. | Chưa có lần kiểm tay của owner trên thiết bị vật lý hay toàn bộ màn hình trong checklist. [Ma trận dashboard](../qa/QA-01/dashboard-matrix.json) · [QA-01](QA-01-ket-qua.md) |

## Trạng thái bản tích hợp

- Hồi quy local sau chuẩn hóa giao diện có **313 đạt, 5 bỏ qua, 0 lỗi** trong 318 ca cấu hình MariaDB/MongoDB. Các ca bỏ qua gồm ba ca fixture nâng cấp local, một ca báo cáo demo và một ca BSON chạy riêng. Lượt BSON riêng và kiểm khôi phục đều đạt. [Kết quả](UI-COPY-01-ket-qua.md)
- GitHub Actions **Maven CI đã thành công cho `c79983a`**. Workflow chạy Maven compile, test và package. Kết quả này giải quyết mốc xác minh CI của commit tích hợp. Trạng thái QA-01 trong sổ và quyết định phát hành vẫn theo owner. [CI](https://github.com/tuanank5/cosmetics_shop/actions/runs/45818577904) · [Workflow](../../.github/workflows/ci.yml)
- Google/GitHub OAuth thật, OTP qua Redis/SMTP thật, MoMo Sandbox, Ollama thật và môi trường phát hành công khai còn thiếu bằng chứng trong hồ sơ. [AUTH-02](AUTH-02-nghiem-thu-xac-thuc.md) · [QA-01](QA-01-ket-qua.md)
