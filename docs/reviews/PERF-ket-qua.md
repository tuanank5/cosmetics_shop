# PERF · Baseline và chi phí đọc giỏ Customer

**IN_PROGRESS.** Owner giao phần độc lập sau BASE, AUTH và QUALITY. HEAD và ref local origin/main khi bắt đầu cùng ở `e33e4e3e72e23852b8dcc60623da822acd32cae7`. Workspace đã có tài liệu nghiệm thu chưa commit, backup và QA của các WP trước. Baseline được build trong clone mới. Ứng viên là cùng parent cộng bốn file trong manifest. Tổng tiến độ vẫn 3/19 DONE. AUTH-LIVE tiếp tục BLOCKED_EXTERNAL. [Giao thức chốt trước khi đo](../qa/PERF/protocol.md) · [Manifest ứng viên](../qa/PERF/candidate/source.json)

## Phép đo

Mỗi bản có 12 kịch bản, 100 request đã warm mỗi kịch bản, hai client riêng. Sáu tài khoản Customer chia theo cặp cho giỏ 1, 10 hoặc 100 dòng. Catalog/PDP dùng Guest, Admin dùng hai session Admin. Search kết hợp từ khóa, danh mục, sắp giá và phân trang. POST quote đi qua login, session, CSRF và tính tiền server-side. Con số HTTP gồm nhận toàn bộ response body trên loopback. Upload SKU đo riêng qua trình duyệt. IPN có chữ ký và thao tác tạo đơn chạy trong integration regression với gateway fixture. AUTH-LIVE, merchant thật, tải production và lịch sử chat Mongo thuộc gate khác.

MariaDB có datadir mới cho từng lượt; seed xác định và mật khẩu fixture sinh ngẫu nhiên. MongoDB 6.0.8 chạy riêng với collection review trống. Số dòng và cờ active trên cả hai bản đã đối chiếu. Lần đọc đầu từng route nằm trong summary.json; JVM/DB đã khởi động và đọc dữ liệu khi login. Cold browser là context mới. Độ lớn ảnh và nội dung giỏ là dữ liệu tổng hợp. Ở 100 mẫu, p99 nhạy với một vài request chậm. [Baseline](../qa/PERF/baseline/summary.json) · [Ứng viên](../qa/PERF/candidate/summary.json)

## Finding và phạm vi thay đổi

Trace GET giỏ, GET checkout và POST quote có hai SELECT chi tiết giỏ cùng tham số người mua. GiaoDien_Advice đọc danh sách để cộng số lượng cho huy hiệu. Controller đọc lại cùng danh sách để dựng trang. Với giỏ 100 dòng, mỗi truy vấn trả 100 dòng, khoảng 10.657 byte khi xuất TSV. Đây là hai lần join/map cùng dữ liệu trong một request. Số SELECT giữ cố định giữa giỏ 1, 10 và 100 dòng; dấu hiệu là đọc trùng, không phải N+1 theo dòng. [SQL và EXPLAIN trước](../qa/PERF/baseline/plans.json) · [Chi phí từng SELECT](../qa/PERF/baseline/diagnostics.json)

Ứng viên dùng danh sách đang render để cộng `soLuong` cho Customer. Huy hiệu Guest vẫn đọc giỏ session. Ba controller/advice được sửa là `GiaoDien_Advice`, `GioHang_Controller` và `Checkout_Controller`. Không cache giữa request. Chỉ xử lý dữ liệu hiển thị; giá, kiểm chủ sở hữu, khóa SKU, đặt COD/MoMo và voucher tiếp tục do các service hiện hành quyết định. CheckoutPagesTest thêm một ca kiểm huy hiệu bằng 2 cho một SKU số lượng 2 và đúng một lần gọi service. WebKit đối chiếu badge và số dòng trên cả hai bản. [Kiểm WebKit](../qa/PERF/candidate/cart-webkit.json)

## Baseline → ứng viên

Mỗi ô ghi kết quả trước → sau. Cả 2.400 request đo đều đạt kiểm HTTP và marker nội dung, error rate 0% tại từng route. SQL là số SELECT MariaDB từ trace riêng một request, HTML là kích thước body trung bình trước nén.

| Route | p50 ms | p95 ms | p99 ms | RPS | SELECT | HTML byte |
|---|---:|---:|---:|---:|---:|---:|
| catalog | 56.2 → 49.8 | 88.7 → 68.8 | 98.7 → 72.0 | 34.3 → 38.4 | 4 → 4 | 46806 → 46806 |
| catalog-page | 38.2 → 43.5 | 55.1 → 60.4 | 63.5 → 61.1 | 50.2 → 45.3 | 4 → 4 | 47627 → 47627 |
| search-filter | 19.2 → 23.0 | 47.2 → 39.3 | 130.3 → 44.0 | 80.3 → 80.4 | 4 → 4 | 49095 → 49095 |
| pdp | 11.4 → 13.2 | 31.9 → 35.1 | 33.7 → 36.9 | 112.8 → 112.3 | 2 → 2 | 15217 → 15217 |
| admin | 40.3 → 38.3 | 65.8 → 58.2 | 69.8 → 61.1 | 46.8 → 47.6 | 6 → 6 | 43571 → 43571 |
| cart-1 | 6.6 → 4.6 | 27.2 → 26.2 | 31.0 → 29.1 | 204.5 → 199.4 | 3 → 2 | 15440 → 15440 |
| checkout-1 | 7.7 → 6.9 | 29.6 → 30.3 | 32.9 → 31.7 | 158.9 → 153.5 | 4 → 3 | 24976 → 24976 |
| cart-10 | 7.2 → 8.2 | 29.8 → 29.7 | 31.9 → 31.8 | 156.7 → 140.4 | 3 → 2 | 51018 → 51018 |
| checkout-10 | 7.1 → 7.1 | 30.6 → 27.0 | 31.3 → 28.8 | 174.7 → 216.3 | 4 → 3 | 34310 → 34310 |
| cart-100 | 27.6 → 23.0 | 47.1 → 44.5 | 56.3 → 49.1 | 63.4 → 70.7 | 3 → 2 | 409494 → 409494 |
| checkout-100 | 15.4 → 10.6 | 35.8 → 33.0 | 37.4 → 34.4 | 92.7 → 109.1 | 4 → 3 | 127886 → 127886 |
| quote-100 | 16.2 → 12.0 | 35.2 → 33.3 | 37.8 → 35.0 | 105.5 → 101.6 | 4 → 3 | 127886 → 127886 |

Checkout 100 dòng giảm p50 khoảng 31%, p95 khoảng 8%, RPS tăng khoảng 18%. Giỏ 100 dòng giảm p95 khoảng 5%, RPS tăng khoảng 12%. Giỏ 10 dòng giảm throughput khoảng 10%, checkout 1 dòng tăng p95 0,7 ms và quote 100 dòng giảm throughput khoảng 4%. Cả những route đối chứng cũng dao động giữa hai lượt. Quyết định giữ thay đổi dựa trên một SELECT và một lần truyền/map danh sách được loại ở mọi kích thước, WebKit hiển thị đúng huy hiệu và số liệu giỏ lớn. Hai lượt đo chưa xác lập mức cải thiện latency ổn định cho mọi tải. [Từng mẫu trước](../qa/PERF/baseline/latency.json) · [Từng mẫu sau](../qa/PERF/candidate/latency.json) · [Script so sánh](../qa/PERF/compare.py)

## Các điểm còn lại

- **Catalog.** 12 thẻ cần bốn SELECT MariaDB, gồm query dữ liệu, count phân trang và hai bộ lọc. Plan không lọc duyệt khoảng 3.600 SKU và dùng temporary/filesort cho phép nhóm/sắp xếp. Plan lọc dùng unique index trên slug danh mục. Cờ active có độ chọn lọc thấp. Chưa có thử nghiệm index cho thấy lợi ích ở cả đường đọc và ghi.
- **Admin.** Một trang 15 sản phẩm cần sáu SELECT, gồm kiểm phiên, data/count, danh mục, hãng và đếm đơn đang xử lý. Options trả 24 và 40 dòng. Phép đếm nghĩa vụ trả 120 nhóm trên toàn catalog. Đây là ứng viên để thử giới hạn theo ID của trang ở đường đọc. Kiểm BR21 trên đường ghi cần tiếp tục xét đủ nghĩa vụ. Các file diagnostics ghi số dòng, byte TSV và bảy thời gian ANALYZE của từng SELECT theo thứ tự trong plans.json.
- **PDP và giỏ lớn.** PDP thực hiện hai SELECT MariaDB, còn truy xuất review thuộc Mongo. Giỏ 100 dòng vẫn trả khoảng 409 KB HTML. Việc loại SELECT trùng không giảm payload HTML. Chưa thay pagination hay UX giỏ.
- **Cache và module mới.** Không thêm Redis cache, index, hoặc cache công khai giá/tồn/quyền. Fixture chưa đại diện cho lịch sử attempt, audit hay AI-HISTORY của các WP tiếp theo. Chưa có số đo làm cơ sở cho các thay đổi ở đó.

## Trình duyệt, tài nguyên và cache ảnh

Edge cold mobile 390 px ghi LCP Home/catalog/PDP trước là 552/180/160 ms, sau là 536/180/172 ms. Cold desktop 1440 px ghi 316/172/120 ms trước và 304/192/120 ms sau. CLS Home desktop là 0,094 ở hai bản; Home mobile từ 0 lên 0,101 trong lần đo đơn. Hai màn catalog/PDP đều ghi CLS 0. WebKit ghi LCP, không hỗ trợ entry layout-shift trong môi trường này. Dữ liệu lưu riêng theo viewport, route, lượt cold/warm với transfer/body bytes, thời điểm, kích thước, eager/lazy và ưu tiên ảnh. Byte transfer của tài nguyên cùng origin ở mobile cold trên Edge là 448.435 cho Home, 200.168 cho catalog và 181.220 cho PDP ở cả hai bản, chưa tính document HTML. [Waterfall trước](../qa/PERF/baseline/edge.json) · [Waterfall sau](../qa/PERF/candidate/edge.json) · [WebKit](../qa/PERF/candidate/webkit.json)

Tại viewport 390 px, thẻ đầu catalog tải eager, các thẻ ngoài màn hình tải lazy; ảnh PDP nằm trong viewport, 358×358 px, loading/priority auto. Ảnh SKU trả 200 cả lần đầu và lần gửi điều kiện, không có ETag. Đổi UUID trên fixture sau tải cho PDP dùng URL mới trả 200; URL cũ hết tham chiếu trả 404. Chưa có bằng chứng đủ để thay header ảnh hoặc cache một tài nguyên đã thu hồi. Nên kiểm lại CLS Home bằng nhiều navigation trên thiết bị đại diện trước khi mở thay đổi giao diện.

JVM dùng 56,31 → 49,95 CPU-giây trong cửa sổ HTTP/trace; peak RSS JVM 546 → 555 MiB, MariaDB xấp xỉ 134 MiB ở hai lượt. G1 từ startup đến cuối tải ghi 70 → 68 sự kiện pause, tổng 315 → 287 ms, pause lớn nhất 13,4 → 11,0 ms. Đây là chỉ số process trên máy dùng chung; peak RSS và GC bao gồm startup. Lượt chẩn đoán SQL baseline được chạy lại trên datadir riêng, thời gian ANALYZE chỉ dùng để định vị chi phí. Riêng Admin, đọc danh mục/hãng trả 548/892 byte TSV với median 0,116/0,138 ms, trong khi đếm nghĩa vụ toàn catalog trả 120 dòng và median 86,1 ms trong lượt chẩn đoán đó. [Tài nguyên trước](../qa/PERF/baseline/resources.json) · [Tài nguyên sau](../qa/PERF/candidate/resources.json) · [GC](../qa/PERF/candidate/gc.txt)

## Regression và điều kiện nghiệm thu

| Gate local | Baseline | Ứng viên |
|---|---:|---:|
| JDK 21 Maven test | 236 PASS, 88 SKIP | 237 PASS, 88 SKIP |
| Regression fixture MariaDB, gồm 12 ca unit chạy kèm | 94/94 | 94/94 |
| Maven package | PASS | PASS |
| WebKit giỏ/checkout, đối chiếu dòng và badge | 6/6 | 6/6 |
| WebKit và Edge đọc Home/catalog/PDP | 12 navigation/engine | 12 navigation/engine |

Hai suite integration dùng fixture riêng, gồm quyền sở hữu, COD, tồn/voucher, rollback, mock MoMo và OAuth. Không gộp các lượt này vào sample latency. Regression login/provider dùng fixture; không dùng tài khoản sản xuất. Không thay migration, transaction thanh toán, đường ghi kho, Redis hay cấu hình production. Chưa có SHA ứng viên đã commit, PR, CI hoặc nghiệm thu. [Maven](../qa/PERF/candidate/test.txt) · [Integration](../qa/PERF/candidate/integration.txt) · [Package](../qa/PERF/candidate/package.txt) · [Giao thức tái lập](../qa/PERF/protocol.md)

## PERF-FINAL · 28/11/2026

Bốn file nguồn khớp manifest đã đo. Đã kiểm lại số liệu thô và chạy thêm WebKit với giỏ thay đổi qua hai tab trên MariaDB riêng. PERF vẫn IN_PROGRESS. [Kết quả, hồi quy RPS và giới hạn kiểm chứng](../qa/PERF/final-review/review.md)
